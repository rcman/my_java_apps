import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// Enums
enum ExportFormat {
    THREEJS("threejs"),
    BABYLONJS("babylonjs");
    
    private final String value;
    ExportFormat(String value) { this.value = value; }
    public String getValue() { return value; }
}

enum EditMode {
    OBJECT("object"),
    VERTEX("vertex"),
    EDGE("edge"),
    FACE("face");
    
    private final String value;
    EditMode(String value) { this.value = value; }
    public String getValue() { return value; }
}

enum Tool {
    SELECT("select"),
    MOVE("move"),
    ROTATE("rotate"),
    SCALE("scale"),
    EXTRUDE("extrude"),
    DELETE("delete"),
    ADD_VERTEX("add_vertex"),
    ADD_FACE("add_face");
    
    private final String value;
    Tool(String value) { this.value = value; }
    public String getValue() { return value; }
}

// Data classes
class Vector3 {
    public double x, y, z;
    
    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Vector3() {
        this(0, 0, 0);
    }
    
    public double[] toArray() {
        return new double[]{x, y, z};
    }
    
    public Map<String, Double> toMap() {
        Map<String, Double> map = new HashMap<>();
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        return map;
    }
    
    public double distanceTo(Vector3 other) {
        return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2) + Math.pow(z - other.z, 2));
    }
    
    public Vector3 add(Vector3 other) {
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }
    
    public Vector3 subtract(Vector3 other) {
        return new Vector3(x - other.x, y - other.y, z - other.z);
    }
    
    public Vector3 multiply(double scalar) {
        return new Vector3(x * scalar, y * scalar, z * scalar);
    }
}

class Vector2 {
    public double x, y;
    
    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public Vector2() {
        this(0, 0);
    }
    
    public double[] toArray() {
        return new double[]{x, y};
    }
    
    public Map<String, Double> toMap() {
        Map<String, Double> map = new HashMap<>();
        map.put("x", x);
        map.put("y", y);
        return map;
    }
}

class Color3D {
    public double r, g, b, a;
    
    public Color3D(double r, double g, double b, double a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }
    
    public Color3D(double r, double g, double b) {
        this(r, g, b, 1.0);
    }
    
    public Color3D() {
        this(1.0, 1.0, 1.0, 1.0);
    }
    
    public int toHex() {
        return ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
    }
    
    public double[] toArray() {
        return new double[]{r, g, b};
    }
    
    public Color toAWTColor() {
        return new Color((float)r, (float)g, (float)b, (float)a);
    }
    
    public String toHexString() {
        return String.format("#%02x%02x%02x", 
            (int)(r * 255), (int)(g * 255), (int)(b * 255));
    }
}

class Material {
    public String name;
    public Color3D diffuseColor;
    public Color3D specularColor;
    public Color3D emissiveColor;
    public Color3D ambientColor;
    public double shininess;
    public double opacity;
    public boolean transparent;
    public String diffuseTexture;
    public String normalTexture;
    public String specularTexture;
    
    public Material(String name) {
        this.name = name;
        this.diffuseColor = new Color3D(1.0, 1.0, 1.0);
        this.specularColor = new Color3D(1.0, 1.0, 1.0);
        this.emissiveColor = new Color3D(0.0, 0.0, 0.0);
        this.ambientColor = new Color3D(1.0, 1.0, 1.0);
        this.shininess = 30.0;
        this.opacity = 1.0;
        this.transparent = false;
    }
    
    public Material(String name, Color3D diffuseColor) {
        this(name);
        this.diffuseColor = diffuseColor;
    }
}

class Face {
    public List<Integer> vertices;
    public int materialIndex;
    public List<Integer> normals;
    public List<Integer> uvs;
    
    public Face(List<Integer> vertices, int materialIndex) {
        this.vertices = new ArrayList<>(vertices);
        this.materialIndex = materialIndex;
        this.normals = new ArrayList<>();
        this.uvs = new ArrayList<>();
    }
    
    public Face(List<Integer> vertices) {
        this(vertices, 0);
    }
}

class Animation {
    public String name;
    public double duration;
    public Map<String, Object> keyframes;
    
    public Animation(String name, double duration, Map<String, Object> keyframes) {
        this.name = name;
        this.duration = duration;
        this.keyframes = new HashMap<>(keyframes);
    }
}

class Model3D {
    public String name;
    public List<Vector3> vertices;
    public List<Vector3> normals;
    public List<Vector2> uvs;
    public List<Face> faces;
    public List<Material> materials;
    public List<Animation> animations;
    public Map<String, Object> metadata;
    
    public Model3D(String name) {
        this.name = name;
        this.vertices = new ArrayList<>();
        this.normals = new ArrayList<>();
        this.uvs = new ArrayList<>();
        this.faces = new ArrayList<>();
        this.materials = new ArrayList<>();
        this.animations = new ArrayList<>();
        this.metadata = new HashMap<>();
    }
    
    public Model3D() {
        this("Untitled");
    }
    
    public int addVertex(double x, double y, double z) {
        vertices.add(new Vector3(x, y, z));
        return vertices.size() - 1;
    }
    
    public int addNormal(double x, double y, double z) {
        normals.add(new Vector3(x, y, z));
        return normals.size() - 1;
    }
    
    public int addUV(double u, double v) {
        uvs.add(new Vector2(u, v));
        return uvs.size() - 1;
    }
    
    public int addMaterial(Material material) {
        materials.add(material);
        return materials.size() - 1;
    }
    
    public int addFace(List<Integer> vertexIndices, int materialIndex) {
        faces.add(new Face(vertexIndices, materialIndex));
        return faces.size() - 1;
    }
    
    public int addFace(List<Integer> vertexIndices) {
        return addFace(vertexIndices, 0);
    }
    
    public void removeVertex(int index) {
        if (index >= 0 && index < vertices.size()) {
            vertices.remove(index);
            
            // Update face indices and remove faces that reference this vertex
            Iterator<Face> faceIterator = faces.iterator();
            while (faceIterator.hasNext()) {
                Face face = faceIterator.next();
                if (face.vertices.contains(index)) {
                    faceIterator.remove();
                } else {
                    // Update indices higher than removed vertex
                    for (int i = 0; i < face.vertices.size(); i++) {
                        if (face.vertices.get(i) > index) {
                            face.vertices.set(i, face.vertices.get(i) - 1);
                        }
                    }
                }
            }
        }
    }
    
    public void removeFace(int index) {
        if (index >= 0 && index < faces.size()) {
            faces.remove(index);
        }
    }
    
    public Vector3 getFaceCenter(int faceIndex) {
        if (faceIndex >= 0 && faceIndex < faces.size()) {
            Face face = faces.get(faceIndex);
            Vector3 center = new Vector3();
            int count = 0;
            
            for (int vertexIdx : face.vertices) {
                if (vertexIdx < vertices.size()) {
                    Vector3 vertex = vertices.get(vertexIdx);
                    center.x += vertex.x;
                    center.y += vertex.y;
                    center.z += vertex.z;
                    count++;
                }
            }
            
            if (count > 0) {
                center.x /= count;
                center.y /= count;
                center.z /= count;
            }
            
            return center;
        }
        return new Vector3();
    }
    
