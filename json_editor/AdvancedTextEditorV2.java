import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.ExecutionException;

public class AdvancedTextEditor extends JFrame {
    // Constants
    private static final String APP_TITLE = "Advanced Java Text Editor";
    private static final int DEFAULT_FONT_SIZE = 14;
    private static final int MAX_RECENT_FILES = 10;
    private static final int MAX_UNDO_STACK = 100;
    
    // Main components
    private JTabbedPane tabbedPane;
    private JLabel statusLabel;
    private JMenuBar menuBar;
    private JToolBar toolBar;
    
    // Dialogs
    private FindReplaceDialog findReplaceDialog;
    private GoToLineDialog goToLineDialog;
    private StyleConfigDialog styleConfigDialog;
    private FileExplorerDialog fileExplorerDialog;
    private TerminalDialog terminalDialog;
    
    // Settings
    private boolean showLineNumbers = true;
    private boolean wordWrap = false;
    private boolean autoIndent = true;
    private boolean autoSave = false;
    private int tabSize = 4;
    private int fontSize = DEFAULT_FONT_SIZE;
    private String currentTheme = "Dark";
    
    // File management
    private List<String> recentFiles;
    private File currentDirectory;
    
    // Color scheme
    private Color backgroundColor = new Color(30, 30, 30);
    private Color textColor = new Color(220, 220, 220);
    private Color selectionColor = new Color(70, 130, 180);
    private Color lineNumberColor = new Color(128, 128, 128);
    
    // Split view
    private boolean splitView = false;
    private JSplitPane splitPane;
    
    public AdvancedTextEditor() {
        super(APP_TITLE);
        initializeComponents();
        setupMenuBar();
        setupToolBar();
        setupKeyBindings();
        setupUI();
        
        recentFiles = new ArrayList<>();
        
        // Create initial tab
        newFile();
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        applyTheme(currentTheme);
    }
    
    private void initializeComponents() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(new EmptyBorder(5, 10, 5, 10));
        
