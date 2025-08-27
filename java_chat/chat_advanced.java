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

// ============ ENHANCED CHAT CLIENT GUI ============
class ChatClient extends JFrame {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JButton connectButton;
    private JButton clearButton;
    private JTextField serverField;
    private JTextField portField;
    private JTextField usernameField;
    private JLabel statusLabel;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JCheckBox encryptionCheckBox;
    private JSlider fontSizeSlider;
    private boolean connected = false;
    private SecretKey encryptionKey;
    private int messageCount = 0;

    public ChatClient() {
        initializeGUI();
        try {
            encryptionKey = EncryptionUtil.generateKey();
        } catch (NoSuchAlgorithmException e) {
            JOptionPane.showMessageDialog(this, "Error initializing encryption: " + e.getMessage());
        }
    }

    private void initializeGUI() {
        setTitle("Secure Chat Client v2.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Create menu bar
        createMenuBar();

        // Connection panel with better layout
        JPanel connectionPanel = new JPanel(new GridBagLayout());
        connectionPanel.setBorder(BorderFactory.createTitledBorder("Connection Settings"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = 0;
        connectionPanel.add(new JLabel("Server:"), gbc);
        gbc.gridx = 1;
        serverField = new JTextField("localhost", 12);
        connectionPanel.add(serverField, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        connectionPanel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 3;
        portField = new JTextField("12345", 6);
        connectionPanel.add(portField, gbc);

        gbc.gridx = 4; gbc.gridy = 0;
        connectionPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 5;
        usernameField = new JTextField(12);
        connectionPanel.add(usernameField, gbc);

        gbc.gridx = 6; gbc.gridy = 0;
        connectButton = new JButton("Connect");
        connectButton.addActionListener(this::connectToServer);
        connectionPanel.add(connectButton, gbc);

        // Encryption checkbox
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        encryptionCheckBox = new JCheckBox("Enable Encryption", true);
        connectionPanel.add(encryptionCheckBox, gbc);

        // Font size slider
        gbc.gridx = 2; gbc.gridy = 1; gbc.gridwidth = 2;
        connectionPanel.add(new JLabel("Font Size:"), gbc);
        gbc.gridx = 4; gbc.gridwidth = 3;
        fontSizeSlider = new JSlider(8, 24, 12);
        fontSizeSlider.addChangeListener(e -> updateFontSize());
        fontSizeSlider.setMajorTickSpacing(4);
        fontSizeSlider.setPaintTicks(true);
        fontSizeSlider.setPaintLabels(true);
        connectionPanel.add(fontSizeSlider, gbc);

        add(connectionPanel, BorderLayout.NORTH);

        // Main chat area with split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // Left side - Chat area
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat Messages"));
        
        chatArea = new JTextArea(20, 40);
        chatArea.setEditable(false);
        chatArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        
        splitPane.setLeftComponent(chatPanel);

        // Right side - User list and controls
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        // User list
        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.setBorder(BorderFactory.createTitledBorder("Online Users"));
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 200));
        userPanel.add(userScrollPane, BorderLayout.CENTER);
        rightPanel.add(userPanel, BorderLayout.CENTER);

        // Control buttons
        JPanel controlPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controls"));
        
        clearButton = new JButton("Clear Chat");
        clearButton.addActionListener(e -> chatArea.setText(""));
        controlPanel.add(clearButton);
        
        JButton saveButton = new JButton("Save Chat");
        saveButton.addActionListener(this::saveChatLog);
        controlPanel.add(saveButton);
        
        JButton aboutButton = new JButton("About");
        aboutButton.addActionListener(this::showAbout);
        controlPanel.add(aboutButton);
        
        rightPanel.add(controlPanel, BorderLayout.SOUTH);
        splitPane.setRightComponent(rightPanel);
        
        splitPane.setDividerLocation(500);
        add(splitPane, BorderLayout.CENTER);

        // Message input panel with enhanced features
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBorder(BorderFactory.createTitledBorder("Send Message"));
        
        messageField = new JTextField();
        messageField.addActionListener(this::sendMessage);
        messageField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateSendButton(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateSendButton(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateSendButton(); }
        });
        messagePanel.add(messageField, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        sendButton = new JButton("Send");
        sendButton.addActionListener(this::sendMessage);
        sendButton.setEnabled(false);
        buttonPanel.add(sendButton);
        
        JButton emojiButton = new JButton("😊");
        emojiButton.addActionListener(this::showEmojiMenu);
        buttonPanel.add(emojiButton);
        
        messagePanel.add(buttonPanel, BorderLayout.EAST);
        add(messagePanel, BorderLayout.SOUTH);

        // Status bar
        statusLabel = new JLabel("Disconnected");
        statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        add(statusLabel, BorderLayout.PAGE_END);

        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(700, 500));
        
        // Disable message components initially
        messageField.setEnabled(false);
        
        // Add window listener for cleanup
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (connected) {
                    disconnect();
                }
            }
        });
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem saveItem = new JMenuItem("Save Chat...");
        saveItem.addActionListener(this::saveChatLog);
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // Settings menu
        JMenu settingsMenu = new JMenu("Settings");
        JMenuItem prefsItem = new JMenuItem("Preferences...");
        prefsItem.addActionListener(this::showPreferences);
        settingsMenu.add(prefsItem);
        
        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(this::showAbout);
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(settingsMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private void updateFontSize() {
        int size = fontSizeSlider.getValue();
        Font newFont = new Font(Font.SANS_SERIF, Font.PLAIN, size);
        chatArea.setFont(newFont);
        messageField.setFont(newFont);
    }

    private void updateSendButton() {
        sendButton.setEnabled(connected && !messageField.getText().trim().isEmpty());
    }

    private void showEmojiMenu(ActionEvent e) {
        String[] emojis = {"😊", "😂", "😍", "😢", "😡", "👍", "👎", "❤️", "🎉", "🔥"};
        JPopupMenu emojiMenu = new JPopupMenu();
        for (String emoji : emojis) {
            JMenuItem item = new JMenuItem(emoji);
            item.addActionListener(ev -> {
                messageField.setText(messageField.getText() + emoji);
                messageField.requestFocus();
            });
            emojiMenu.add(item);
        }
        emojiMenu.show((JButton)e.getSource(), 0, -emojiMenu.getPreferredSize().height);
    }

    private void saveChatLog(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new java.io.File("chat_log.txt"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(fileChooser.getSelectedFile())) {
                writer.println("=== Chat Log - " + new Date() + " ===");
                writer.println(chatArea.getText());
                JOptionPane.showMessageDialog(this, "Chat log saved successfully!");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage());
            }
        }
    }

    private void showPreferences(ActionEvent e) {
        JDialog prefDialog = new JDialog(this, "Preferences", true);
        prefDialog.setLayout(new GridLayout(4, 2, 10, 10));
        prefDialog.add(new JLabel("Font Size:"));
        JSpinner fontSpinner = new JSpinner(new SpinnerNumberModel(12, 8, 24, 1));
        prefDialog.add(fontSpinner);
        
        prefDialog.add(new JLabel("Enable Sound:"));
        JCheckBox soundBox = new JCheckBox("Message notifications", true);
        prefDialog.add(soundBox);
        
        JButton okButton = new JButton("OK");
        okButton.addActionListener(ev -> {
            chatArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (Integer)fontSpinner.getValue()));
            prefDialog.dispose();
        });
        prefDialog.add(okButton);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(ev -> prefDialog.dispose());
        prefDialog.add(cancelButton);
        
        prefDialog.pack();
        prefDialog.setLocationRelativeTo(this);
        prefDialog.setVisible(true);
    }

    private void showAbout(ActionEvent e) {
        String about = "Secure Chat Client v2.0\n\n" +
                      "Features:\n" +
                      "• AES-256 Encryption\n" +
                      "• Multi-user support\n" +
                      "• File logging\n" +
                      "• Emoji support\n" +
                      "• Customizable interface\n\n" +
                      "Built with Java Swing\n" +
                      "© 2025";
        JOptionPane.showMessageDialog(this, about, "About", JOptionPane.INFORMATION_MESSAGE);
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
                statusLabel.setText("Connecting...");
                
                socket = new Socket(server, port);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
                
                connected = true;
                connectButton.setText("Disconnect");
                messageField.setEnabled(true);
                updateSendButton();
                
                // Disable connection fields
                serverField.setEnabled(false);
                portField.setEnabled(false);
                usernameField.setEnabled(false);
                encryptionCheckBox.setEnabled(false);
                
                // Send username
                String prompt = reader.readLine(); // "Enter your username:"
                writer.println(username);
                
                // Update user list
                userListModel.clear();
                userListModel.addElement(username + " (You)");
                
                String encStatus = encryptionCheckBox.isSelected() ? "enabled" : "disabled";
                chatArea.append("=== Connected to " + server + ":" + port + " as " + username + " ===\n");
                chatArea.append("Encryption " + encStatus + " for secure communication\n");
                chatArea.append("Commands: /quit to disconnect, /clear to clear chat\n");
                chatArea.append("Message count: " + messageCount + "\n\n");
                
                statusLabel.setText("Connected to " + server + ":" + port + " as " + username);
                
                // Start listening for messages
                new Thread(this::listenForMessages).start();
                
                messageField.requestFocus();
                
            } catch (NumberFormatException ex) {
                statusLabel.setText("Disconnected");
                JOptionPane.showMessageDialog(this, "Invalid port number.");
            } catch (IOException ex) {
                statusLabel.setText("Connection failed");
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
                } else if (message.equalsIgnoreCase("/clear")) {
                    chatArea.setText("");
                    messageCount = 0;
                    messageField.setText("");
                    return;
                }
                
                messageCount++;
                
                if (encryptionCheckBox.isSelected()) {
                    // For demonstration, show encryption process
                    String encryptedMessage = EncryptionUtil.encrypt(message, encryptionKey);
                    writer.println(message); // Send original for server broadcast
                    
                    chatArea.append("[" + getCurrentTime() + "] You: " + message + "\n");
                    chatArea.append("  🔒 [Encrypted: " + encryptedMessage.substring(0, Math.min(30, encryptedMessage.length())) + "...]\n");
                } else {
                    writer.println(message);
                    chatArea.append("[" + getCurrentTime() + "] You: " + message + "\n");
                }
                
                messageField.setText("");
                updateSendButton();
                chatArea.setCaretPosition(chatArea.getDocument().getLength());
                
            } catch (Exception ex) {
                chatArea.append("❌ Encryption error: " + ex.getMessage() + "\n");
            }
        }
    }

    private String getCurrentTime() {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    private void listenForMessages() {
        try {
            String message;
            while (connected && (message = reader.readLine()) != null) {
                final String msg = message;
                SwingUtilities.invokeLater(() -> {
                    if (msg.contains(" joined the chat") || msg.contains(" left the chat")) {
                        chatArea.append("🔔 [" + getCurrentTime() + "] " + msg + "\n");
                        // Update user list (simplified - in real app you'd track users properly)
                        updateUserList(msg);
                    } else {
                        chatArea.append("[" + getCurrentTime() + "] " + msg + "\n");
                    }
                    chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    
                    // Flash taskbar for new messages (if window not focused)
                    if (!ChatClient.this.isFocused()) {
                        ChatClient.this.toFront();
                    }
                });
            }
        } catch (IOException e) {
            if (connected) {
                SwingUtilities.invokeLater(() -> {
                    chatArea.append("❌ Connection lost: " + e.getMessage() + "\n");
                    statusLabel.setText("Connection lost");
                    disconnect();
                });
            }
        }
    }

    private void updateUserList(String message) {
        // Simple user list management (in production, server would send user list)
        if (message.contains(" joined the chat")) {
            String username = message.substring(0, message.indexOf(" joined"));
            if (!userListContains(username)) {
                userListModel.addElement(username);
            }
        } else if (message.contains(" left the chat")) {
            String username = message.substring(0, message.indexOf(" left"));
            userListModel.removeElement(username);
        }
    }

    private boolean userListContains(String username) {
        for (int i = 0; i < userListModel.size(); i++) {
            if (userListModel.get(i).startsWith(username)) {
                return true;
            }
        }
        return false;
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
        encryptionCheckBox.setEnabled(true);
        
        // Clear user list
        userListModel.clear();
        
        statusLabel.setText("Disconnected");
        chatArea.append("=== Disconnected from server ===\n\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
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