    public void extrudeFace(int faceIndex, double distance) {
        if (faceIndex >= 0 && faceIndex < faces.size()) {
            Face face = faces.get(faceIndex);
            List<Integer> originalVertices = new ArrayList<>(face.vertices);
            
            // Calculate face normal
            if (originalVertices.size() >= 3) {
                Vector3 v1 = vertices.get(originalVertices.get(0));
                Vector3 v2 = vertices.get(originalVertices.get(1));
                Vector3 v3 = vertices.get(originalVertices.get(2));
                
                Vector3 edge1 = v2.subtract(v1);
                Vector3 edge2 = v3.subtract(v1);
                
                Vector3 normal = new Vector3(
                    edge1.y * edge2.z - edge1.z * edge2.y,
                    edge1.z * edge2.x - edge1.x * edge2.z,
                    edge1.x * edge2.y - edge1.y * edge2.x
                );
                
                double length = Math.sqrt(normal.x * normal.x + normal.y * normal.y + normal.z * normal.z);
                if (length > 0) {
                    normal = normal.multiply(distance / length);
                }
                
                // Create new vertices
                List<Integer> newVertices = new ArrayList<>();
                for (int vertexIdx : originalVertices) {
                    Vector3 oldVertex = vertices.get(vertexIdx);
                    Vector3 newVertex = oldVertex.add(normal);
                    newVertices.add(addVertex(newVertex.x, newVertex.y, newVertex.z));
                }
                
                // Update the original face to use new vertices
                face.vertices = newVertices;
                
                // Create side faces
                for (int i = 0; i < originalVertices.size(); i++) {
                    int nextI = (i + 1) % originalVertices.size();
                    
                    List<Integer> quadVertices = Arrays.asList(
                        originalVertices.get(i),
                        originalVertices.get(nextI),
                        newVertices.get(nextI),
                        newVertices.get(i)
                    );
                    addFace(quadVertices);
                }
            }
        }
    }
    
    public void addAnimation(Animation animation) {
        animations.add(animation);
    }
    
    public void calculateNormals() {
        normals.clear();
        
        for (Face face : faces) {
            if (face.vertices.size() >= 3) {
                Vector3 v1 = vertices.get(face.vertices.get(0));
                Vector3 v2 = vertices.get(face.vertices.get(1));
                Vector3 v3 = vertices.get(face.vertices.get(2));
                
                Vector3 edge1 = v2.subtract(v1);
                Vector3 edge2 = v3.subtract(v1);
                
                Vector3 normal = new Vector3(
                    edge1.y * edge2.z - edge1.z * edge2.y,
                    edge1.z * edge2.x - edge1.x * edge2.z,
                    edge1.x * edge2.y - edge1.y * edge2.x
                );
                
                double length = Math.sqrt(normal.x * normal.x + normal.y * normal.y + normal.z * normal.z);
                if (length > 0) {
                    normal.x /= length;
                    normal.y /= length;
                    normal.z /= length;
                }
                
                int normalIndex = addNormal(normal.x, normal.y, normal.z);
                face.normals.clear();
                for (int i = 0; i < face.vertices.size(); i++) {
                    face.normals.add(normalIndex);
                }
            }
        }
    }
}

// Simple JSON parsing classes
class SimpleJsonParser {
    private String json;
    private int index;
    
    public SimpleJsonParser(String json) {
        this.json = json.trim();
        this.index = 0;
    }
    
    public JsonValue parse() {
        skipWhitespace();
        return parseValue();
    }
    
    private JsonValue parseValue() {
        skipWhitespace();
        char c = peek();
        
        if (c == '{') {
            return new JsonValue(parseObject());
        } else if (c == '[') {
            return new JsonValue(parseArray());
        } else if (c == '"') {
            return new JsonValue(parseString());
        } else if (c == 't' || c == 'f') {
            return new JsonValue(parseBoolean());
        } else if (c == 'n') {
            parseNull();
            return new JsonValue(null);
        } else if (Character.isDigit(c) || c == '-') {
            return new JsonValue(parseNumber());
        }
        
        throw new RuntimeException("Unexpected character: " + c);
    }
    
    private JsonObject parseObject() {
        JsonObject obj = new JsonObject();
        consume('{');
        skipWhitespace();
        
        if (peek() == '}') {
            consume('}');
            return obj;
        }
        
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            consume(':');
            skipWhitespace();
            JsonValue value = parseValue();
            obj.put(key, value);
            
            skipWhitespace();
            if (peek() == '}') {
                consume('}');
                break;
            } else if (peek() == ',') {
                consume(',');
            } else {
                throw new RuntimeException("Expected ',' or '}'");
            }
        }
        
        return obj;
    }
    
    private JsonArray parseArray() {
        JsonArray arr = new JsonArray();
        consume('[');
        skipWhitespace();
        
        if (peek() == ']') {
            consume(']');
            return arr;
        }
        
        while (true) {
            skipWhitespace();
            arr.add(parseValue());
            skipWhitespace();
            
            if (peek() == ']') {
                consume(']');
                break;
            } else if (peek() == ',') {
                consume(',');
            } else {
                throw new RuntimeException("Expected ',' or ']'");
            }
        }
        
        return arr;
    }
    
    private String parseString() {
        consume('"');
        StringBuilder sb = new StringBuilder();
        
        while (peek() != '"') {
            char c = consume();
            if (c == '\\') {
                char escaped = consume();
                switch (escaped) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    default: sb.append(escaped); break;
                }
            } else {
                sb.append(c);
            }
        }
        
        consume('"');
        return sb.toString();
    }
    
    private boolean parseBoolean() {
        if (json.startsWith("true", index)) {
            index += 4;
            return true;
        } else if (json.startsWith("false", index)) {
            index += 5;
            return false;
        }
        throw new RuntimeException("Invalid boolean");
    }
    
    private void parseNull() {
        if (json.startsWith("null", index)) {
            index += 4;
        } else {
            throw new RuntimeException("Invalid null");
        }
    }
    
    private double parseNumber() {
        int start = index;
        
        if (peek() == '-') {
            consume();
        }
        
        if (peek() == '0') {
            consume();
        } else if (Character.isDigit(peek())) {
            while (Character.isDigit(peek())) {
                consume();
            }
        } else {
            throw new RuntimeException("Invalid number");
        }
        
        if (peek() == '.') {
            consume();
            while (Character.isDigit(peek())) {
                consume();
            }
        }
        
        if (peek() == 'e' || peek() == 'E') {
            consume();
            if (peek() == '+' || peek() == '-') {
                consume();
            }
            while (Character.isDigit(peek())) {
                consume();
            }
        }
        
        return Double.parseDouble(json.substring(start, index));
    }
    
    private char peek() {
        if (index >= json.length()) return '\0';
        return json.charAt(index);
    }
    
    private char consume() {
        if (index >= json.length()) throw new RuntimeException("Unexpected end of input");
        return json.charAt(index++);
    }
    
    private void consume(char expected) {
        char c = consume();
        if (c != expected) {
            throw new RuntimeException("Expected '" + expected + "' but got '" + c + "'");
        }
    }
    
    private void skipWhitespace() {
        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
            index++;
        }
    }
}

// Simple JSON data structures
class JsonValue {
    private Object value;
    
    public JsonValue(Object value) {
        this.value = value;
    }
    
    public String getAsString() {
        return value != null ? value.toString() : null;
    }
    
    public double getAsDouble() {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(value.toString());
    }
    
    public int getAsInt() {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }
    
    public boolean getAsBoolean() {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
    
    public JsonObject getAsJsonObject() {
        return (JsonObject) value;
    }
    
    public JsonArray getAsJsonArray() {
        return (JsonArray) value;
    }
    
    public boolean isJsonObject() {
        return value instanceof JsonObject;
    }
    
    public boolean isJsonArray() {
        return value instanceof JsonArray;
    }
    
    public boolean isNull() {
        return value == null;
    }
}

class JsonObject {
    private Map<String, JsonValue> map = new HashMap<>();
    
    public void put(String key, JsonValue value) {
        map.put(key, value);
    }
    
    public JsonValue get(String key) {
        return map.get(key);
    }
    
    public boolean has(String key) {
        return map.containsKey(key);
    }
    
    public Set<String> keySet() {
        return map.keySet();
    }
}

class JsonArray {
    private List<JsonValue> list = new ArrayList<>();
    
    public void add(JsonValue value) {
        list.add(value);
    }
    
    public JsonValue get(int index) {
        return list.get(index);
    }
    
    public int size() {
        return list.size();
    }
}

// Three.js JSON loader without Gson
class ThreeJSLoader {
    
    public Model3D loadFromFile(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        return loadFromJson(content);
    }
    