        // Setup tab close functionality
        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    int tabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());
                    if (tabIndex >= 0) {
                        closeTab(tabIndex);
                    }
                }
            }
        });
    }
    
    private void setupMenuBar() {
        menuBar = new JMenuBar();
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(createMenuItem("New Tab", "ctrl T", e -> newFile()));
        fileMenu.add(createMenuItem("Open", "ctrl O", e -> openFile()));
        fileMenu.addSeparator();
        
        JMenu recentMenu = new JMenu("Recent Files");
        updateRecentFilesMenu(recentMenu);
        fileMenu.add(recentMenu);
        fileMenu.addSeparator();
        
        fileMenu.add(createMenuItem("Save", "ctrl S", e -> saveFile()));
        fileMenu.add(createMenuItem("Save As", "ctrl shift S", e -> saveFileAs()));
        fileMenu.addSeparator();
        fileMenu.add(createMenuItem("Exit", "ctrl Q", e -> exitApplication()));
        
        // Edit menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.add(createMenuItem("Undo", "ctrl Z", e -> undo()));
        editMenu.add(createMenuItem("Redo", "ctrl Y", e -> redo()));
        editMenu.addSeparator();
        editMenu.add(createMenuItem("Cut", "ctrl X", e -> cut()));
        editMenu.add(createMenuItem("Copy", "ctrl C", e -> copy()));
        editMenu.add(createMenuItem("Paste", "ctrl V", e -> paste()));
        editMenu.addSeparator();
        editMenu.add(createMenuItem("Find & Replace", "ctrl F", e -> showFindReplace()));
        editMenu.add(createMenuItem("Go to Line", "ctrl G", e -> showGoToLine()));
        editMenu.addSeparator();
        editMenu.add(createMenuItem("Duplicate Line", "ctrl D", e -> duplicateLine()));
        editMenu.add(createMenuItem("Comment/Uncomment", "ctrl slash", e -> commentUncommentLines()));
        editMenu.addSeparator();
        editMenu.add(createMenuItem("Select All", "ctrl A", e -> selectAll()));
        
        // View menu
        JMenu viewMenu = new JMenu("View");
        viewMenu.add(createMenuItem("Style Configuration", "", e -> showStyleConfig()));
        viewMenu.add(createMenuItem("File Explorer", "", e -> showFileExplorer()));
        viewMenu.add(createMenuItem("Terminal", "", e -> showTerminal()));
        viewMenu.addSeparator();
        
        JCheckBoxMenuItem lineNumbersItem = new JCheckBoxMenuItem("Show Line Numbers", showLineNumbers);
        lineNumbersItem.addActionListener(e -> {
            showLineNumbers = lineNumbersItem.isSelected();
            refreshCurrentTab();
        });
        viewMenu.add(lineNumbersItem);
        
        JCheckBoxMenuItem wordWrapItem = new JCheckBoxMenuItem("Word Wrap", wordWrap);
        wordWrapItem.addActionListener(e -> {
            wordWrap = wordWrapItem.isSelected();
            refreshCurrentTab();
        });
        viewMenu.add(wordWrapItem);
        
        viewMenu.addSeparator();
        viewMenu.add(createMenuItem("Split View Horizontal", "", e -> toggleSplitView(true)));
        viewMenu.add(createMenuItem("Split View Vertical", "", e -> toggleSplitView(false)));
        
        // Settings menu
        JMenu settingsMenu = new JMenu("Settings");
        
        JCheckBoxMenuItem autoIndentItem = new JCheckBoxMenuItem("Auto Indent", autoIndent);
        autoIndentItem.addActionListener(e -> autoIndent = autoIndentItem.isSelected());
        settingsMenu.add(autoIndentItem);
        
        JCheckBoxMenuItem autoSaveItem = new JCheckBoxMenuItem("Auto Save", autoSave);
        autoSaveItem.addActionListener(e -> autoSave = autoSaveItem.isSelected());
        settingsMenu.add(autoSaveItem);
        
        settingsMenu.addSeparator();
        
        JMenu themeMenu = new JMenu("Themes");
        String[] themes = {"Dark", "Light", "Monokai", "Solarized Dark"};
        ButtonGroup themeGroup = new ButtonGroup();
        for (String theme : themes) {
            JRadioButtonMenuItem themeItem = new JRadioButtonMenuItem(theme, theme.equals(currentTheme));
            themeItem.addActionListener(e -> applyTheme(theme));
            themeGroup.add(themeItem);
            themeMenu.add(themeItem);
        }
        settingsMenu.add(themeMenu);
        
        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(createMenuItem("About", "", e -> showAbout()));
        
        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(settingsMenu);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void updateRecentFilesMenu(JMenu recentMenu) {
        recentMenu.removeAll();
        for (String filePath : recentFiles) {
            JMenuItem item = new JMenuItem(new File(filePath).getName());
            item.addActionListener(e -> openFileInBackground(new File(filePath)));
            recentMenu.add(item);
        }
        if (recentFiles.isEmpty()) {
            JMenuItem emptyItem = new JMenuItem("No recent files");
            emptyItem.setEnabled(false);
            recentMenu.add(emptyItem);
        }
    }
    
    private void setupToolBar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        toolBar.add(createToolButton("New", e -> newFile()));
        toolBar.add(createToolButton("Open", e -> openFile()));
        toolBar.add(createToolButton("Save", e -> saveFile()));
        toolBar.addSeparator();
        toolBar.add(createToolButton("Undo", e -> undo()));
        toolBar.add(createToolButton("Redo", e -> redo()));
        toolBar.addSeparator();
        toolBar.add(createToolButton("Cut", e -> cut()));
        toolBar.add(createToolButton("Copy", e -> copy()));
        toolBar.add(createToolButton("Paste", e -> paste()));
        toolBar.addSeparator();
        toolBar.add(createToolButton("Find", e -> showFindReplace()));
    }
    
    private void setupKeyBindings() {
        // Additional key bindings can be added here if needed
    }
    
    private void setupUI() {
        setLayout(new BorderLayout());
        add(toolBar, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }
    
    private JMenuItem createMenuItem(String text, String accelerator, ActionListener listener) {
        JMenuItem item = new JMenuItem(text);
        if (!accelerator.isEmpty()) {
            item.setAccelerator(KeyStroke.getKeyStroke(accelerator));
        }
        item.addActionListener(listener);
        return item;
    }
    
    private JButton createToolButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.addActionListener(listener);
        button.setFocusable(false);
        return button;
    }
    
    private void newFile() {
        EditorTab tab = new EditorTab(this);
        String tabTitle = "Untitled " + (tabbedPane.getTabCount() + 1);
        
        tabbedPane.addTab(tabTitle, tab);
        tabbedPane.setSelectedComponent(tab);
        
        // Add close button to tab
        int tabIndex = tabbedPane.indexOfComponent(tab);
        tabbedPane.setTabComponentAt(tabIndex, new TabComponent(tabTitle, () -> closeTab(tabIndex)));
        
        updateStatus();
    }
    
    private void openFile() {
        JFileChooser fileChooser = new JFileChooser(currentDirectory);
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            "Text files", "txt", "java", "py", "html", "css", "js", "json", "xml", "md"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            openFileInBackground(file);
        }
    }
    
    // Public method for external access (e.g., from FileExplorerDialog)
    public void openFileExternal(File file) {
        openFileInBackground(file);
    }
    
    private void openFileInBackground(File file) {
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return new String(Files.readAllBytes(file.toPath()));
            }
            
            @Override
            protected void done() {
                try {
                    String content = get();
                    EditorTab tab = new EditorTab(AdvancedTextEditor.this);
                    tab.setFile(file);
                    tab.setText(content);
                    tab.setModified(false);
                    
                    String tabTitle = file.getName();
                    tabbedPane.addTab(tabTitle, tab);
                    tabbedPane.setSelectedComponent(tab);
                    
                    int tabIndex = tabbedPane.indexOfComponent(tab);
                    tabbedPane.setTabComponentAt(tabIndex, new TabComponent(tabTitle, () -> closeTab(tabIndex)));
                    
                    addToRecentFiles(file.getAbsolutePath());
                    currentDirectory = file.getParentFile();
                    updateStatus();
                } catch (InterruptedException | ExecutionException e) {
                    showErrorDialog("Error opening file: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                }
            }
        };
        worker.execute();
    }
    
    private void saveFile() {
        EditorTab currentTab = getCurrentTab();
        if (currentTab != null) {
            if (currentTab.getFile() != null) {
                saveToFileInBackground(currentTab, currentTab.getFile());
            } else {
                saveFileAs();
            }
        }
    }
    
    private void saveFileAs() {
        EditorTab currentTab = getCurrentTab();
        if (currentTab != null) {
            JFileChooser fileChooser = new JFileChooser(currentDirectory);
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                saveToFileInBackground(currentTab, file);
                currentTab.setFile(file);
                
                // Update tab title
                int tabIndex = tabbedPane.getSelectedIndex();
                if (tabIndex >= 0) {
                    tabbedPane.setTitleAt(tabIndex, file.getName());
                    tabbedPane.setTabComponentAt(tabIndex, 
                        new TabComponent(file.getName(), () -> closeTab(tabIndex)));
                }
            }
        }
    }
    
    private void saveToFileInBackground(EditorTab tab, File file) {
        String content = tab.getText();
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Files.write(file.toPath(), content.getBytes());
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get();
                    tab.setModified(false);
                    addToRecentFiles(file.getAbsolutePath());
                    updateStatus();
                } catch (InterruptedException | ExecutionException e) {
                    showErrorDialog("Error saving file: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                }
            }
        };
        worker.execute();
    }
    
    private void closeTab(int index) {
        if (index >= 0 && index < tabbedPane.getTabCount()) {
            EditorTab tab = (EditorTab) tabbedPane.getComponentAt(index);
            if (tab.isModified()) {
                int option = JOptionPane.showConfirmDialog(this, 
                    "File has unsaved changes. Save before closing?", 
                    "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
                
                if (option == JOptionPane.YES_OPTION) {
                    saveFile();
                } else if (option == JOptionPane.CANCEL_OPTION) {
                    return;
                }
            }
            
            tabbedPane.removeTabAt(index);
            
            if (tabbedPane.getTabCount() == 0) {
                newFile();
            }
        }
    }
    
    private EditorTab getCurrentTab() {
        Component selected = tabbedPane.getSelectedComponent();
        return selected instanceof EditorTab ? (EditorTab) selected : null;
    }
    
    private void undo() {
        EditorTab currentTab = getCurrentTab();
        if (currentTab != null) {
            currentTab.undo();
        }
    }
    
    private void redo() {
        EditorTab currentTab = getCurrentTab();
        if (currentTab != null) {
            currentTab.redo();
        }
    }
    
    private void cut() {
        EditorTab currentTab = getCurrentTab();
        if (currentTab != null) {
            currentTab.getTextPane().cut();
        }
    }
    
    private void copy() {
        EditorTab currentTab = getCurrentTab();
        if (currentTab != null) {
            currentTab.getTextPane().copy();
        }
    }
    
    private void paste() {
        EditorTab currentTab = getCurrentTab();
        if (currentTab != null) {
            currentTab.getTextPane().paste();
        }
    }
    
    private void selectAll() {
        EditorTab currentTab = getCurrentTab();
        if (currentTab != null) {
            currentTab.getTextPane().selectAll();
        }
    }
    
    private void duplicateLine() {
        EditorTab currentTab = getCurrentTab();
        if (currentTab != null) {
            currentTab.duplicateLine();
        }
    }
    
    private void commentUncommentLines() {
        EditorTab currentTab = getCurrentTab();
        if (currentTab != null) {
            currentTab.commentUncommentLines();
        }
    }
    
    private void showFindReplace() {
        if (findReplaceDialog == null) {
            findReplaceDialog = new FindReplaceDialog(this);
        }
        findReplaceDialog.setVisible(true);
    }
    
    private void showGoToLine() {
        if (goToLineDialog == null) {
            goToLineDialog = new GoToLineDialog(this);
        }
        goToLineDialog.setVisible(true);
    }
    
    private void showStyleConfig() {
        if (styleConfigDialog == null) {
            styleConfigDialog = new StyleConfigDialog(this);
        }
        styleConfigDialog.setVisible(true);
    }
    
    private void showFileExplorer() {
        if (fileExplorerDialog == null) {
            fileExplorerDialog = new FileExplorerDialog(this);
        }
        fileExplorerDialog.setVisible(true);
    }
    
    private void showTerminal() {
        if (terminalDialog == null) {
            terminalDialog = new TerminalDialog(this);
        }
        terminalDialog.setVisible(true);
    }
    
    private void showAbout() {
        String message = "Advanced Java Text Editor\n\n" +
                        "A feature-rich text editor built with Java Swing\n" +
                        "Version 1.0.0\n\n" +
                        "Features:\n" +
                        "• Multiple tabs and split view\n" +
                        "• Syntax highlighting\n" +
                        "• Find & Replace with regex support\n" +
                        "• Undo/Redo functionality\n" +
                        "• File explorer and terminal\n" +
                        "• Customizable themes and styling\n" +
                        "• Auto-indent and bracket matching\n" +
                        "• Line numbers and minimap\n" +
                        "• Zoom and font customization\n" +
                        "• Recent files support\n\n" +
                        "Keyboard Shortcuts:\n" +
                        "Ctrl+N: New Tab, Ctrl+O: Open, Ctrl+S: Save\n" +
                        "Ctrl+Z: Undo, Ctrl+Y: Redo, Ctrl+F: Find\n" +
                        "Ctrl+G: Go to Line, Ctrl+D: Duplicate Line\n" +
                        "Ctrl+/: Comment/Uncomment";
        
        JOptionPane.showMessageDialog(this, message, "About", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void exitApplication() {
        // Check for unsaved changes in all tabs
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof EditorTab && ((EditorTab) comp).isModified()) {
                int option = JOptionPane.showConfirmDialog(this,
                    "There are unsaved changes. Exit anyway?",
                    "Unsaved Changes", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.NO_OPTION) {
                    return;
                }
                break;
            }
        }
        System.exit(0);
    }
    
    private void toggleSplitView(boolean horizontal) {
        splitView = !splitView;
        
        if (splitView && tabbedPane.getTabCount() > 1) {
            EditorTab currentTab = getCurrentTab();
            if (currentTab != null) {
                splitPane = new JSplitPane(
                    horizontal ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT);
                
                // Create a copy of the current tab for the split view
                EditorTab splitTab = new EditorTab(this);
                splitTab.setText(currentTab.getText());
                
                splitPane.setLeftComponent(new JScrollPane(currentTab.getTextPane()));
                splitPane.setRightComponent(new JScrollPane(splitTab.getTextPane()));
                splitPane.setDividerLocation(0.5);
                splitPane.setResizeWeight(0.5);
                
                remove(tabbedPane);
                add(splitPane, BorderLayout.CENTER);
                revalidate();
                repaint();
            }
        } else {
            if (splitPane != null) {
                remove(splitPane);
                add(tabbedPane, BorderLayout.CENTER);
                revalidate();
                repaint();
            }
        }
    }
    
    public void applyTheme(String theme) {
        currentTheme = theme;
        
        switch (theme) {
            case "Light":
                backgroundColor = new Color(240, 240, 240);
                textColor = new Color(40, 40, 40);
                selectionColor = new Color(173, 216, 230);
                lineNumberColor = new Color(128, 128, 128);
                break;
            case "Monokai":
                backgroundColor = new Color(39, 40, 34);
                textColor = new Color(248, 248, 242);
                selectionColor = new Color(73, 72, 62);
                lineNumberColor = new Color(144, 145, 129);
                break;
            case "Solarized Dark":
                backgroundColor = new Color(0, 43, 54);
                textColor = new Color(131, 148, 150);
                selectionColor = new Color(7, 54, 66);
                lineNumberColor = new Color(88, 110, 117);
                break;
            default: // Dark
                backgroundColor = new Color(30, 30, 30);
                textColor = new Color(220, 220, 220);
                selectionColor = new Color(70, 130, 180);
                lineNumberColor = new Color(128, 128, 128);
                break;
        }
        
        // Apply theme to all open tabs
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof EditorTab) {
                ((EditorTab) comp).applyTheme(backgroundColor, textColor, selectionColor);
            }
        }
        
        // Apply theme to main components
        getContentPane().setBackground(backgroundColor);
        tabbedPane.setBackground(backgroundColor);
        statusLabel.setBackground(backgroundColor);
        statusLabel.setForeground(textColor);
        statusLabel.setOpaque(true);
        
        repaint();
    }
    
    private void refreshCurrentTab() {
        EditorTab currentTab = getCurrentTab();
        if (currentTab != null) {
            currentTab.refresh();
        }
    }
    
    private void addToRecentFiles(String filePath) {
        recentFiles.remove(filePath);
        recentFiles.add(0, filePath);
        if (recentFiles.size() > MAX_RECENT_FILES) {
            recentFiles.remove(recentFiles.size() - 1);
        }
        // Update recent files menu if needed
        SwingUtilities.invokeLater(() -> {
            JMenu fileMenu = menuBar.getMenu(0);
            JMenu recentMenu = (JMenu) fileMenu.getMenuComponent(2);
            updateRecentFilesMenu(recentMenu);
        });
    }
    
    public void updateStatus() {
        EditorTab currentTab = getCurrentTab();
        if (currentTab != null) {
            String text = currentTab.getText();
            int characters = text.length();
            int lines = text.isEmpty() ? 1 : text.split("\n", -1).length;
            int caretPos = currentTab.getTextPane().getCaretPosition();
            
            String status = String.format("Characters: %d | Lines: %d | Cursor: %d | Language: %s",
                characters, lines, caretPos, currentTab.getLanguage());
            
            if (currentTab.getFile() != null) {
                status += " | File: " + currentTab.getFile().getName();
            }
            
            if (currentTab.isModified()) {
                status += " | Modified";
            }
            
            statusLabel.setText(status);
        }
    }
    
    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    // Getters for dialogs
    public EditorTab getCurrentEditorTab() {
        return getCurrentTab();
    }
    
    public Color getBackgroundColor() { return backgroundColor; }
    public Color getTextColor() { return textColor; }
    public Color getSelectionColor() { return selectionColor; }
    public Color getLineNumberColor() { return lineNumberColor; }
    public int getFontSize() { return fontSize; }
    public boolean getShowLineNumbers() { return showLineNumbers; }
    public boolean getWordWrap() { return wordWrap; }
    
    public void setBackgroundColor(Color color) { backgroundColor = color; applyTheme("Custom"); }
    public void setTextColor(Color color) { textColor = color; applyTheme("Custom"); }
    public void setSelectionColor(Color color) { selectionColor = color; applyTheme("Custom"); }
    public void setLineNumberColor(Color color) { lineNumberColor = color; applyTheme("Custom"); }
    public void setFontSize(int size) { 
        fontSize = size; 
        refreshCurrentTab();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            new AdvancedTextEditor().setVisible(true);
        });
    }
}

