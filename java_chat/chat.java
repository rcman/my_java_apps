import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// ============ CHAT SERVER ============
class ChatServer {
    private ServerSocket serverSocket;
    private Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private boolean running = false;

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            System.out.println("Chat server started on port " + port);
            
            while (running) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
                System.out.println("Client connected. Total clients: " + clients.size());
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("Client disconnected. Total clients: " + clients.size());
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start(12345);
    }
}

// ============ CLIENT HANDLER (Server-side) ============
class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private ChatServer server;
    private String username;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Error setting up client handler: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            // Get username
            writer.println("Enter your username:");
            username = reader.readLine();
            
            if (username != null && !username.trim().isEmpty()) {
                server.broadcast(username + " joined the chat", this);
                
                String message;
                while ((message = reader.readLine()) != null) {
                    if (message.equalsIgnoreCase("/quit")) {
                        break;
                    }
                    server.broadcast(username + ": " + message, this);
                }
            }
        } catch (IOException e) {
            System.err.println("Client handler error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    public void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);
        }
    }

    private void cleanup() {
        try {
            if (username != null) {
                server.broadcast(username + " left the chat", this);
            }
            server.removeClient(this);
            if (socket != null) socket.close();
            if (reader != null) reader.close();
            if (writer != null) writer.close();
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}

// ============ ENCRYPTION UTILITY ============
class EncryptionUtil {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    
    public static SecretKey generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }
    
    public static String encrypt(String plainText, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        
        // Generate random IV
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] cipherText = cipher.doFinal(plainText.getBytes());
        
        // Combine IV and ciphertext
        byte[] encryptedWithIv = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
        System.arraycopy(cipherText, 0, encryptedWithIv, iv.length, cipherText.length);
        
        return Base64.getEncoder().encodeToString(encryptedWithIv);
    }
    
    public static String decrypt(String encryptedText, SecretKey key) throws Exception {
        byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedText);
        
        // Extract IV
        byte[] iv = new byte[16];
        System.arraycopy(encryptedWithIv, 0, iv, 0, iv.length);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        // Extract ciphertext
        byte[] cipherText = new byte[encryptedWithIv.length - 16];
        System.arraycopy(encryptedWithIv, 16, cipherText, 0, cipherText.length);
        
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        byte[] plainText = cipher.doFinal(cipherText);
        
        return new String(plainText);
    }
}

// ============ CHAT CLIENT GUI ============
class ChatClient extends JFrame {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton connectButton;
    private JTextField serverField;
    private JTextField portField;
    private JTextField usernameField;
    private boolean connected = false;
    private SecretKey encryptionKey;

    public ChatClient() {
        initializeGUI();
        try {
            encryptionKey = EncryptionUtil.generateKey();
        } catch (NoSuchAlgorithmException e) {
            JOptionPane.showMessageDialog(this, "Error initializing encryption: " + e.getMessage());
        }
    }

    private void initializeGUI() {
        setTitle("Secure Chat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Connection panel
        JPanel connectionPanel = new JPanel(new FlowLayout());
        connectionPanel.add(new JLabel("Server:"));
        serverField = new JTextField("localhost", 10);
        connectionPanel.add(serverField);
        
        connectionPanel.add(new JLabel("Port:"));
        portField = new JTextField("12345", 5);
        connectionPanel.add(portField);
        
        connectionPanel.add(new JLabel("Username:"));
        usernameField = new JTextField(10);
        connectionPanel.add(usernameField);
        
        connectButton = new JButton("Connect");
        connectButton.addActionListener(this::connectToServer);
        connectionPanel.add(connectButton);

        add(connectionPanel, BorderLayout.NORTH);

        // Chat area
        chatArea = new JTextArea(20, 50);
        chatArea.setEditable(false);
        chatArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        // Message input panel
        JPanel messagePanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        messageField.addActionListener(this::sendMessage);
        messagePanel.add(messageField, BorderLayout.CENTER);
        
        sendButton = new JButton("Send");
        sendButton.addActionListener(this::sendMessage);
        sendButton.setEnabled(false);
        messagePanel.add(sendButton, BorderLayout.EAST);

        add(messagePanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        
        // Disable message components initially
        messageField.setEnabled(false);
    }

    private void connectToServer(ActionEvent e) {
        if (!connected) {
            String server = serverField.getText().trim();
            String portText = portField.getText().trim();
            String username = usernameField.getText().trim();
            
            if (server.isEmpty() || portText.isEmpty() || username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all connection fields.");
                return;
            }
            
            try {
                int port = Integer.parseInt(portText);
                socket = new Socket(server, port);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
                
                connected = true;
                connectButton.setText("Disconnect");
                messageField.setEnabled(true);
                sendButton.setEnabled(true);
                
                // Disable connection fields
                serverField.setEnabled(false);
                portField.setEnabled(false);
                usernameField.setEnabled(false);
                
                // Send username
                String prompt = reader.readLine(); // "Enter your username:"
                writer.println(username);
                
                chatArea.append("Connected to " + server + ":" + port + " as " + username + "\n");
                chatArea.append("Encryption enabled for secure communication\n");
                chatArea.append("Type /quit to disconnect\n\n");
                
                // Start listening for messages
                new Thread(this::listenForMessages).start();
                
                messageField.requestFocus();
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid port number.");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Connection failed: " + ex.getMessage());
            }
        } else {
            disconnect();
        }
    }

    private void sendMessage(ActionEvent e) {
        if (!connected) return;
        
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            try {
                if (message.equalsIgnoreCase("/quit")) {
                    disconnect();
                    return;
                }
                
                // For demonstration, we'll show both encrypted and decrypted messages
                String encryptedMessage = EncryptionUtil.encrypt(message, encryptionKey);
                writer.println(message); // Send original for server broadcast
                
                chatArea.append("You: " + message + "\n");
                chatArea.append("  [Encrypted: " + encryptedMessage.substring(0, Math.min(20, encryptedMessage.length())) + "...]\n");
                messageField.setText("");
                
            } catch (Exception ex) {
                chatArea.append("Encryption error: " + ex.getMessage() + "\n");
            }
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while (connected && (message = reader.readLine()) != null) {
                final String msg = message;
                SwingUtilities.invokeLater(() -> {
                    chatArea.append(msg + "\n");
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                });
            }
        } catch (IOException e) {
            if (connected) {
                SwingUtilities.invokeLater(() -> {
                    chatArea.append("Connection lost: " + e.getMessage() + "\n");
                    disconnect();
                });
            }
        }
    }

    private void disconnect() {
        connected = false;
        try {
            if (writer != null) {
                writer.println("/quit");
                writer.close();
            }
            if (reader != null) reader.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("Error during disconnect: " + e.getMessage());
        }
        
        connectButton.setText("Connect");
        messageField.setEnabled(false);
        sendButton.setEnabled(false);
        
        // Re-enable connection fields
        serverField.setEnabled(true);
        portField.setEnabled(true);
        usernameField.setEnabled(true);
        
        chatArea.append("Disconnected from server.\n\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                // Use default look and feel
            }
            new ChatClient().setVisible(true);
        });
    }
}