    public Model3D loadFromJson(String jsonContent) {
        try {
            SimpleJsonParser parser = new SimpleJsonParser(jsonContent);
            JsonValue root = parser.parse();
            return parseThreeJSModel(root.getAsJsonObject());
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Three.js JSON: " + e.getMessage(), e);
        }
    }
    
    private Model3D parseThreeJSModel(JsonObject root) {
        Model3D model = new Model3D();
        
        // Parse metadata
        if (root.has("metadata")) {
            JsonObject metadata = root.get("metadata").getAsJsonObject();
            if (metadata.has("generator")) {
                model.name = metadata.get("generator").getAsString();
            }
        }
        
        // Parse name
        if (root.has("name")) {
            model.name = root.get("name").getAsString();
        }
        
        // Parse data
        if (root.has("data")) {
            JsonObject data = root.get("data").getAsJsonObject();
            
            // Parse vertices
            if (data.has("vertices")) {
                JsonArray vertices = data.get("vertices").getAsJsonArray();
                for (int i = 0; i < vertices.size(); i += 3) {
                    double x = vertices.get(i).getAsDouble();
                    double y = vertices.get(i + 1).getAsDouble();
                    double z = vertices.get(i + 2).getAsDouble();
                    model.addVertex(x, y, z);
                }
            }
            
            // Parse normals
            if (data.has("normals")) {
                JsonArray normals = data.get("normals").getAsJsonArray();
                for (int i = 0; i < normals.size(); i += 3) {
                    double x = normals.get(i).getAsDouble();
                    double y = normals.get(i + 1).getAsDouble();
                    double z = normals.get(i + 2).getAsDouble();
                    model.addNormal(x, y, z);
                }
            }
            
            // Parse UVs
            if (data.has("uvs") && data.get("uvs").isJsonArray()) {
                JsonArray uvArray = data.get("uvs").getAsJsonArray();
                if (uvArray.size() > 0 && uvArray.get(0).isJsonArray()) {
                    JsonArray uvs = uvArray.get(0).getAsJsonArray();
                    for (int i = 0; i < uvs.size(); i += 2) {
                        double u = uvs.get(i).getAsDouble();
                        double v = uvs.get(i + 1).getAsDouble();
                        model.addUV(u, v);
                    }
                }
            }
            
            // Parse materials
            if (data.has("materials")) {
                JsonArray materials = data.get("materials").getAsJsonArray();
                for (int i = 0; i < materials.size(); i++) {
                    JsonObject matObj = materials.get(i).getAsJsonObject();
                    String name = matObj.has("name") ? matObj.get("name").getAsString() : "Material";
                    Material material = new Material(name);
                    
                    if (matObj.has("color")) {
                        int color = matObj.get("color").getAsInt();
                        material.diffuseColor = new Color3D(
                            ((color >> 16) & 0xFF) / 255.0,
                            ((color >> 8) & 0xFF) / 255.0,
                            (color & 0xFF) / 255.0
                        );
                    }
                    
                    if (matObj.has("opacity")) {
                        material.opacity = matObj.get("opacity").getAsDouble();
                    }
                    
                    if (matObj.has("transparent")) {
                        material.transparent = matObj.get("transparent").getAsBoolean();
                    }
                    
                    model.addMaterial(material);
                }
            }
            
            // Parse faces
            if (data.has("faces")) {
                JsonArray faces = data.get("faces").getAsJsonArray();
                parseFaces(model, faces);
            }
        }
        
        return model;
    }
    
    private void parseFaces(Model3D model, JsonArray faces) {
        for (int i = 0; i < faces.size(); ) {
            int type = faces.get(i).getAsInt();
            i++;
            
            // Parse based on face type
            if (type == 0) { // Triangle
                List<Integer> vertices = new ArrayList<>();
                for (int j = 0; j < 3; j++) {
                    vertices.add(faces.get(i).getAsInt());
                    i++;
                }
                
                int materialIndex = 0;
                if (i < faces.size()) {
                    materialIndex = faces.get(i).getAsInt();
                    i++;
                }
                
                model.addFace(vertices, materialIndex);
            } else if (type == 1) { // Quad
                List<Integer> vertices = new ArrayList<>();
                for (int j = 0; j < 4; j++) {
                    vertices.add(faces.get(i).getAsInt());
                    i++;
                }
                
                int materialIndex = 0;
                if (i < faces.size()) {
                    materialIndex = faces.get(i).getAsInt();
                    i++;
                }
                
                model.addFace(vertices, materialIndex);
            } else {
                // Skip unknown face types
                i++;
            }
        }
    }
}

// Model generators
class ModelGenerator {
    public Model3D createCube(double size, String name) {
        Model3D model = new Model3D(name);
        double halfSize = size / 2;
        
        // Add vertices
        model.addVertex(-halfSize, -halfSize, -halfSize);
        model.addVertex(halfSize, -halfSize, -halfSize);
        model.addVertex(halfSize, halfSize, -halfSize);
        model.addVertex(-halfSize, halfSize, -halfSize);
        model.addVertex(-halfSize, -halfSize, halfSize);
        model.addVertex(halfSize, -halfSize, halfSize);
        model.addVertex(halfSize, halfSize, halfSize);
        model.addVertex(-halfSize, halfSize, halfSize);
        
        // Add UVs
        model.addUV(0, 0);
        model.addUV(1, 0);
        model.addUV(1, 1);
        model.addUV(0, 1);
        
        // Add faces
        int[][] faceIndices = {
            {0, 1, 2}, {0, 2, 3}, // Front
            {5, 4, 7}, {5, 7, 6}, // Back
            {4, 0, 3}, {4, 3, 7}, // Left
            {1, 5, 6}, {1, 6, 2}, // Right
            {3, 2, 6}, {3, 6, 7}, // Top
            {4, 5, 1}, {4, 1, 0}  // Bottom
        };
        
        for (int[] face : faceIndices) {
            model.addFace(Arrays.asList(face[0], face[1], face[2]));
        }
        
        Material material = new Material("CubeMaterial", new Color3D(0.8, 0.8, 0.8));
        model.addMaterial(material);
        model.calculateNormals();
        
        return model;
    }
    
    public Model3D createSphere(double radius, int segments, int rings, String name) {
        Model3D model = new Model3D(name);
        
        // Generate vertices
        for (int ring = 0; ring <= rings; ring++) {
            double theta = Math.PI * ring / rings;
            double sinTheta = Math.sin(theta);
            double cosTheta = Math.cos(theta);
            
            for (int segment = 0; segment < segments; segment++) {
                double phi = 2 * Math.PI * segment / segments;
                double sinPhi = Math.sin(phi);
                double cosPhi = Math.cos(phi);
                
                double x = radius * sinTheta * cosPhi;
                double y = radius * cosTheta;
                double z = radius * sinTheta * sinPhi;
                
                model.addVertex(x, y, z);
                model.addUV((double)segment / segments, (double)ring / rings);
            }
        }
        
        // Generate faces
        for (int ring = 0; ring < rings; ring++) {
            for (int segment = 0; segment < segments; segment++) {
                int current = ring * segments + segment;
                int nextRing = (ring + 1) * segments + segment;
                int nextSegment = ring * segments + ((segment + 1) % segments);
                int nextBoth = (ring + 1) * segments + ((segment + 1) % segments);
                
                if (ring != 0) {
                    model.addFace(Arrays.asList(current, nextRing, nextSegment));
                }
                if (ring != rings - 1) {
                    model.addFace(Arrays.asList(nextSegment, nextRing, nextBoth));
                }
            }
        }
        
        Material material = new Material("SphereMaterial", new Color3D(0.7, 0.7, 1.0));
        model.addMaterial(material);
        model.calculateNormals();
        
        return model;
    }
    