// Syntax Highlighting Engine
class SyntaxHighlighter {
    private Map<String, LanguageStyle> languageStyles;
    private String currentTheme;
    
    public SyntaxHighlighter(String theme) {
        this.currentTheme = theme;
        this.languageStyles = new HashMap<>();
        initializeLanguageStyles();
    }
    
    private void initializeLanguageStyles() {
        // Java Language Style
        LanguageStyle javaStyle = new LanguageStyle();
        javaStyle.keywords = Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null"
        );
        javaStyle.keywordColor = getColorForTheme("keyword");
        javaStyle.stringColor = getColorForTheme("string");
        javaStyle.commentColor = getColorForTheme("comment");
        javaStyle.numberColor = getColorForTheme("number");
        javaStyle.operatorColor = getColorForTheme("operator");
        languageStyles.put("Java", javaStyle);
        
        // Python Language Style
        LanguageStyle pythonStyle = new LanguageStyle();
        pythonStyle.keywords = Arrays.asList(
            "and", "as", "assert", "break", "class", "continue", "def", "del", "elif", "else", "except",
            "exec", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "not", "or",
            "pass", "print", "raise", "return", "try", "while", "with", "yield", "True", "False", "None"
        );
        pythonStyle.keywordColor = getColorForTheme("keyword");
        pythonStyle.stringColor = getColorForTheme("string");
        pythonStyle.commentColor = getColorForTheme("comment");
        pythonStyle.numberColor = getColorForTheme("number");
        pythonStyle.operatorColor = getColorForTheme("operator");
        languageStyles.put("Python", pythonStyle);
        
