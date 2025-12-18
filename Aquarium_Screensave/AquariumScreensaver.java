import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AquariumScreensaver extends JPanel {
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;
    private static final int TARGET_FPS = 60;
    
    private List<Fish> fishList;
    private List<Bubble> bubbles;
    private List<Decoration> decorations;
    private List<LightRay> lightRays;
    private Random random;
    private long lastTime;
    
    public AquariumScreensaver() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(0, 50, 100));
        
        random = new Random();
        fishList = new ArrayList<>();
        bubbles = new ArrayList<>();
        decorations = new ArrayList<>();
        lightRays = new ArrayList<>();
        
        // Create fish schools
        createFishSchool(300, 300, 6, FishBehavior.SCHOOLING);
        createFishSchool(1400, 500, 5, FishBehavior.SCHOOLING);
        
        // Add some solo fish with different behaviors
        for (int i = 0; i < 3; i++) {
            fishList.add(new Fish(random.nextInt(WIDTH), random.nextInt(HEIGHT), FishBehavior.WANDERING));
        }
        for (int i = 0; i < 2; i++) {
            fishList.add(new Fish(random.nextInt(WIDTH), random.nextInt(HEIGHT), FishBehavior.FAST_SWIMMER));
        }
        
        // Create decorations
        createDecorations();
        
        // Create light rays
        for (int i = 0; i < 6; i++) {
            lightRays.add(new LightRay(200 + i * 300));
        }
        
        lastTime = System.nanoTime();
        
        // Animation timer
        Timer timer = new Timer(1000 / TARGET_FPS, e -> {
            long currentTime = System.nanoTime();
            double deltaTime = (currentTime - lastTime) / 1_000_000_000.0;
            lastTime = currentTime;
            
            update(deltaTime);
            repaint();
        });
        timer.start();
        
        // Bubble spawner
        Timer bubbleTimer = new Timer(300, e -> {
            if (bubbles.size() < 40) {
                // Spawn bubbles near decorations sometimes
                if (random.nextDouble() < 0.3 && !decorations.isEmpty()) {
                    Decoration dec = decorations.get(random.nextInt(decorations.size()));
                    bubbles.add(new Bubble((int)dec.x + random.nextInt(50) - 25, HEIGHT - 80));
                } else {
                    bubbles.add(new Bubble(random.nextInt(WIDTH), HEIGHT - 80));
                }
            }
        });
        bubbleTimer.start();
    }
    
    private void createFishSchool(int centerX, int centerY, int count, FishBehavior behavior) {
        for (int i = 0; i < count; i++) {
            int offsetX = random.nextInt(100) - 50;
            int offsetY = random.nextInt(100) - 50;
            Fish fish = new Fish(centerX + offsetX, centerY + offsetY, behavior);
            fish.schoolId = fishList.size() / count;
            fishList.add(fish);
        }
    }
    
    private void createDecorations() {
        // Rocks
        decorations.add(new Rock(200, HEIGHT - 150, 80, 60));
        decorations.add(new Rock(500, HEIGHT - 130, 100, 70));
        decorations.add(new Rock(1300, HEIGHT - 140, 90, 65));
        decorations.add(new Rock(1600, HEIGHT - 160, 120, 80));
        
        // Coral
        decorations.add(new Coral(400, HEIGHT - 120, 60, new Color(255, 100, 150)));
        decorations.add(new Coral(900, HEIGHT - 130, 70, new Color(255, 150, 100)));
        decorations.add(new Coral(1500, HEIGHT - 115, 55, new Color(200, 100, 255)));
        
        // Treasure chest
        decorations.add(new TreasureChest(WIDTH / 2 - 50, HEIGHT - 140));
    }
    
    private void update(double dt) {
        // Update fish with schooling behavior
        for (Fish fish : fishList) {
            if (fish.behavior == FishBehavior.SCHOOLING) {
                updateSchoolingBehavior(fish, dt);
            }
            fish.update(dt);
        }
        
        // Update bubbles
        bubbles.removeIf(b -> b.y < -10);
        for (Bubble bubble : bubbles) {
            bubble.update(dt);
        }
        
        // Update light rays
        for (LightRay ray : lightRays) {
            ray.update(dt);
        }
    }
    
    private void updateSchoolingBehavior(Fish fish, double dt) {
        double avgX = 0, avgY = 0;
        double avgVx = 0, avgVy = 0;
        int count = 0;
        
        // Calculate school center and average velocity
        for (Fish other : fishList) {
            if (other.schoolId == fish.schoolId && other != fish) {
                double dist = Math.sqrt(Math.pow(other.x - fish.x, 2) + Math.pow(other.y - fish.y, 2));
                if (dist < 200) {
                    avgX += other.x;
                    avgY += other.y;
                    avgVx += other.vx;
                    avgVy += other.vy;
                    count++;
                }
            }
        }
        
        if (count > 0) {
            avgX /= count;
            avgY /= count;
            avgVx /= count;
            avgVy /= count;
            
            // Steer towards school center
            double cohesionX = (avgX - fish.x) * 0.5;
            double cohesionY = (avgY - fish.y) * 0.5;
            
            // Align with school velocity
            double alignX = (avgVx - fish.vx) * 0.3;
            double alignY = (avgVy - fish.vy) * 0.3;
            
            // Separation - avoid crowding
            double separationX = 0, separationY = 0;
            for (Fish other : fishList) {
                if (other.schoolId == fish.schoolId && other != fish) {
                    double dist = Math.sqrt(Math.pow(other.x - fish.x, 2) + Math.pow(other.y - fish.y, 2));
                    if (dist < 50 && dist > 0) {
                        separationX -= (other.x - fish.x) / dist * 20;
                        separationY -= (other.y - fish.y) / dist * 20;
                    }
                }
            }
            
            // Apply forces
            fish.vx += (cohesionX + alignX + separationX) * dt;
            fish.vy += (cohesionY + alignY + separationY) * dt;
            
            // Limit speed
            double speed = Math.sqrt(fish.vx * fish.vx + fish.vy * fish.vy);
            if (speed > fish.maxSpeed) {
                fish.vx = (fish.vx / speed) * fish.maxSpeed;
                fish.vy = (fish.vy / speed) * fish.maxSpeed;
            }
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Draw water gradient background
        drawBackground(g2d);
        
        // Draw light rays
        for (LightRay ray : lightRays) {
            ray.draw(g2d);
        }
        
        // Draw seaweed/plants
        drawPlants(g2d);
        
        // Draw decorations
        for (Decoration dec : decorations) {
            dec.draw(g2d);
        }
        
        // Draw bubbles (behind fish)
        for (Bubble bubble : bubbles) {
            bubble.draw(g2d);
        }
        
        // Draw fish
        for (Fish fish : fishList) {
            fish.draw(g2d);
        }
        
        // Draw sand at bottom
        drawSand(g2d);
    }
    
    private void drawBackground(Graphics2D g2d) {
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(20, 100, 160),
            0, HEIGHT, new Color(0, 30, 80)
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
    }
    
    private void drawPlants(Graphics2D g2d) {
        g2d.setColor(new Color(20, 100, 50, 180));
        for (int i = 0; i < 10; i++) {
            int x = i * 200 + 50;
            drawSeaweed(g2d, x, HEIGHT - 50);
        }
    }
    
    private void drawSeaweed(Graphics2D g2d, int x, int baseY) {
        Path2D path = new Path2D.Double();
        path.moveTo(x, baseY);
        
        int segments = 6;
        int segmentHeight = 60;
        double sway = Math.sin(System.currentTimeMillis() / 1000.0 + x) * 15;
        
        for (int i = 1; i <= segments; i++) {
            double xOffset = Math.sin(i * 0.5 + System.currentTimeMillis() / 1000.0) * sway;
            path.lineTo(x + xOffset, baseY - i * segmentHeight);
        }
        
        g2d.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(path);
    }
    
    private void drawSand(Graphics2D g2d) {
        GradientPaint sand = new GradientPaint(
            0, HEIGHT - 80, new Color(220, 200, 150),
            0, HEIGHT, new Color(180, 160, 120)
        );
        g2d.setPaint(sand);
        g2d.fillRect(0, HEIGHT - 80, WIDTH, 80);
        
        // Add some texture to sand
        g2d.setColor(new Color(200, 180, 130, 100));
        for (int i = 0; i < 50; i++) {
            int x = random.nextInt(WIDTH);
            int y = HEIGHT - 80 + random.nextInt(80);
            g2d.fillOval(x, y, 3, 2);
        }
    }
    
    // Fish behavior enum
    enum FishBehavior {
        SCHOOLING,
        WANDERING,
        FAST_SWIMMER
    }
    
    // Light Ray class
    class LightRay {
        double x;
        double sway;
        double swaySpeed;
        int width;
        
        LightRay(int startX) {
            this.x = startX;
            this.sway = random.nextDouble() * Math.PI * 2;
            this.swaySpeed = 0.3 + random.nextDouble() * 0.4;
            this.width = 80 + random.nextInt(40);
        }
        
        void update(double dt) {
            sway += dt * swaySpeed;
        }
        
        void draw(Graphics2D g2d) {
            double swayOffset = Math.sin(sway) * 50;
            
            Path2D ray = new Path2D.Double();
            ray.moveTo(x + swayOffset, 0);
            ray.lineTo(x + swayOffset - width / 2, HEIGHT);
            ray.lineTo(x + swayOffset + width / 2, HEIGHT);
            ray.closePath();
            
            GradientPaint gradient = new GradientPaint(
                (float)x, 0, new Color(255, 255, 200, 40),
                (float)x, HEIGHT, new Color(255, 255, 200, 0)
            );
            g2d.setPaint(gradient);
            g2d.fill(ray);
        }
    }
    
    // Decoration interface
    abstract class Decoration {
        double x, y;
        abstract void draw(Graphics2D g2d);
    }
    
    // Rock class
    class Rock extends Decoration {
        int width, height;
        Color color;
        
        Rock(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.color = new Color(100 + random.nextInt(40), 100 + random.nextInt(40), 100 + random.nextInt(40));
        }
        
        void draw(Graphics2D g2d) {
            // Draw irregular rock shape
            Ellipse2D rock = new Ellipse2D.Double(x, y, width, height);
            
            g2d.setColor(color.darker());
            g2d.fill(rock);
            
            g2d.setColor(color);
            g2d.fill(new Ellipse2D.Double(x + 5, y - 5, width - 10, height - 5));
            
            // Highlight
            g2d.setColor(new Color(255, 255, 255, 50));
            g2d.fillOval((int)x + 10, (int)y + 5, width / 3, height / 4);
        }
    }
    
    // Coral class
    class Coral extends Decoration {
        int size;
        Color color;
        
        Coral(int x, int y, int size, Color color) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.color = color;
        }
        
        void draw(Graphics2D g2d) {
            // Draw branching coral
            drawCoralBranch(g2d, (int)x, (int)y, size, 0, 3);
        }
        
        void drawCoralBranch(Graphics2D g2d, int x, int y, int length, int depth, int maxDepth) {
            if (depth >= maxDepth || length < 10) return;
            
            g2d.setColor(new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                150 - depth * 30
            ));
            
            g2d.setStroke(new BasicStroke(8 - depth * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            int endX = x + (int)(Math.sin(depth * 0.5) * length);
            int endY = y - length;
            
            g2d.drawLine(x, y, endX, endY);
            
            // Draw branches
            if (depth < maxDepth - 1) {
                drawCoralBranch(g2d, endX, endY, length * 2 / 3, depth + 1, maxDepth);
                drawCoralBranch(g2d, endX, endY, length * 2 / 3, depth + 1, maxDepth);
            }
        }
    }
    
    // Treasure Chest class
    class TreasureChest extends Decoration {
        double glowPhase;
        
        TreasureChest(int x, int y) {
            this.x = x;
            this.y = y;
            this.glowPhase = 0;
        }
        
        void draw(Graphics2D g2d) {
            glowPhase += 0.02;
            
            // Chest base
            g2d.setColor(new Color(139, 90, 43));
            g2d.fillRect((int)x, (int)y + 30, 100, 50);
            
            // Chest lid
            g2d.setColor(new Color(160, 110, 60));
            g2d.fillRoundRect((int)x, (int)y, 100, 40, 20, 20);
            
            // Metal bands
            g2d.setColor(new Color(192, 192, 192));
            g2d.setStroke(new BasicStroke(4));
            g2d.drawRect((int)x + 5, (int)y + 35, 90, 40);
            g2d.drawLine((int)x + 5, (int)y + 55, (int)x + 95, (int)y + 55);
            
            // Lock
            g2d.fillRect((int)x + 45, (int)y + 50, 10, 15);
            g2d.fillOval((int)x + 40, (int)y + 45, 20, 15);
            
            // Glowing treasure inside
            int glowAlpha = (int)(100 + Math.sin(glowPhase) * 50);
            g2d.setColor(new Color(255, 215, 0, glowAlpha));
            for (int i = 0; i < 3; i++) {
                g2d.fillOval((int)x + 25 + i * 20, (int)y + 15, 12, 12);
            }
            
            // Glow effect
            RadialGradientPaint glow = new RadialGradientPaint(
                new Point2D.Double(x + 50, y + 20),
                40,
                new float[]{0.0f, 1.0f},
                new Color[]{
                    new Color(255, 215, 0, glowAlpha / 2),
                    new Color(255, 215, 0, 0)
                }
            );
            g2d.setPaint(glow);
            g2d.fillOval((int)x + 10, (int)y - 20, 80, 60);
        }
    }
    
    // Fish class with enhanced behaviors
    class Fish {
        double x, y;
        double vx, vy;
        double speed;
        double maxSpeed;
        int size;
        Color color;
        boolean facingRight;
        double wobble;
        FishBehavior behavior;
        int schoolId;
        double behaviorTimer;
        
        Fish(int startX, int startY, FishBehavior behavior) {
            this.x = startX;
            this.y = startY;
            this.behavior = behavior;
            this.schoolId = -1;
            this.behaviorTimer = 0;
            
            // Different speeds based on behavior
            switch (behavior) {
                case FAST_SWIMMER:
                    this.speed = 80 + random.nextDouble() * 60;
                    this.maxSpeed = 140;
                    this.size = 25 + random.nextInt(20);
                    break;
                case WANDERING:
                    this.speed = 20 + random.nextDouble() * 30;
                    this.maxSpeed = 50;
                    this.size = 35 + random.nextInt(35);
                    break;
                case SCHOOLING:
                default:
                    this.speed = 40 + random.nextDouble() * 30;
                    this.maxSpeed = 70;
                    this.size = 28 + random.nextInt(15);
                    break;
            }
            
            this.facingRight = random.nextBoolean();
            this.vx = facingRight ? speed : -speed;
            this.vy = (random.nextDouble() - 0.5) * speed * 0.3;
            
            // Different colors for different behaviors
            Color[] schoolColors = {
                new Color(255, 140, 0), new Color(255, 215, 0),
                new Color(65, 105, 225), new Color(255, 20, 147)
            };
            Color[] wandererColors = {
                new Color(138, 43, 226), new Color(0, 255, 127),
                new Color(255, 105, 180)
            };
            Color[] fastColors = {
                new Color(0, 191, 255), new Color(30, 144, 255),
                new Color(135, 206, 250)
            };
            
            if (behavior == FishBehavior.FAST_SWIMMER) {
                this.color = fastColors[random.nextInt(fastColors.length)];
            } else if (behavior == FishBehavior.WANDERING) {
                this.color = wandererColors[random.nextInt(wandererColors.length)];
            } else {
                this.color = schoolColors[random.nextInt(schoolColors.length)];
            }
        }
        
        void update(double dt) {
            behaviorTimer += dt;
            
            // Behavior-specific updates
            if (behavior == FishBehavior.WANDERING) {
                // Random direction changes
                if (behaviorTimer > 2 && random.nextDouble() < 0.02) {
                    vx += (random.nextDouble() - 0.5) * 50;
                    vy += (random.nextDouble() - 0.5) * 50;
                    behaviorTimer = 0;
                }
            } else if (behavior == FishBehavior.FAST_SWIMMER) {
                // Occasional bursts of speed
                if (behaviorTimer > 3 && random.nextDouble() < 0.01) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    vx = Math.cos(angle) * maxSpeed;
                    vy = Math.sin(angle) * maxSpeed;
                    behaviorTimer = 0;
                }
            }
            
            // Update position
            x += vx * dt;
            y += vy * dt;
            
            // Wobble effect
            wobble += dt * 8;
            
            // Boundary wrapping
            if (x < -50) x = WIDTH + 50;
            else if (x > WIDTH + 50) x = -50;
            
            if (y < 100) {
                vy = Math.abs(vy);
            } else if (y > HEIGHT - 150) {
                vy = -Math.abs(vy);
            }
            
            // Limit velocity
            double currentSpeed = Math.sqrt(vx * vx + vy * vy);
            if (currentSpeed > maxSpeed) {
                vx = (vx / currentSpeed) * maxSpeed;
                vy = (vy / currentSpeed) * maxSpeed;
            }
            
            // Update facing direction
            facingRight = vx > 0;
        }
        
        void draw(Graphics2D g2d) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw shadow
            g2d.setColor(new Color(0, 0, 0, 30));
            g2d.fill(new Ellipse2D.Double(x - size * 0.6 + 5, y - size * 0.3 + 8, size * 1.2, size * 0.6));
            
            // Fish body
            Ellipse2D body = new Ellipse2D.Double(
                x - size * 0.6, 
                y - size * 0.3 + Math.sin(wobble) * 3, 
                size * 1.2, 
                size * 0.6
            );
            
            // Tail
            int tailX = facingRight ? (int)(x - size * 0.6) : (int)(x + size * 0.6);
            int tailY = (int)y;
            Polygon tail = new Polygon();
            tail.addPoint(tailX, tailY);
            tail.addPoint(tailX + (facingRight ? -20 : 20), tailY - 15 + (int)(Math.sin(wobble) * 5));
            tail.addPoint(tailX + (facingRight ? -20 : 20), tailY + 15 + (int)(Math.sin(wobble) * 5));
            
            // Draw fish
            g2d.setColor(color);
            g2d.fill(body);
            g2d.fill(tail);
            
            // Outline
            g2d.setColor(color.darker());
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(body);
            g2d.draw(tail);
            
            // Eye
            g2d.setColor(Color.WHITE);
            int eyeX = facingRight ? (int)(x + size * 0.3) : (int)(x - size * 0.3);
            g2d.fillOval(eyeX - 4, (int)y - 6, 8, 8);
            g2d.setColor(Color.BLACK);
            g2d.fillOval(eyeX - 2, (int)y - 4, 4, 4);
        }
    }
    
    // Bubble class
    class Bubble {
        double x, y;
        double vy;
        double vx;
        int size;
        double wobble;
        
        Bubble(int startX, int startY) {
            this.x = startX;
            this.y = startY;
            this.size = 5 + random.nextInt(10);
            this.vy = -20 - random.nextDouble() * 30;
            this.vx = (random.nextDouble() - 0.5) * 10;
            this.wobble = random.nextDouble() * Math.PI * 2;
        }
        
        void update(double dt) {
            y += vy * dt;
            wobble += dt * 3;
            x += Math.sin(wobble) * 0.5 + vx * dt;
        }
        
        void draw(Graphics2D g2d) {
            RadialGradientPaint gradient = new RadialGradientPaint(
                new Point2D.Double(x - size * 0.3, y - size * 0.3),
                size * 0.8f,
                new float[]{0.0f, 0.7f, 1.0f},
                new Color[]{
                    new Color(255, 255, 255, 100),
                    new Color(200, 230, 255, 60),
                    new Color(100, 150, 200, 20)
                }
            );
            
            g2d.setPaint(gradient);
            g2d.fillOval((int)(x - size/2), (int)(y - size/2), size, size);
            
            g2d.setColor(new Color(255, 255, 255, 150));
            g2d.fillOval((int)(x - size * 0.3), (int)(y - size * 0.3), size/3, size/3);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("HD Aquarium Screensaver");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new AquariumScreensaver());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