    public Model3D createPlane(double width, double height, String name) {
        Model3D model = new Model3D(name);
        
        double halfWidth = width / 2;
        double halfHeight = height / 2;
        
        model.addVertex(-halfWidth, 0, -halfHeight);
        model.addVertex(halfWidth, 0, -halfHeight);
        model.addVertex(halfWidth, 0, halfHeight);
        model.addVertex(-halfWidth, 0, halfHeight);
        
        model.addUV(0, 0);
        model.addUV(1, 0);
        model.addUV(1, 1);
        model.addUV(0, 1);
        
        model.addFace(Arrays.asList(0, 1, 2));
        model.addFace(Arrays.asList(0, 2, 3));
        
        Material material = new Material("PlaneMaterial", new Color3D(0.8, 1.0, 0.8));
        model.addMaterial(material);
        model.calculateNormals();
        
        return model;
    }
    
    public Model3D createCylinder(double radius, double height, int segments, String name) {
        Model3D model = new Model3D(name);
        double halfHeight = height / 2;
        
        // Bottom center
        model.addVertex(0, -halfHeight, 0);
        // Top center
        model.addVertex(0, halfHeight, 0);
        
        // Bottom and top vertices
        for (int i = 0; i < segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            
            model.addVertex(x, -halfHeight, z);  // Bottom
            model.addVertex(x, halfHeight, z);   // Top
            
            model.addUV((double)i / segments, 0);  // Bottom UV
            model.addUV((double)i / segments, 1);  // Top UV
        }
        
        // Create faces
        for (int i = 0; i < segments; i++) {
            int nextI = (i + 1) % segments;
            
            int bottomCurrent = 2 + i * 2;
            int bottomNext = 2 + nextI * 2;
            int topCurrent = 3 + i * 2;
            int topNext = 3 + nextI * 2;
            
            // Bottom face
            model.addFace(Arrays.asList(0, bottomNext, bottomCurrent));
            // Top face
            model.addFace(Arrays.asList(1, topCurrent, topNext));
            // Side faces
            model.addFace(Arrays.asList(bottomCurrent, bottomNext, topNext));
            model.addFace(Arrays.asList(bottomCurrent, topNext, topCurrent));
        }
        
        Material material = new Material("CylinderMaterial", new Color3D(1.0, 0.8, 0.6));
        model.addMaterial(material);
        model.calculateNormals();
        
        return model;
    }
}

// Model exporters without Gson
class ModelExporter {
    
    public String exportThreeJS(Model3D model) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        // Metadata
        json.append("  \"metadata\": {\n");
        json.append("    \"version\": 4.5,\n");
        json.append("    \"type\": \"Geometry\",\n");
        json.append("    \"generator\": \"JavaModel3DGenerator\"\n");
        json.append("  },\n");
        
        json.append("  \"name\": \"").append(escapeJson(model.name)).append("\",\n");
        
        // Data
        json.append("  \"data\": {\n");
        
        // Vertices
        json.append("    \"vertices\": [");
        for (int i = 0; i < model.vertices.size(); i++) {
            if (i > 0) json.append(", ");
            Vector3 vertex = model.vertices.get(i);
            json.append(vertex.x).append(", ").append(vertex.y).append(", ").append(vertex.z);
        }
        json.append("],\n");
        
        // Normals
        json.append("    \"normals\": [");
        for (int i = 0; i < model.normals.size(); i++) {
            if (i > 0) json.append(", ");
            Vector3 normal = model.normals.get(i);
            json.append(normal.x).append(", ").append(normal.y).append(", ").append(normal.z);
        }
        json.append("],\n");
        
        // UVs
        json.append("    \"uvs\": [[");
        for (int i = 0; i < model.uvs.size(); i++) {
            if (i > 0) json.append(", ");
            Vector2 uv = model.uvs.get(i);
            json.append(uv.x).append(", ").append(uv.y);
        }
        json.append("]],\n");
        
        // Faces
        json.append("    \"faces\": [");
        for (int i = 0; i < model.faces.size(); i++) {
            if (i > 0) json.append(", ");
            Face face = model.faces.get(i);
            json.append("0"); // Face type
            for (int vertexIdx : face.vertices) {
                json.append(", ").append(vertexIdx);
            }
            json.append(", ").append(face.materialIndex);
        }
        json.append("],\n");
        
        // Materials
        json.append("    \"materials\": [");
        for (int i = 0; i < model.materials.size(); i++) {
            if (i > 0) json.append(", ");
            Material material = model.materials.get(i);
            json.append("{\n");
            json.append("      \"uuid\": \"").append(java.util.UUID.randomUUID().toString()).append("\",\n");
            json.append("      \"type\": \"MeshLambertMaterial\",\n");
            json.append("      \"name\": \"").append(escapeJson(material.name)).append("\",\n");
            json.append("      \"color\": ").append(material.diffuseColor.toHex()).append(",\n");
            json.append("      \"emissive\": ").append(material.emissiveColor != null ? material.emissiveColor.toHex() : 0).append(",\n");
            json.append("      \"transparent\": ").append(material.transparent).append(",\n");
            json.append("      \"opacity\": ").append(material.opacity);
            if (material.diffuseTexture != null) {
                json.append(",\n      \"map\": \"").append(escapeJson(material.diffuseTexture)).append("\"");
            }
            if (material.normalTexture != null) {
                json.append(",\n      \"normalMap\": \"").append(escapeJson(material.normalTexture)).append("\"");
            }
            json.append("\n    }");
        }
        json.append("]\n");
        
        json.append("  }");
        
        // Animations
        if (!model.animations.isEmpty()) {
            json.append(",\n  \"animations\": [");
            for (int i = 0; i < model.animations.size(); i++) {
                if (i > 0) json.append(", ");
                Animation anim = model.animations.get(i);
                json.append("{\n");
                json.append("    \"name\": \"").append(escapeJson(anim.name)).append("\",\n");
                json.append("    \"duration\": ").append(anim.duration).append(",\n");
                json.append("    \"tracks\": [");
                // Simple keyframe representation - expand based on your needs
                json.append("]\n");
                json.append("  }");
            }
            json.append("]");
        }
        
        json.append("\n}");
        return json.toString();
    }
    
    public String exportBabylonJS(Model3D model) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        // Producer info
        json.append("  \"producer\": {\n");
        json.append("    \"name\": \"JavaModel3DGenerator\",\n");
        json.append("    \"version\": \"1.0\",\n");
        json.append("    \"exporter_version\": \"1.0\"\n");
        json.append("  },\n");
        
        json.append("  \"autoClear\": true,\n");
        json.append("  \"clearColor\": [0.2, 0.2, 0.3],\n");
        json.append("  \"ambientColor\": [0, 0, 0],\n");
        json.append("  \"gravity\": [0, -9.81, 0],\n");
        
        // Meshes
        json.append("  \"meshes\": [{\n");
        json.append("    \"name\": \"").append(escapeJson(model.name)).append("\",\n");
        json.append("    \"id\": \"").append(escapeJson(model.name)).append("\",\n");
        json.append("    \"materialId\": \"").append(!model.materials.isEmpty() ? escapeJson(model.materials.get(0).name) : "").append("\",\n");
        json.append("    \"position\": [0, 0, 0],\n");
        json.append("    \"rotation\": [0, 0, 0],\n");
        json.append("    \"scaling\": [1, 1, 1],\n");
        json.append("    \"isVisible\": true,\n");
        json.append("    \"freezeWorldMatrix\": false,\n");
        json.append("    \"isEnabled\": true,\n");
        json.append("    \"checkCollisions\": false,\n");
        json.append("    \"billboardMode\": 0,\n");
        json.append("    \"receiveShadows\": false,\n");
        
        // Positions
        json.append("    \"positions\": [");
        for (int i = 0; i < model.vertices.size(); i++) {
            if (i > 0) json.append(", ");
            Vector3 vertex = model.vertices.get(i);
            json.append(vertex.x).append(", ").append(vertex.y).append(", ").append(vertex.z);
        }
        json.append("],\n");
        