        // JavaScript Language Style
        LanguageStyle jsStyle = new LanguageStyle();
        jsStyle.keywords = Arrays.asList(
            "abstract", "arguments", "await", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "debugger", "default", "delete", "do", "double", "else", "enum", "eval",
            "export", "extends", "false", "final", "finally", "float", "for", "function", "goto", "if",
            "implements", "import", "in", "instanceof", "int", "interface", "let", "long", "native", "new",
            "null", "package", "private", "protected", "public", "return", "short", "static", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient", "true", "try", "typeof",
            "var", "void", "volatile", "while", "with", "yield"
        );
        jsStyle.keywordColor = getColorForTheme("keyword");
        jsStyle.stringColor = getColorForTheme("string");
        jsStyle.commentColor = getColorForTheme("comment");
        jsStyle.numberColor = getColorForTheme("number");
        jsStyle.operatorColor = getColorForTheme("operator");
        languageStyles.put("JavaScript", jsStyle);
        
        // HTML Language Style
        LanguageStyle htmlStyle = new LanguageStyle();
        htmlStyle.keywords = Arrays.asList(
            "html", "head", "title", "body", "div", "span", "p", "a", "img", "ul", "ol", "li", "table",
            "tr", "td", "th", "form", "input", "button", "select", "option", "textarea", "h1", "h2", "h3",
            "h4", "h5", "h6", "br", "hr", "meta", "link", "script", "style", "header", "footer", "nav",
            "section", "article", "aside", "main", "figure", "figcaption", "canvas", "video", "audio"
        );
        htmlStyle.keywordColor = getColorForTheme("htmlTag");
        htmlStyle.stringColor = getColorForTheme("string");
        htmlStyle.commentColor = getColorForTheme("comment");
        htmlStyle.numberColor = getColorForTheme("number");
        htmlStyle.operatorColor = getColorForTheme("operator");
        languageStyles.put("HTML", htmlStyle);
        
        // CSS Language Style
        LanguageStyle cssStyle = new LanguageStyle();
        cssStyle.keywords = Arrays.asList(
            "color", "background", "border", "margin", "padding", "width", "height", "font", "text",
            "display", "position", "top", "left", "right", "bottom", "float", "clear", "overflow",
            "visibility", "z-index", "opacity", "transform", "transition", "animation", "flex", "grid"
        );
        cssStyle.keywordColor = getColorForTheme("cssProperty");
        cssStyle.stringColor = getColorForTheme("string");
        cssStyle.commentColor = getColorForTheme("comment");
        cssStyle.numberColor = getColorForTheme("number");
        cssStyle.operatorColor = getColorForTheme("operator");
        languageStyles.put("CSS", cssStyle);
    }
    
    private Color getColorForTheme(String type) {
        switch (currentTheme) {
            case "Light":
                switch (type) {
                    case "keyword": return new Color(0, 0, 255);
                    case "string": return new Color(163, 21, 21);
                    case "comment": return new Color(0, 128, 0);
                    case "number": return new Color(255, 140, 0);
                    case "operator": return new Color(128, 0, 128);
                    case "htmlTag": return new Color(128, 0, 0);
                    case "cssProperty": return new Color(0, 0, 255);
                    default: return Color.BLACK;
                }
            case "Monokai":
                switch (type) {
                    case "keyword": return new Color(249, 38, 114);
                    case "string": return new Color(230, 219, 116);
                    case "comment": return new Color(117, 113, 94);
                    case "number": return new Color(174, 129, 255);
                    case "operator": return new Color(249, 38, 114);
                    case "htmlTag": return new Color(249, 38, 114);
                    case "cssProperty": return new Color(102, 217, 239);
                    default: return new Color(248, 248, 242);
                }
            case "Solarized Dark":
                switch (type) {
                    case "keyword": return new Color(268, 153, 132);
                    case "string": return new Color(42, 161, 152);
                    case "comment": return new Color(101, 123, 131);
                    case "number": return new Color(211, 54, 130);
                    case "operator": return new Color(147, 161, 161);
                    case "htmlTag": return new Color(268, 153, 132);
                    case "cssProperty": return new Color(38, 139, 210);
                    default: return new Color(131, 148, 150);
                }
            default: // Dark
                switch (type) {
                    case "keyword": return new Color(86, 156, 214);
                    case "string": return new Color(206, 145, 120);
                    case "comment": return new Color(106, 153, 85);
                    case "number": return new Color(181, 206, 168);
                    case "operator": return new Color(212, 212, 212);
                    case "htmlTag": return new Color(86, 156, 214);
                    case "cssProperty": return new Color(156, 220, 254);
                    default: return new Color(220, 220, 220);
                }
        }
    }
    
    public void highlightText(StyledDocument doc, String language) {
        if (!languageStyles.containsKey(language)) {
            return;
        }
        
        LanguageStyle style = languageStyles.get(language);
        String text = "";
        
        try {
            text = doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            return;
        }
        
        // Clear previous styles
        Style defaultStyle = doc.addStyle("default", null);
        StyleConstants.setForeground(defaultStyle, getColorForTheme("default"));
        doc.setCharacterAttributes(0, doc.getLength(), defaultStyle, true);
        
        // Highlight keywords
        highlightKeywords(doc, text, style);
        
        // Highlight strings
        highlightStrings(doc, text, style);
        
        // Highlight comments
        highlightComments(doc, text, style, language);
        
        // Highlight numbers
        highlightNumbers(doc, text, style);
        
        // Highlight operators
        highlightOperators(doc, text, style);
        
        // Language-specific highlighting
        if (language.equals("HTML")) {
            highlightHTMLTags(doc, text, style);
        } else if (language.equals("CSS")) {
            highlightCSSSelectors(doc, text, style);
        }
    }
    
    private void highlightKeywords(StyledDocument doc, String text, LanguageStyle style) {
        Style keywordStyle = doc.addStyle("keyword", null);
        StyleConstants.setForeground(keywordStyle, style.keywordColor);
        StyleConstants.setBold(keywordStyle, true);
        
        for (String keyword : style.keywords) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b");
            Matcher matcher = pattern.matcher(text);
            
            while (matcher.find()) {
                doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), keywordStyle, false);
            }
        }
    }
    
    private void highlightStrings(StyledDocument doc, String text, LanguageStyle style) {
        Style stringStyle = doc.addStyle("string", null);
        StyleConstants.setForeground(stringStyle, style.stringColor);
        
        // Double quotes
        Pattern doubleQuotePattern = Pattern.compile("\"([^\"\\\\]|\\\\.)*\"");
        Matcher matcher = doubleQuotePattern.matcher(text);
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), stringStyle, false);
        }
        
        // Single quotes
        Pattern singleQuotePattern = Pattern.compile("'([^'\\\\]|\\\\.)*'");
        matcher = singleQuotePattern.matcher(text);
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), stringStyle, false);
        }
    }
    
    private void highlightComments(StyledDocument doc, String text, LanguageStyle style, String language) {
        Style commentStyle = doc.addStyle("comment", null);
        StyleConstants.setForeground(commentStyle, style.commentColor);
        StyleConstants.setItalic(commentStyle, true);
        
        if (language.equals("Python")) {
            // Python single-line comments
            Pattern pattern = Pattern.compile("#.*$", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), commentStyle, false);
            }
        } else if (language.equals("HTML")) {
            // HTML comments
            Pattern pattern = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), commentStyle, false);
            }
        } else if (language.equals("CSS")) {
            // CSS comments
            Pattern pattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), commentStyle, false);
            }
        } else {
            // Java/JavaScript style comments
            // Single-line comments
            Pattern singlePattern = Pattern.compile("//.*$", Pattern.MULTILINE);
            Matcher matcher = singlePattern.matcher(text);
            while (matcher.find()) {
                doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), commentStyle, false);
            }
            
            // Multi-line comments
            Pattern multiPattern = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
            matcher = multiPattern.matcher(text);
            while (matcher.find()) {
                doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), commentStyle, false);
            }
        }
    }
    
    private void highlightNumbers(StyledDocument doc, String text, LanguageStyle style) {
        Style numberStyle = doc.addStyle("number", null);
        StyleConstants.setForeground(numberStyle, style.numberColor);
        
        Pattern pattern = Pattern.compile("\\b\\d+(\\.\\d+)?([eE][+-]?\\d+)?[fFdD]?\\b");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), numberStyle, false);
        }
    }
    
    private void highlightOperators(StyledDocument doc, String text, LanguageStyle style) {
        Style operatorStyle = doc.addStyle("operator", null);
        StyleConstants.setForeground(operatorStyle, style.operatorColor);
        
        String operators = "\\+|\\-|\\*|\\/|%|==|!=|<=|>=|<|>|&&|\\|\\||!|&|\\||\\^|~|<<|>>|\\?|:";
        Pattern pattern = Pattern.compile(operators);
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), operatorStyle, false);
        }
    }
    
    private void highlightHTMLTags(StyledDocument doc, String text, LanguageStyle style) {
        Style tagStyle = doc.addStyle("htmlTag", null);
        StyleConstants.setForeground(tagStyle, style.keywordColor);
        StyleConstants.setBold(tagStyle, true);
        
        Pattern pattern = Pattern.compile("</?\\w+.*?>");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            doc.setCharacterAttributes(matcher.start(), matcher.end() - matcher.start(), tagStyle, false);
        }
    }
    
    private void highlightCSSSelectors(StyledDocument doc, String text, LanguageStyle style) {
        Style selectorStyle = doc.addStyle("cssSelector", null);
        StyleConstants.setForeground(selectorStyle, getColorForTheme("keyword"));
        StyleConstants.setBold(selectorStyle, true);
        
        Pattern pattern = Pattern.compile("^\\s*[.#]?[\\w-]+\\s*\\{", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            String match = matcher.group();
            int start = matcher.start();
            int end = start + match.indexOf('{');
            doc.setCharacterAttributes(start, end - start, selectorStyle, false);
        }
    }
    
    public void setTheme(String theme) {
        this.currentTheme = theme;
        initializeLanguageStyles();
    }
}