        // Normals
        json.append("    \"normals\": [");
        for (int i = 0; i < model.normals.size(); i++) {
            if (i > 0) json.append(", ");
            Vector3 normal = model.normals.get(i);
            json.append(normal.x).append(", ").append(normal.y).append(", ").append(normal.z);
        }
        json.append("],\n");
        
        // UVs
        json.append("    \"uvs\": [");
        for (int i = 0; i < model.uvs.size(); i++) {
            if (i > 0) json.append(", ");
            Vector2 uv = model.uvs.get(i);
            json.append(uv.x).append(", ").append(uv.y);
        }
        json.append("],\n");
        
        // Indices
        json.append("    \"indices\": [");
        boolean first = true;
        for (Face face : model.faces) {
            for (int vertexIdx : face.vertices) {
                if (!first) json.append(", ");
                json.append(vertexIdx);
                first = false;
            }
        }
        json.append("]\n");
        json.append("  }],\n");
        
        // Materials
        json.append("  \"materials\": [");
        for (int i = 0; i < model.materials.size(); i++) {
            if (i > 0) json.append(", ");
            Material material = model.materials.get(i);
            json.append("{\n");
            json.append("    \"name\": \"").append(escapeJson(material.name)).append("\",\n");
            json.append("    \"id\": \"").append(escapeJson(material.name)).append("\",\n");
            json.append("    \"diffuse\": [").append(material.diffuseColor.r).append(", ").append(material.diffuseColor.g).append(", ").append(material.diffuseColor.b).append("],\n");
            json.append("    \"specular\": [").append(material.specularColor != null ? material.specularColor.r : 1).append(", ").append(material.specularColor != null ? material.specularColor.g : 1).append(", ").append(material.specularColor != null ? material.specularColor.b : 1).append("],\n");
            json.append("    \"emissive\": [").append(material.emissiveColor != null ? material.emissiveColor.r : 0).append(", ").append(material.emissiveColor != null ? material.emissiveColor.g : 0).append(", ").append(material.emissiveColor != null ? material.emissiveColor.b : 0).append("],\n");
            json.append("    \"ambient\": [").append(material.ambientColor != null ? material.ambientColor.r : 1).append(", ").append(material.ambientColor != null ? material.ambientColor.g : 1).append(", ").append(material.ambientColor != null ? material.ambientColor.b : 1).append("],\n");
            json.append("    \"specularPower\": ").append(material.shininess).append(",\n");
            json.append("    \"alpha\": ").append(material.opacity).append(",\n");
            json.append("    \"backFaceCulling\": true,\n");
            json.append("    \"wireframe\": false");
            
            if (material.diffuseTexture != null) {
                json.append(",\n    \"diffuseTexture\": {\n");
                json.append("      \"name\": \"").append(escapeJson(material.diffuseTexture)).append("\",\n");
                json.append("      \"level\": 1.0,\n");
                json.append("      \"hasAlpha\": false,\n");
                json.append("      \"coordinatesMode\": 0,\n");
                json.append("      \"uOffset\": 0,\n");
                json.append("      \"vOffset\": 0,\n");
                json.append("      \"uScale\": 1.0,\n");
                json.append("      \"vScale\": 1.0,\n");
                json.append("      \"uAng\": 0,\n");
                json.append("      \"vAng\": 0,\n");
                json.append("      \"wAng\": 0,\n");
                json.append("      \"wrapU\": 1,\n");
                json.append("      \"wrapV\": 1,\n");
                json.append("      \"coordinatesIndex\": 0\n");
                json.append("    }");
            }
            
            json.append("\n  }");
        }
        json.append("],\n");
        
        json.append("  \"cameras\": [],\n");
        json.append("  \"lights\": []\n");
        
        json.append("}");
        return json.toString();
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}

// 3D Viewport using Java2D for rendering
class Viewport3D extends JPanel {
    private double rotationX = 20;
    private double rotationY = 45;
    private double zoom = 100;
    private double panX = 0;
    private double panY = 0;
    
    private EditMode editMode = EditMode.OBJECT;
    private Tool currentTool = Tool.SELECT;
    
    private Set<Integer> selectedVertices = new HashSet<>();
    private Set<Integer> selectedFaces = new HashSet<>();
    private Set<Integer> selectedEdges = new HashSet<>();
    
    private boolean isDragging = false;
    private int lastX, lastY;
    
    private Model3D model;
    private Runnable onSelectionChanged;
    
    public Viewport3D(Runnable onSelectionChanged) {
        this.onSelectionChanged = onSelectionChanged;
        setBackground(new Color(42, 42, 42));
        setPreferredSize(new Dimension(600, 400));
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
                isDragging = false;
                
                if (currentTool == Tool.SELECT && model != null) {
                    handleSelection(e.getX(), e.getY(), e.isShiftDown());
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
            }
        });
        
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastX;
                int dy = e.getY() - lastY;
                
                if (!isDragging && (Math.abs(dx) > 3 || Math.abs(dy) > 3)) {
                    isDragging = true;
                }
                
                if (isDragging) {
                    if (currentTool == Tool.SELECT && editMode == EditMode.OBJECT) {
                        rotationY += dx * 0.5;
                        rotationX += dy * 0.5;
                        repaint();
                    } else if (currentTool == Tool.MOVE && hasSelection()) {
                        moveSelection(dx, dy);
                    }
                }
                
                lastX = e.getX();
                lastY = e.getY();
            }
        });
        
        addMouseWheelListener(e -> {
            if (e.getWheelRotation() < 0) {
                zoom *= 1.1;
            } else {
                zoom *= 0.9;
            }
            zoom = Math.max(10, Math.min(500, zoom));
            repaint();
        });
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE && hasSelection()) {
                    deleteSelected();
                } else if (e.getKeyCode() == KeyEvent.VK_E && editMode == EditMode.FACE && !selectedFaces.isEmpty()) {
                    extrudeSelectedFaces();
                } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    cycleEditMode();
                }
            }
        });
        
        setFocusable(true);
    }
    
    public void setEditMode(EditMode mode) {
        this.editMode = mode;
        clearSelection();
        repaint();
    }
    
    public void setTool(Tool tool) {
        this.currentTool = tool;
    }
    
    public void clearSelection() {
        selectedVertices.clear();
        selectedFaces.clear();
        selectedEdges.clear();
        if (onSelectionChanged != null) {
            onSelectionChanged.run();
        }
    }
    
    private void handleSelection(int x, int y, boolean addToSelection) {
        if (model == null) return;
        
        if (editMode == EditMode.VERTEX) {
            Integer vertexIdx = getVertexAtPosition(x, y);
            if (vertexIdx != null) {
                if (addToSelection) {
                    if (selectedVertices.contains(vertexIdx)) {
                        selectedVertices.remove(vertexIdx);
                    } else {
                        selectedVertices.add(vertexIdx);
                    }
                } else {
                    selectedVertices.clear();
                    selectedVertices.add(vertexIdx);
                }
                repaint();
                if (onSelectionChanged != null) {
                    onSelectionChanged.run();
                }
            }
        } else if (editMode == EditMode.FACE) {
            Integer faceIdx = getFaceAtPosition(x, y);
            if (faceIdx != null) {
                if (addToSelection) {
                    if (selectedFaces.contains(faceIdx)) {
                        selectedFaces.remove(faceIdx);
                    } else {
                        selectedFaces.add(faceIdx);
                    }
                } else {
                    selectedFaces.clear();
                    selectedFaces.add(faceIdx);
                }
                repaint();
                if (onSelectionChanged != null) {
                    onSelectionChanged.run();
                }
            }
        }
    }
    
    private Integer getVertexAtPosition(int x, int y) {
        if (model == null) return null;
        
        for (int i = 0; i < model.vertices.size(); i++) {
            Vector3 vertex = model.vertices.get(i);
            Point screenPoint = projectPoint(vertex.x, vertex.y, vertex.z);
            double distance = Math.sqrt(Math.pow(x - screenPoint.x, 2) + Math.pow(y - screenPoint.y, 2));
            if (distance <= 10) {
                return i;
            }
        }
        return null;
    }
    
    private Integer getFaceAtPosition(int x, int y) {
        if (model == null) return null;
        
        for (int i = 0; i < model.faces.size(); i++) {
            Face face = model.faces.get(i);
            if (face.vertices.size() >= 3) {
                List<Point> points = new ArrayList<>();
                for (int vertexIdx : face.vertices) {
                    if (vertexIdx < model.vertices.size()) {
                        Vector3 vertex = model.vertices.get(vertexIdx);
                        points.add(projectPoint(vertex.x, vertex.y, vertex.z));
                    }
                }
                
                if (pointInPolygon(x, y, points)) {
                    return i;
                }
            }
        }
        return null;
    }
    
    private boolean pointInPolygon(int x, int y, List<Point> polygon) {
        boolean inside = false;
        int j = polygon.size() - 1;
        
        for (int i = 0; i < polygon.size(); i++) {
            Point pi = polygon.get(i);
            Point pj = polygon.get(j);
            
            if ((pi.y > y) != (pj.y > y) && 
                (x < (pj.x - pi.x) * (y - pi.y) / (pj.y - pi.y) + pi.x)) {
                inside = !inside;
            }
            j = i;
        }
        
        return inside;
    }
    
    private boolean hasSelection() {
        return !selectedVertices.isEmpty() || !selectedFaces.isEmpty() || !selectedEdges.isEmpty();
    }
    
    private void moveSelection(int dx, int dy) {
        if (model == null) return;
        
        double factor = 0.01 / zoom * 100;
        
        for (int vertexIdx : selectedVertices) {
            if (vertexIdx < model.vertices.size()) {
                Vector3 vertex = model.vertices.get(vertexIdx);
                vertex.x += dx * factor;
                vertex.y -= dy * factor;
            }
        }
        repaint();
    }
    
    private void deleteSelected() {
        if (model == null) return;
        
        if (!selectedVertices.isEmpty()) {
            List<Integer> sortedVertices = new ArrayList<>(selectedVertices);
            sortedVertices.sort(Collections.reverseOrder());
            for (int vertexIdx : sortedVertices) {
                model.removeVertex(vertexIdx);
            }
            selectedVertices.clear();
        } else if (!selectedFaces.isEmpty()) {
            List<Integer> sortedFaces = new ArrayList<>(selectedFaces);
            sortedFaces.sort(Collections.reverseOrder());
            for (int faceIdx : sortedFaces) {
                model.removeFace(faceIdx);
            }
            selectedFaces.clear();
        }
        
        model.calculateNormals();
        repaint();
        if (onSelectionChanged != null) {
            onSelectionChanged.run();
        }
    }
    
    private void extrudeSelectedFaces() {
        if (model == null || selectedFaces.isEmpty()) return;
        
        for (int faceIdx : new ArrayList<>(selectedFaces)) {
            model.extrudeFace(faceIdx, 0.5);
        }
        
        model.calculateNormals();
        repaint();
    }
    
    private void cycleEditMode() {
        EditMode[] modes = EditMode.values();
        int currentIdx = Arrays.asList(modes).indexOf(editMode);
        int nextIdx = (currentIdx + 1) % modes.length;
        setEditMode(modes[nextIdx]);
    }
    
    private Point projectPoint(double x, double y, double z) {
        double cosX = Math.cos(Math.toRadians(rotationX));
        double sinX = Math.sin(Math.toRadians(rotationX));
        double cosY = Math.cos(Math.toRadians(rotationY));
        double sinY = Math.sin(Math.toRadians(rotationY));
        
        // Rotate around Y axis
        double newX = x * cosY - z * sinY;
        double newZ = x * sinY + z * cosY;
        
        // Rotate around X axis
        double newY = y * cosX - newZ * sinX;
        newZ = y * sinX + newZ * cosX;
        
        // Project to 2D
        int screenX = (int)(newX * zoom + getWidth() / 2 + panX);
        int screenY = (int)(newY * zoom + getHeight() / 2 + panY);
        
        return new Point(screenX, screenY);
    }
    
    public void drawModel(Model3D model) {
        this.model = model;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (model == null || model.vertices.isEmpty()) {
            return;
        }
        
        // Project all vertices
        List<Point> projected = new ArrayList<>();
        for (Vector3 vertex : model.vertices) {
            projected.add(projectPoint(vertex.x, vertex.y, vertex.z));
        }
        
        // Draw faces
        if (editMode != EditMode.VERTEX) {
            for (int i = 0; i < model.faces.size(); i++) {
                Face face = model.faces.get(i);
                if (face.vertices.size() >= 3) {
                    int[] xPoints = new int[face.vertices.size()];
                    int[] yPoints = new int[face.vertices.size()];
                    
                    for (int j = 0; j < face.vertices.size(); j++) {
                        int vertexIdx = face.vertices.get(j);
                        if (vertexIdx < projected.size()) {
                            Point p = projected.get(vertexIdx);
                            xPoints[j] = p.x;
                            yPoints[j] = p.y;
                        }
                    }
                    
                    // Get material color
                    Color color = Color.GRAY;
                    if (face.materialIndex < model.materials.size()) {
                        Material material = model.materials.get(face.materialIndex);
                        color = material.diffuseColor.toAWTColor();
                    }
                    
                    // Highlight selected faces
                    if (selectedFaces.contains(i)) {
                        color = Color.ORANGE;
                    }
                    
                    g2d.setColor(color);
                    g2d.fillPolygon(xPoints, yPoints, face.vertices.size());
                    
                    g2d.setColor(selectedFaces.contains(i) ? Color.ORANGE.darker() : Color.DARK_GRAY);
                    g2d.drawPolygon(xPoints, yPoints, face.vertices.size());
                }
            }
        }
        
        // Draw wireframe
        if (editMode == EditMode.EDGE || editMode == EditMode.VERTEX) {
            g2d.setColor(Color.LIGHT_GRAY);
            for (Face face : model.faces) {
                for (int i = 0; i < face.vertices.size(); i++) {
                    int v1Idx = face.vertices.get(i);
                    int v2Idx = face.vertices.get((i + 1) % face.vertices.size());
                    
                    if (v1Idx < projected.size() && v2Idx < projected.size()) {
                        Point p1 = projected.get(v1Idx);
                        Point p2 = projected.get(v2Idx);
                        g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                    }
                }
            }
        }
        
        // Draw vertices
        if (editMode == EditMode.VERTEX) {
            for (int i = 0; i < projected.size(); i++) {
                Point p = projected.get(i);
                int radius = selectedVertices.contains(i) ? 5 : 3;
                Color color = selectedVertices.contains(i) ? Color.ORANGE : Color.WHITE;
                
                g2d.setColor(color);
                g2d.fillOval(p.x - radius, p.y - radius, radius * 2, radius * 2);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(p.x - radius, p.y - radius, radius * 2, radius * 2);
            }
        }
        
        // Draw face centers
        else if (editMode == EditMode.FACE) {
            for (int i = 0; i < model.faces.size(); i++) {
                Vector3 center = model.getFaceCenter(i);
                Point p = projectPoint(center.x, center.y, center.z);
                
                int radius = selectedFaces.contains(i) ? 4 : 2;
                Color color = selectedFaces.contains(i) ? Color.ORANGE : Color.GRAY;
                
                g2d.setColor(color);
                g2d.fillOval(p.x - radius, p.y - radius, radius * 2, radius * 2);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(p.x - radius, p.y - radius, radius * 2, radius * 2);
            }
        }
    }
    
    public Set<Integer> getSelectedVertices() { return selectedVertices; }
    public Set<Integer> getSelectedFaces() { return selectedFaces; }
    public Set<Integer> getSelectedEdges() { return selectedEdges; }
}

// Supporting classes
class EditModeToolbar extends JPanel {
    private Viewport3D viewport;
    private JLabel selectionLabel;
    