// Language Style class
class LanguageStyle {
    public List<String> keywords;
    public Color keywordColor;
    public Color stringColor;
    public Color commentColor;
    public Color numberColor;
    public Color operatorColor;
}

// Enhanced EditorTab class with syntax highlighting
class EditorTab extends JPanel {
    private JTextPane textPane;
    private JTextArea lineNumberArea;
    private JScrollPane scrollPane;
    private File file;
    private boolean modified = false;
    private String language = "Plain Text";
    private UndoManager undoManager;
    private AdvancedTextEditor parent;
    private SyntaxHighlighter syntaxHighlighter;
    private Timer syntaxTimer;
    
    public EditorTab(AdvancedTextEditor parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        initializeComponents();
        setupUndoRedo();
        setupSyntaxHighlighting();
    }
    
    private void initializeComponents() {
        textPane = new JTextPane();
        textPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        
        lineNumberArea = new JTextArea();
        lineNumberArea.setFont(textPane.getFont());
        lineNumberArea.setEditable(false);
        lineNumberArea.setFocusable(false);
        lineNumberArea.setBackground(new Color(240, 240, 240));
        lineNumberArea.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        
        scrollPane = new JScrollPane(textPane);
        scrollPane.setRowHeaderView(lineNumberArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        
        add(scrollPane, BorderLayout.CENTER);
        
        // Document listener for modifications and syntax highlighting
        textPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                setModified(true);
                updateLineNumbers();
                scheduleSyntaxHighlighting();
                if (parent != null) {
                    SwingUtilities.invokeLater(() -> parent.updateStatus());
                }
            }
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                setModified(true);
                updateLineNumbers();
                scheduleSyntaxHighlighting();
                if (parent != null) {
                    SwingUtilities.invokeLater(() -> parent.updateStatus());
                }
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
                updateLineNumbers();
                if (parent != null) {
                    SwingUtilities.invokeLater(() -> parent.updateStatus());
                }
            }
        });
        
        // Caret listener for status updates
        textPane.addCaretListener(e -> {
            if (parent != null) {
                SwingUtilities.invokeLater(() -> parent.updateStatus());
            }
        });
        
        // Initial line numbers
        updateLineNumbers();
    }
    
    private void setupUndoRedo() {
        undoManager = new UndoManager();
        textPane.getDocument().addUndoableEditListener(undoManager);
    }
    
    private void setupSyntaxHighlighting() {
        syntaxHighlighter = new SyntaxHighlighter("Dark");
        
        // Timer to delay syntax highlighting for better performance
        syntaxTimer = new Timer(500, e -> {
            applySyntaxHighlighting();
            syntaxTimer.stop();
        });
        syntaxTimer.setRepeats(false);
    }
    
    private void scheduleSyntaxHighlighting() {
        if (syntaxTimer.isRunning()) {
            syntaxTimer.restart();
        } else {
            syntaxTimer.start();
        }
    }
    
    private void applySyntaxHighlighting() {
        if (!language.equals("Plain Text")) {
            SwingUtilities.invokeLater(() -> {
                int caretPos = textPane.getCaretPosition();
                syntaxHighlighter.highlightText(textPane.getStyledDocument(), language);
                try {
                    textPane.setCaretPosition(Math.min(caretPos, textPane.getDocument().getLength()));
                } catch (IllegalArgumentException e) {
                    // Ignore invalid caret position
                }
            });
        }
    }
    
    private void updateLineNumbers() {
        SwingUtilities.invokeLater(() -> {
            try {
                int lines = textPane.getDocument().getDefaultRootElement().getElementCount();
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= lines; i++) {
                    sb.append(String.format("%3d\n", i));
                }
                lineNumberArea.setText(sb.toString());
            } catch (Exception e) {
                // Handle any exceptions gracefully
            }
        });
    }
    
    public void duplicateLine() {
        try {
            int caretPos = textPane.getCaretPosition();
            Element root = textPane.getDocument().getDefaultRootElement();
            int lineNum = root.getElementIndex(caretPos);
            Element line = root.getElement(lineNum);
            int lineStart = line.getStartOffset();
            int lineEnd = line.getEndOffset();
            
            String lineText = textPane.getText(lineStart, lineEnd - lineStart);
            textPane.getDocument().insertString(lineEnd, lineText, null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    public void commentUncommentLines() {
        String commentPrefix = getCommentPrefix();
        
        try {
            int caretPos = textPane.getCaretPosition();
            Element root = textPane.getDocument().getDefaultRootElement();
            int lineNum = root.getElementIndex(caretPos);
            Element line = root.getElement(lineNum);
            int lineStart = line.getStartOffset();
            int lineEnd = line.getEndOffset();
            
            String lineText = textPane.getText(lineStart, lineEnd - lineStart);
            String trimmedLine = lineText.trim();
            
            if (trimmedLine.startsWith(commentPrefix.trim())) {
                // Uncomment
                String newLine = lineText.replaceFirst(Pattern.quote(commentPrefix), "");
                textPane.getDocument().remove(lineStart, lineEnd - lineStart);
                textPane.getDocument().insertString(lineStart, newLine, null);
            } else {
                // Comment
                String newLine = commentPrefix + lineText;
                textPane.getDocument().remove(lineStart, lineEnd - lineStart);
                textPane.getDocument().insertString(lineStart, newLine, null);
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    private String getCommentPrefix() {
        switch (language) {
            case "Java":
            case "JavaScript":
            case "C":
            case "C++":
            case "CSS":
                return "// ";
            case "Python":
                return "# ";
            case "HTML":
            case "XML":
                return "<!-- ";
            default:
                return "# ";
        }
    }
    
    public void undo() {
        if (undoManager.canUndo()) {
            undoManager.undo();
        }
    }
    
    public void redo() {
        if (undoManager.canRedo()) {
            undoManager.redo();
        }
    }
    
    public void refresh() {
        updateLineNumbers();
        applySyntaxHighlighting();
        repaint();
    }
    
    public void applyTheme(Color backgroundColor, Color textColor, Color selectionColor) {
        textPane.setBackground(backgroundColor);
        textPane.setForeground(textColor);
        textPane.setSelectionColor(selectionColor);
        textPane.setCaretColor(textColor);
        
        lineNumberArea.setBackground(backgroundColor.brighter());
        lineNumberArea.setForeground(textColor.darker());
        
        scrollPane.getViewport().setBackground(backgroundColor);
        
        // Update syntax highlighter theme
        String theme = "Dark";
        if (backgroundColor.equals(new Color(240, 240, 240))) {
            theme = "Light";
        } else if (backgroundColor.equals(new Color(39, 40, 34))) {
            theme = "Monokai";
        } else if (backgroundColor.equals(new Color(0, 43, 54))) {
            theme = "Solarized Dark";
        }
        
        syntaxHighlighter.setTheme(theme);
        applySyntaxHighlighting();
    }
    
    public void detectLanguage() {
        if (file != null) {
            String fileName = file.getName().toLowerCase();
            if (fileName.endsWith(".java")) {
                language = "Java";
            } else if (fileName.endsWith(".py")) {
                language = "Python";
            } else if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
                language = "HTML";
            } else if (fileName.endsWith(".js")) {
                language = "JavaScript";
            } else if (fileName.endsWith(".css")) {
                language = "CSS";
            } else if (fileName.endsWith(".xml")) {
                language = "XML";
            } else if (fileName.endsWith(".json")) {
                language = "JSON";
            } else {
                language = "Plain Text";
            }
            
            // Apply syntax highlighting after language detection
            applySyntaxHighlighting();
        }
    }
    
    // Getters and setters
    public JTextPane getTextPane() { return textPane; }
    public File getFile() { return file; }
    public void setFile(File file) { 
        this.file = file; 
        detectLanguage();
    }
    public String getText() { return textPane.getText(); }
    public void setText(String text) { 
        textPane.setText(text); 
        applySyntaxHighlighting();
    }
    public boolean isModified() { return modified; }
    public void setModified(boolean modified) { this.modified = modified; }
    public String getLanguage() { return language; }
}

// Tab component with close button
class TabComponent extends JPanel {
    private final String title;
    private final Runnable closeAction;
    
    public TabComponent(String title, Runnable closeAction) {
        this.title = title;
        this.closeAction = closeAction;
        
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        add(titleLabel);
        
        JButton closeButton = new JButton("×");
        closeButton.setPreferredSize(new Dimension(17, 17));
        closeButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        closeButton.setFocusable(false);
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.addActionListener(e -> closeAction.run());
        add(closeButton);
    }
}

// Find and Replace Dialog
class FindReplaceDialog extends JDialog {
    private JTextField findField;
    private JTextField replaceField;
    private JCheckBox caseSensitiveBox;
    private JCheckBox regexBox;
    private JLabel statusLabel;
    private AdvancedTextEditor parent;
    
    public FindReplaceDialog(AdvancedTextEditor parent) {
        super(parent, "Find & Replace", false);
        this.parent = parent;
        initializeComponents();
        setSize(400, 200);
        setLocationRelativeTo(parent);
    }
    
    private void initializeComponents() {
        findField = new JTextField(20);
        replaceField = new JTextField(20);
        caseSensitiveBox = new JCheckBox("Case Sensitive");
        regexBox = new JCheckBox("Regular Expression");
        statusLabel = new JLabel(" ");
        
        JButton findNextButton = new JButton("Find Next");
        findNextButton.addActionListener(e -> findNext());
        
        JButton findPrevButton = new JButton("Find Previous");
        findPrevButton.addActionListener(e -> findPrevious());
        
        JButton replaceButton = new JButton("Replace");
        replaceButton.addActionListener(e -> replace());
        
        JButton replaceAllButton = new JButton("Replace All");
        replaceAllButton.addActionListener(e -> replaceAll());
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(findNextButton);
        buttonPanel.add(findPrevButton);
        buttonPanel.add(replaceButton);
        buttonPanel.add(replaceAllButton);
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        add(new JLabel("Find:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        add(findField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.NONE;
        add(new JLabel("Replace:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        add(replaceField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE;
        add(caseSensitiveBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        add(regexBox, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        add(buttonPanel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        add(statusLabel, gbc);
    }
    
    private void findNext() {
        String searchText = findField.getText();
        if (searchText.isEmpty()) return;
        
        EditorTab currentTab = parent.getCurrentEditorTab();
        if (currentTab != null) {
            JTextPane textPane = currentTab.getTextPane();
            String content = textPane.getText();
            int startPos = textPane.getCaretPosition();
            
            int index = findInText(content, searchText, startPos, true);
            if (index != -1) {
                textPane.setCaretPosition(index);
                textPane.select(index, index + searchText.length());
                statusLabel.setText("Found at position " + index);
            } else {
                statusLabel.setText("Not found");
            }
        }
    }
    
    private void findPrevious() {
        String searchText = findField.getText();
        if (searchText.isEmpty()) return;
        
        EditorTab currentTab = parent.getCurrentEditorTab();
        if (currentTab != null) {
            JTextPane textPane = currentTab.getTextPane();
            String content = textPane.getText();
            int startPos = Math.max(0, textPane.getSelectionStart() - 1);
            
            int index = findInText(content, searchText, startPos, false);
            if (index != -1) {
                textPane.setCaretPosition(index);
                textPane.select(index, index + searchText.length());
                statusLabel.setText("Found at position " + index);
            } else {
                statusLabel.setText("Not found");
            }
        }
    }
    
    private void replace() {
        EditorTab currentTab = parent.getCurrentEditorTab();
        if (currentTab != null) {
            JTextPane textPane = currentTab.getTextPane();
            String selectedText = textPane.getSelectedText();
            String findText = findField.getText();
            String replaceText = replaceField.getText();
            
            if (selectedText != null && selectedText.equals(findText)) {
                textPane.replaceSelection(replaceText);
                statusLabel.setText("Replaced");
            }
            findNext();
        }
    }
    
    private void replaceAll() {
        String findText = findField.getText();
        String replaceText = replaceField.getText();
        
        if (findText.isEmpty()) return;
        
        EditorTab currentTab = parent.getCurrentEditorTab();
        if (currentTab != null) {
            JTextPane textPane = currentTab.getTextPane();
            String content = textPane.getText();
            
            int count = 0;
            if (regexBox.isSelected()) {
                try {
                    int flags = caseSensitiveBox.isSelected() ? 0 : Pattern.CASE_INSENSITIVE;
                    Pattern pattern = Pattern.compile(findText, flags);
                    Matcher matcher = pattern.matcher(content);
                    
                    StringBuffer result = new StringBuffer();
                    while (matcher.find()) {
                        matcher.appendReplacement(result, replaceText);
                        count++;
                    }
                    matcher.appendTail(result);
                    content = result.toString();
                } catch (Exception e) {
                    statusLabel.setText("Invalid regex: " + e.getMessage());
                    return;
                }
            } else {
                String searchText = caseSensitiveBox.isSelected() ? findText : findText.toLowerCase();
                String searchContent = caseSensitiveBox.isSelected() ? content : content.toLowerCase();
                
                int index = 0;
                StringBuilder result = new StringBuilder();
                int lastIndex = 0;
                
                while ((index = searchContent.indexOf(searchText, index)) != -1) {
                    result.append(content.substring(lastIndex, index));
                    result.append(replaceText);
                    lastIndex = index + findText.length();
                    index = lastIndex;
                    count++;
                }
                result.append(content.substring(lastIndex));
                content = result.toString();
            }
            
            textPane.setText(content);
            statusLabel.setText("Replaced " + count + " occurrences");
        }
    }
    
    private int findInText(String content, String searchText, int startPos, boolean forward) {
        if (startPos < 0) startPos = 0;
        if (startPos >= content.length()) startPos = content.length() - 1;
        
        String searchContent = caseSensitiveBox.isSelected() ? content : content.toLowerCase();
        String search = caseSensitiveBox.isSelected() ? searchText : searchText.toLowerCase();
        
        if (forward) {
            return searchContent.indexOf(search, startPos);
        } else {
            return searchContent.lastIndexOf(search, startPos);
        }
    }
}

// Go to Line Dialog
class GoToLineDialog extends JDialog {
    private JTextField lineField;
    private AdvancedTextEditor parent;
    
    public GoToLineDialog(AdvancedTextEditor parent) {
        super(parent, "Go to Line", true);
        this.parent = parent;
        initializeComponents();
        setSize(250, 120);
        setLocationRelativeTo(parent);
    }
    
    private void initializeComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        lineField = new JTextField(10);
        JButton goButton = new JButton("Go");
        JButton cancelButton = new JButton("Cancel");
        
        goButton.addActionListener(e -> goToLine());
        cancelButton.addActionListener(e -> setVisible(false));
        
        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Line number:"), gbc);
        
        gbc.gridx = 1;
        add(lineField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(goButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, gbc);
        
        // Enter key listener
        lineField.addActionListener(e -> goToLine());
    }
    
    private void goToLine() {
        try {
            int lineNumber = Integer.parseInt(lineField.getText());
            EditorTab currentTab = parent.getCurrentEditorTab();
            
            if (currentTab != null) {
                JTextPane textPane = currentTab.getTextPane();
                Element root = textPane.getDocument().getDefaultRootElement();
                int totalLines = root.getElementCount();
                
                if (lineNumber > 0 && lineNumber <= totalLines) {
                    Element line = root.getElement(lineNumber - 1);
                    int offset = line.getStartOffset();
                    textPane.setCaretPosition(offset);
                    setVisible(false);
                    lineField.setText("");
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Line number must be between 1 and " + totalLines,
                        "Invalid Line Number", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number",
                "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }
}

// Style Configuration Dialog
class StyleConfigDialog extends JDialog {
    private AdvancedTextEditor parent;
    private JButton backgroundColorButton;
    private JButton textColorButton;
    private JButton selectionColorButton;
    private JButton lineNumberColorButton;
    private JSpinner fontSizeSpinner;
    
    public StyleConfigDialog(AdvancedTextEditor parent) {
        super(parent, "Style Configuration", false);
        this.parent = parent;
        initializeComponents();
        setSize(400, 300);
        setLocationRelativeTo(parent);
    }
    
    private void initializeComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Color buttons
        backgroundColorButton = createColorButton("Background Color", parent.getBackgroundColor());
        textColorButton = createColorButton("Text Color", parent.getTextColor());
        selectionColorButton = createColorButton("Selection Color", parent.getSelectionColor());
        lineNumberColorButton = createColorButton("Line Number Color", parent.getLineNumberColor());
        
        // Font size spinner
        fontSizeSpinner = new JSpinner(new SpinnerNumberModel(parent.getFontSize(), 8, 72, 1));
        fontSizeSpinner.addChangeListener(e -> parent.setFontSize((Integer) fontSizeSpinner.getValue()));
        
        // Layout components
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST;
        add(new JLabel("Colors:"), gbc);
        
        gbc.gridy = 1;
        add(backgroundColorButton, gbc);
        gbc.gridy = 2;
        add(textColorButton, gbc);
        gbc.gridy = 3;
        add(selectionColorButton, gbc);
        gbc.gridy = 4;
        add(lineNumberColorButton, gbc);
        
        gbc.gridy = 5;
        add(new JLabel("Font Size:"), gbc);
        gbc.gridx = 1;
        add(fontSizeSpinner, gbc);
        
        // Theme buttons
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        add(new JLabel("Quick Themes:"), gbc);
        
        JPanel themePanel = new JPanel();
        themePanel.add(createThemeButton("Dark"));
        themePanel.add(createThemeButton("Light"));
        themePanel.add(createThemeButton("Monokai"));
        themePanel.add(createThemeButton("Solarized"));
        
        gbc.gridy = 7;
        add(themePanel, gbc);
    }
    
    private JButton createColorButton(String text, Color initialColor) {
        JButton button = new JButton(text);
        button.setBackground(initialColor);
        button.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose " + text, initialColor);
            if (newColor != null) {
                button.setBackground(newColor);
                updateParentColor(text, newColor);
            }
        });
        return button;
    }
    
    private JButton createThemeButton(String theme) {
        JButton button = new JButton(theme);
        button.addActionListener(e -> parent.applyTheme(theme.equals("Solarized") ? "Solarized Dark" : theme));
        return button;
    }
    
    private void updateParentColor(String colorType, Color color) {
        switch (colorType) {
            case "Background Color":
                parent.setBackgroundColor(color);
                break;
            case "Text Color":
                parent.setTextColor(color);
                break;
            case "Selection Color":
                parent.setSelectionColor(color);
                break;
            case "Line Number Color":
                parent.setLineNumberColor(color);
                break;
        }
    }
}

// File Explorer Dialog
class FileExplorerDialog extends JDialog {
    private AdvancedTextEditor parent;
    private JTree fileTree;
    private File currentDirectory;
    
    public FileExplorerDialog(AdvancedTextEditor parent) {
        super(parent, "File Explorer", false);
        this.parent = parent;
        initializeComponents();
        setSize(300, 400);
        setLocationRelativeTo(parent);
        
        currentDirectory = new File(System.getProperty("user.home"));
        refreshTree();
    }
    
    private void initializeComponents() {
        setLayout(new BorderLayout());
        
        JButton selectFolderButton = new JButton("Select Folder");
        selectFolderButton.addActionListener(e -> selectFolder());
        
        fileTree = new JTree();
        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedFile();
                }
            }
        });
        
        add(selectFolderButton, BorderLayout.NORTH);
        add(new JScrollPane(fileTree), BorderLayout.CENTER);
    }
    
    private void selectFolder() {
        JFileChooser chooser = new JFileChooser(currentDirectory);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentDirectory = chooser.getSelectedFile();
            refreshTree();
        }
    }
    
    private void refreshTree() {
        if (currentDirectory != null && currentDirectory.exists()) {
            fileTree.setModel(new FileTreeModel(currentDirectory));
        }
    }
    
    private void openSelectedFile() {
        Object selectedNode = fileTree.getLastSelectedPathComponent();
        if (selectedNode instanceof FileTreeNode) {
            FileTreeNode node = (FileTreeNode) selectedNode;
            File file = (File) node.getUserObject();
            if (file.isFile()) {
                parent.openFileExternal(file);
            }
        }
    }
}

// Simple File Tree Model
class FileTreeModel extends javax.swing.tree.DefaultTreeModel {
    public FileTreeModel(File root) {
        super(new FileTreeNode(root));
    }
}

class FileTreeNode extends javax.swing.tree.DefaultMutableTreeNode {
    public FileTreeNode(File file) {
        super(file);
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                Arrays.sort(children, (f1, f2) -> {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareToIgnoreCase(f2.getName());
                });
                
                for (File child : children) {
                    if (!child.isHidden()) {
                        add(new FileTreeNode(child));
                    }
                }
            }
        }
    }
    
    @Override
    public String toString() {
        File file = (File) getUserObject();
        return file.getName().isEmpty() ? file.getPath() : file.getName();
    }
}

// Terminal Dialog
class TerminalDialog extends JDialog {
    private AdvancedTextEditor parent;
    private JTextArea outputArea;
    private JTextField inputField;
    private File currentDir;
    
    public TerminalDialog(AdvancedTextEditor parent) {
        super(parent, "Terminal", false);
        this.parent = parent;
        initializeComponents();
        setSize(600, 400);
        setLocationRelativeTo(parent);
        
        currentDir = new File(System.getProperty("user.dir"));
        outputArea.append("Terminal ready. Current directory: " + currentDir.getAbsolutePath() + "\n");
    }
    
    private void initializeComponents() {
        setLayout(new BorderLayout());
        
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        outputArea.setBackground(Color.BLACK);
        outputArea.setForeground(Color.GREEN);
        
        inputField = new JTextField();
        inputField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        inputField.setBackground(Color.BLACK);
        inputField.setForeground(Color.GREEN);
        inputField.addActionListener(e -> executeCommand());
        
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JLabel("$ "), BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> outputArea.setText(""));
        
        add(new JScrollPane(outputArea), BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
        add(clearButton, BorderLayout.NORTH);
    }
    
    private void executeCommand() {
        String command = inputField.getText().trim();
        if (command.isEmpty()) return;
        
        outputArea.append("$ " + command + "\n");
        inputField.setText("");
        
        try {
            if (command.equals("pwd")) {
                outputArea.append(currentDir.getAbsolutePath() + "\n");
            } else if (command.equals("ls") || command.equals("dir")) {
                File[] files = currentDir.listFiles();
                if (files != null) {
                    Arrays.sort(files, (f1, f2) -> {
                        if (f1.isDirectory() && !f2.isDirectory()) return -1;
                        if (!f1.isDirectory() && f2.isDirectory()) return 1;
                        return f1.getName().compareToIgnoreCase(f2.getName());
                    });
                    
                    for (File file : files) {
                        outputArea.append(file.getName() + (file.isDirectory() ? "/" : "") + "\n");
                    }
                }
            } else if (command.startsWith("cd ")) {
                String path = command.substring(3).trim();
                File newDir;
                if (path.equals("..")) {
                    newDir = currentDir.getParentFile();
                } else if (path.equals("~")) {
                    newDir = new File(System.getProperty("user.home"));
                } else {
                    newDir = new File(currentDir, path);
                }
                
                if (newDir != null && newDir.exists() && newDir.isDirectory()) {
                    currentDir = newDir.getCanonicalFile();
                    outputArea.append("Changed directory to: " + currentDir.getAbsolutePath() + "\n");
                } else {
                    outputArea.append("Directory not found: " + path + "\n");
                }
            } else if (command.equals("clear")) {
                outputArea.setText("");
            } else if (command.startsWith("echo ")) {
                outputArea.append(command.substring(5) + "\n");
            } else {
                // Try to execute as system command
                executeSystemCommand(command);
            }
        } catch (Exception e) {
            outputArea.append("Error: " + e.getMessage() + "\n");
        }
        
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }
    
    private void executeSystemCommand(String command) {
        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                ProcessBuilder pb = new ProcessBuilder();
                
                // Handle different operating systems
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    pb.command("cmd.exe", "/c", command);
                } else {
                    pb.command("sh", "-c", command);
                }
                
                pb.directory(currentDir);
                Process process = pb.start();
                
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                     BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        publish(line);
                    }
                    
                    while ((line = errorReader.readLine()) != null) {
                        publish("ERROR: " + line);
                    }
                }
                
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    publish("Command exited with code: " + exitCode);
                }
                
                return null;
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                for (String chunk : chunks) {
                    outputArea.append(chunk + "\n");
                }
            }
            
            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    outputArea.append("Error executing command: " + e.getMessage() + "\n");
                }
                outputArea.setCaretPosition(outputArea.getDocument().getLength());
            }
        };
        
        worker.execute();
    }
}