    public EditModeToolbar(Viewport3D viewport) {
        this.viewport = viewport;
        setBorder(new TitledBorder("Edit Tools"));
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Edit mode selection
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        add(new JLabel("Mode:"), gbc);
        
        ButtonGroup modeGroup = new ButtonGroup();
        gbc.gridx = 1;
        for (EditMode mode : EditMode.values()) {
            JRadioButton radio = new JRadioButton(mode.getValue());
            radio.setSelected(mode == EditMode.OBJECT);
            radio.addActionListener(e -> viewport.setEditMode(mode));
            modeGroup.add(radio);
            add(radio, gbc);
            gbc.gridx++;
        }
        
        // Tool selection
        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Tool:"), gbc);
        
        ButtonGroup toolGroup = new ButtonGroup();
        gbc.gridx = 1;
        Tool[] tools = {Tool.SELECT, Tool.MOVE, Tool.ROTATE, Tool.SCALE, Tool.DELETE};
        for (Tool tool : tools) {
            JRadioButton radio = new JRadioButton(tool.getValue());
            radio.setSelected(tool == Tool.SELECT);
            radio.addActionListener(e -> viewport.setTool(tool));
            toolGroup.add(radio);
            add(radio, gbc);
            gbc.gridx++;
        }
        
        // Selection info
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 6;
        selectionLabel = new JLabel("Nothing selected");
        add(selectionLabel, gbc);
    }
    
    public void updateSelectionInfo() {
        int vertices = viewport.getSelectedVertices().size();
        int faces = viewport.getSelectedFaces().size();
        int edges = viewport.getSelectedEdges().size();
        
        if (vertices > 0) {
            selectionLabel.setText(vertices + " vertex" + (vertices != 1 ? "es" : "") + " selected");
        } else if (faces > 0) {
            selectionLabel.setText(faces + " face" + (faces != 1 ? "s" : "") + " selected");
        } else if (edges > 0) {
            selectionLabel.setText(edges + " edge" + (edges != 1 ? "s" : "") + " selected");
        } else {
            selectionLabel.setText("Nothing selected");
        }
    }
}

class MaterialEditor extends JPanel {
    private Material currentMaterial;
    private Runnable onMaterialChanged;
    private JTextField nameField;
    private JButton diffuseButton;
    private JSlider opacitySlider;
    private JSlider shininessSlider;
    
    public MaterialEditor(Runnable onMaterialChanged) {
        this.onMaterialChanged = onMaterialChanged;
        setBorder(new TitledBorder("Material Editor"));
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        nameField = new JTextField();
        nameField.addActionListener(e -> onChange());
        add(nameField, gbc);
        
        // Diffuse color
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        add(new JLabel("Diffuse:"), gbc);
        gbc.gridx = 1;
        diffuseButton = new JButton("   ");
        diffuseButton.addActionListener(e -> chooseDiffuseColor());
        add(diffuseButton, gbc);
        
        // Opacity
        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Opacity:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        opacitySlider = new JSlider(0, 100, 100);
        opacitySlider.addChangeListener(e -> onChange());
        add(opacitySlider, gbc);
        
        // Shininess
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        add(new JLabel("Shininess:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        shininessSlider = new JSlider(1, 100, 30);
        shininessSlider.addChangeListener(e -> onChange());
        add(shininessSlider, gbc);
    }
    
    private void chooseDiffuseColor() {
        if (currentMaterial != null) {
            Color color = JColorChooser.showDialog(this, "Choose Diffuse Color", 
                currentMaterial.diffuseColor.toAWTColor());
            if (color != null) {
                currentMaterial.diffuseColor = new Color3D(
                    color.getRed() / 255.0, 
                    color.getGreen() / 255.0, 
                    color.getBlue() / 255.0
                );
                diffuseButton.setBackground(color);
                onChange();
            }
        }
    }
    
    private void onChange() {
        if (currentMaterial != null && onMaterialChanged != null) {
            currentMaterial.name = nameField.getText();
            currentMaterial.opacity = opacitySlider.getValue() / 100.0;
            currentMaterial.shininess = shininessSlider.getValue();
            onMaterialChanged.run();
        }
    }
    
    public void setMaterial(Material material) {
        this.currentMaterial = material;
        if (material != null) {
            nameField.setText(material.name);
            opacitySlider.setValue((int)(material.opacity * 100));
            shininessSlider.setValue((int)material.shininess);
            diffuseButton.setBackground(material.diffuseColor.toAWTColor());
        } else {
            nameField.setText("");
            opacitySlider.setValue(100);
            shininessSlider.setValue(30);
            diffuseButton.setBackground(Color.WHITE);
        }
    }
}

// Main application class
public class Model3DApp extends JFrame {
    private ModelGenerator generator;
    private ModelExporter exporter;
    private ThreeJSLoader threeJSLoader;
    private Model3D currentModel;
    private List<Model3D> models;
    
    private JList<String> modelListbox;
    private DefaultListModel<String> listModel;
    private JTextField modelNameField;
    private JLabel vertexCountLabel;
    private JLabel faceCountLabel;
    private JSlider sizeSlider;
    private JSlider detailSlider;
    
    private Viewport3D viewport;
    private EditModeToolbar editToolbar;
    private MaterialEditor materialEditor;
    
    public Model3DApp() {
        generator = new ModelGenerator();
        exporter = new ModelExporter();
        threeJSLoader = new ThreeJSLoader();
        models = new CopyOnWriteArrayList<>();
        
        initializeUI();
        createCube(); // Start with a default cube
    }
    
    private void initializeUI() {
        setTitle("3D Model Creator - Java Edition (No External Dependencies)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        
        createMenuBar();
        createMainPanel();
    }
    
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem(new AbstractAction("New Project") {
            @Override
            public void actionPerformed(ActionEvent e) {
                newProject();
            }
        }));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(new AbstractAction("Load Three.js JSON") {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadThreeJSModel();
            }
        }));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(new AbstractAction("Export Three.js JSON") {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportModel(ExportFormat.THREEJS);
            }
        }));
        fileMenu.add(new JMenuItem(new AbstractAction("Export Babylon.js JSON") {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportModel(ExportFormat.BABYLONJS);
            }
        }));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem(new AbstractAction("Exit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        }));
        
        // Create menu
        JMenu createMenu = new JMenu("Create");
        createMenu.add(new JMenuItem(new AbstractAction("Cube") {
            @Override
            public void actionPerformed(ActionEvent e) {
                createCube();
            }
        }));
        createMenu.add(new JMenuItem(new AbstractAction("Sphere") {
            @Override
            public void actionPerformed(ActionEvent e) {
                createSphere();
            }
        }));
        createMenu.add(new JMenuItem(new AbstractAction("Plane") {
            @Override
            public void actionPerformed(ActionEvent e) {
                createPlane();
            }
        }));
        createMenu.add(new JMenuItem(new AbstractAction("Cylinder") {
            @Override
            public void actionPerformed(ActionEvent e) {
                createCylinder();
            }
        }));
        
        menuBar.add(fileMenu);
        menuBar.add(createMenu);
        setJMenuBar(menuBar);
    }
    
    private void createMainPanel() {
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(300);
        
        // Left panel
        JPanel leftPanel = createLeftPanel();
        mainSplit.setLeftComponent(leftPanel);
        
        // Right panel
        JPanel rightPanel = createRightPanel();
        mainSplit.setRightComponent(rightPanel);
        
        add(mainSplit);
    }
    
    private JPanel createLeftPanel() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        
        // Model list
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBorder(new TitledBorder("Models"));
        
        listModel = new DefaultListModel<>();
        modelListbox = new JList<>(listModel);
        modelListbox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        modelListbox.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                onModelSelect();
            }
        });
        listPanel.add(new JScrollPane(modelListbox), BorderLayout.CENTER);
        
        // Properties panel
        JPanel propsPanel = new JPanel(new GridBagLayout());
        propsPanel.setBorder(new TitledBorder("Properties"));
        GridBagConstraints gbc = new GridBagConstraints();
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        propsPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        modelNameField = new JTextField();
        modelNameField.addActionListener(e -> onModelNameChange());
        propsPanel.add(modelNameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        propsPanel.add(new JLabel("Vertices:"), gbc);
        gbc.gridx = 1;
        vertexCountLabel = new JLabel("0");
        propsPanel.add(vertexCountLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        propsPanel.add(new JLabel("Faces:"), gbc);
        gbc.gridx = 1;
        faceCountLabel = new JLabel("0");
        propsPanel.add(faceCountLabel, gbc);
        
        // Creation parameters
        JPanel createPanel = new JPanel(new GridBagLayout());
        createPanel.setBorder(new TitledBorder("Creation Parameters"));
        gbc = new GridBagConstraints();
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        createPanel.add(new JLabel("Size:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        sizeSlider = new JSlider(1, 50, 20);
        createPanel.add(sizeSlider, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        createPanel.add(new JLabel("Detail:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        detailSlider = new JSlider(4, 32, 16);
        createPanel.add(detailSlider, gbc);
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(listPanel, BorderLayout.CENTER);
        topPanel.add(propsPanel, BorderLayout.SOUTH);
        
        leftPanel.add(topPanel, BorderLayout.NORTH);
        leftPanel.add(createPanel, BorderLayout.CENTER);
        
        return leftPanel;
    }
    
    private JPanel createRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        // Viewport
        JPanel viewportPanel = new JPanel(new BorderLayout());
        viewportPanel.setBorder(new TitledBorder("3D Viewport"));
        
        viewport = new Viewport3D(this::onSelectionChanged);
        viewportPanel.add(viewport, BorderLayout.CENTER);
        
        // Edit toolbar
        editToolbar = new EditModeToolbar(viewport);
        
        // Material editor
        materialEditor = new MaterialEditor(this::onMaterialChanged);
        
        // Controls
        JPanel controlsPanel = new JPanel(new FlowLayout());
        controlsPanel.add(new JButton(new AbstractAction("Delete Model") {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteModel();
            }
        }));
        controlsPanel.add(new JButton(new AbstractAction("Duplicate Model") {
            @Override
            public void actionPerformed(ActionEvent e) {
                duplicateModel();
            }
        }));
        controlsPanel.add(new JButton(new AbstractAction("Reset Camera") {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetCamera();
            }
        }));
        
        // Instructions
        JLabel instructionsLabel = new JLabel("Instructions: Tab=cycle modes, E=extrude faces, Delete=delete selection, Shift+Click=multi-select");
        instructionsLabel.setFont(instructionsLabel.getFont().deriveFont(10f));
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(editToolbar, BorderLayout.NORTH);
        bottomPanel.add(materialEditor, BorderLayout.CENTER);
        bottomPanel.add(controlsPanel, BorderLayout.SOUTH);
        
        rightPanel.add(viewportPanel, BorderLayout.CENTER);
        rightPanel.add(bottomPanel, BorderLayout.SOUTH);
        rightPanel.add(instructionsLabel, BorderLayout.PAGE_END);
        
        return rightPanel;
    }
    
    private void onSelectionChanged() {
        if (editToolbar != null) {
            editToolbar.updateSelectionInfo();
        }
    }
    
    private void onMaterialChanged() {
        viewport.repaint();
    }
    
    private void newProject() {
        models.clear();
        currentModel = null;
        updateModelList();
        updateProperties();
        viewport.drawModel(null);
    }
    
    private void loadThreeJSModel() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("JSON files", "json"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Model3D model = threeJSLoader.loadFromFile(fileChooser.getSelectedFile().getAbsolutePath());
                models.add(model);
                currentModel = model;
                updateModelList();
                updateProperties();
                viewport.drawModel(model);
                JOptionPane.showMessageDialog(this, "Model loaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to load model: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void createCube() {
        double size = sizeSlider.getValue() / 10.0;
        String name = "Cube_" + (models.size() + 1);
        Model3D model = generator.createCube(size, name);
        models.add(model);
        currentModel = model;
        updateModelList();
        updateProperties();
        viewport.drawModel(model);
    }
    
    private void createSphere() {
        double size = sizeSlider.getValue() / 10.0;
        int detail = detailSlider.getValue();
        String name = "Sphere_" + (models.size() + 1);
        Model3D model = generator.createSphere(size, detail, detail / 2, name);
        models.add(model);
        currentModel = model;
        updateModelList();
        updateProperties();
        viewport.drawModel(model);
    }
    
    private void createPlane() {
        double size = sizeSlider.getValue() / 10.0;
        String name = "Plane_" + (models.size() + 1);
        Model3D model = generator.createPlane(size, size, name);
        models.add(model);
        currentModel = model;
        updateModelList();
        updateProperties();
        viewport.drawModel(model);
    }
    
    private void createCylinder() {
        double size = sizeSlider.getValue() / 10.0;
        int detail = detailSlider.getValue();
        String name = "Cylinder_" + (models.size() + 1);
        Model3D model = generator.createCylinder(size, size * 1.5, detail, name);
        models.add(model);
        currentModel = model;
        updateModelList();
        updateProperties();
        viewport.drawModel(model);
    }
    
    private void deleteModel() {
        if (currentModel != null && models.contains(currentModel)) {
            models.remove(currentModel);
            currentModel = null;
            updateModelList();
            updateProperties();
            viewport.drawModel(null);
        }
    }
    
    private void duplicateModel() {
        if (currentModel != null) {
            if (currentModel.name.contains("Cube")) {
                createCube();
            } else if (currentModel.name.contains("Sphere")) {
                createSphere();
            } else if (currentModel.name.contains("Plane")) {
                createPlane();
            } else if (currentModel.name.contains("Cylinder")) {
                createCylinder();
            }
        }
    }
    
    private void resetCamera() {
        // Reset camera would be implemented in viewport
        viewport.repaint();
    }
    
    private void onModelSelect() {
        int index = modelListbox.getSelectedIndex();
        if (index >= 0 && index < models.size()) {
            currentModel = models.get(index);
            updateProperties();
            viewport.clearSelection();
            viewport.drawModel(currentModel);
        }
    }
    
    private void onModelNameChange() {
        if (currentModel != null) {
            currentModel.name = modelNameField.getText();
            updateModelList();
        }
    }
    
    private void updateModelList() {
        listModel.clear();
        for (Model3D model : models) {
            listModel.addElement(model.name);
        }
        
        if (currentModel != null && models.contains(currentModel)) {
            int index = models.indexOf(currentModel);
            modelListbox.setSelectedIndex(index);
        }
    }
    
    private void updateProperties() {
        if (currentModel != null) {
            modelNameField.setText(currentModel.name);
            vertexCountLabel.setText(String.valueOf(currentModel.vertices.size()));
            faceCountLabel.setText(String.valueOf(currentModel.faces.size()));
            
            if (!currentModel.materials.isEmpty()) {
                materialEditor.setMaterial(currentModel.materials.get(0));
            } else {
                materialEditor.setMaterial(null);
            }
        } else {
            modelNameField.setText("");
            vertexCountLabel.setText("0");
            faceCountLabel.setText("0");
            materialEditor.setMaterial(null);
        }
    }
    
    private void exportModel(ExportFormat format) {
        if (currentModel == null) {
            JOptionPane.showMessageDialog(this, "Please select a model to export.", "No Model", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        String extension = format == ExportFormat.THREEJS ? "json" : "babylon";
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            format.getValue().toUpperCase() + " files", extension));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String filename = fileChooser.getSelectedFile().getAbsolutePath();
                if (!filename.endsWith("." + extension)) {
                    filename += "." + extension;
                }
                
                String data;
                if (format == ExportFormat.THREEJS) {
                    data = exporter.exportThreeJS(currentModel);
                } else {
                    data = exporter.exportBabylonJS(currentModel);
                }
                
                Files.write(Paths.get(filename), data.getBytes());
                JOptionPane.showMessageDialog(this, "Model exported to " + filename, "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to export model: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            new Model3DApp().setVisible(true);
        });
    }
}