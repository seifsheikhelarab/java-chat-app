package Client;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;

public class ChatClient extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String currentRoom = "lobby";
    
    // UI Components
    private JTextArea chatArea;
    private JTextField inputField;
    private JList<String> userList;
    private JList<String> roomList;
    private DefaultListModel<String> userListModel;
    private DefaultListModel<String> roomListModel;
    private JLabel statusLabel;
    
    // Colors
    private final Color PRIMARY_COLOR = new Color(70, 130, 180);
    private final Color SECONDARY_COLOR = new Color(245, 245, 245);
    private final Color ACCENT_COLOR = new Color(100, 149, 237);
    
    public ChatClient() {
        initializeUI();
        connectToServer();
    }
    
    private void initializeUI() {
        setTitle("NeonChat - Java Chat Client");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Main container
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(SECONDARY_COLOR);
        
        // Header panel
        JPanel headerPanel = createHeaderPanel();
        
        // Chat area
        chatArea = new JTextArea();
        styleChatArea();
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(new LineBorder(new Color(220, 220, 220), 1));
        
        // Input panel
        JPanel inputPanel = createInputPanel();
        
        // Side panel
        JPanel sidePanel = createSidePanel();
        
        // Add components to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(chatScroll, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);
        mainPanel.add(sidePanel, BorderLayout.EAST);
        
        // Menu bar
        createMenuBar();
        
        add(mainPanel);
        setVisible(true);
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
        
        JLabel titleLabel = new JLabel("NeonChat");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        
        statusLabel = new JLabel("Connecting...");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(statusLabel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private void styleChatArea() {
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        chatArea.setBackground(new Color(250, 250, 250));
    }
    
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        inputField = new JTextField();
        styleInputField();
        
        JButton sendButton = new JButton("Send");
        styleButton(sendButton, PRIMARY_COLOR);
        sendButton.addActionListener(e -> sendMessage());
        
        JPanel buttonPanel = createButtonPanel(sendButton);
        
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        
        return inputPanel;
    }
    
    private void styleInputField() {
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBorder(new CompoundBorder(
            new LineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(8, 8, 8, 8)
        ));
        inputField.addActionListener(e -> sendMessage());
    }
    
    private JPanel createButtonPanel(JButton sendButton) {
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 5, 0));
        buttonPanel.setBorder(new EmptyBorder(0, 5, 0, 0));
        
        JButton emojiButton = new JButton("😀");
        styleButton(emojiButton, ACCENT_COLOR);
        emojiButton.addActionListener(e -> showEmojiPicker());
        
        JButton fileButton = new JButton("📁");
        styleButton(fileButton, ACCENT_COLOR);
        fileButton.addActionListener(e -> sendFile());
        
        buttonPanel.add(emojiButton);
        buttonPanel.add(fileButton);
        buttonPanel.add(sendButton);
        
        return buttonPanel;
    }
    
    private JPanel createSidePanel() {
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBorder(new EmptyBorder(0, 10, 0, 0));
        
        // Users card
        JPanel usersCard = createCardPanel("Online Users");
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setCellRenderer(new UserListCellRenderer());
        JScrollPane userScroll = new JScrollPane(userList);
        usersCard.add(userScroll);
        
        // Rooms card
        JPanel roomsCard = createCardPanel("Chat Rooms");
        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        roomList.setCellRenderer(new RoomListCellRenderer());
        roomList.addListSelectionListener(e -> handleRoomSelection());
        
        JScrollPane roomScroll = new JScrollPane(roomList);
        roomsCard.add(roomScroll);
        
        sidePanel.add(usersCard);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(roomsCard);
        
        return sidePanel;
    }
    
    private void handleRoomSelection() {
        if (!roomList.getValueIsAdjusting()) {
            String selectedRoom = roomList.getSelectedValue();
            if (selectedRoom != null && !selectedRoom.equals(currentRoom)) {
                out.println("/room " + selectedRoom);
            }
        }
    }
    
    private JPanel createCardPanel(String title) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setBorder(new EmptyBorder(0, 0, 10, 0));
        
        card.add(titleLabel, BorderLayout.NORTH);
        return card;
    }
    
    private void styleButton(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });
    }
    
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(Color.WHITE);
        
        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        JMenuItem connectItem = new JMenuItem("Reconnect");
        connectItem.addActionListener(e -> reconnect());
        
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        
        fileMenu.add(connectItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        // View menu
        JMenu viewMenu = new JMenu("View");
        viewMenu.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        JCheckBoxMenuItem timestampItem = new JCheckBoxMenuItem("Show timestamps", true);
        viewMenu.add(timestampItem);
        
        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        
        helpMenu.add(aboutItem);
        
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
            "NeonChat v1.0\nA modern Java chat application\n\n" +
            "Features:\n" +
            "- Multiple chat rooms\n" +
            "- Private messaging\n" +
            "- User list tracking\n" +
            "- Modern UI with emoji support",
            "About NeonChat",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showEmojiPicker() {
        String[] emojis = {"😀", "😂", "😍", "😎", "👍", "❤️", "🔥", "🎉", "🙏", "🤔"};
        String selectedEmoji = (String) JOptionPane.showInputDialog(
            this,
            "Select an emoji:",
            "Emoji Picker",
            JOptionPane.PLAIN_MESSAGE,
            null,
            emojis,
            emojis[0]);
        
        if (selectedEmoji != null) {
            inputField.setText(inputField.getText() + selectedEmoji);
        }
    }
    
    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            appendToChatArea("[You sent a file: " + selectedFile.getName() + "]", true);
            // In a real implementation, you would send the file here
        }
    }
    
    // Custom cell renderers
    class UserListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                                                    boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setIcon(UIManager.getIcon("Tree.leafIcon")); // Use a default system icon
            label.setBorder(new EmptyBorder(5, 5, 5, 5));
            return label;
        }
    }
    
    class RoomListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                                                     boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setIcon(UIManager.getIcon("Tree.openIcon")); // Use a default system icon
            label.setBorder(new EmptyBorder(5, 5, 5, 5));
            return label;
        }
    }
    
    // Network methods
    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Get username
            username = JOptionPane.showInputDialog(this, "Enter your username:");
            if (username == null || username.trim().isEmpty()) {
                username = "Guest" + (int)(Math.random() * 1000);
            }
            out.println(username);
            
            // Start message reader thread
            new Thread(this::readMessages).start();
            
            // Request initial lists
            out.println("/users");
            out.println("/rooms");
            
            statusLabel.setText("Connected to: " + currentRoom);
            
        } catch (IOException e) {
            appendToChatArea("Error: Could not connect to server", true);
            statusLabel.setText("Disconnected");
        }
    }
    
    private void readMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                processServerMessage(message);
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                appendToChatArea("Disconnected from server", true);
                statusLabel.setText("Disconnected");
                inputField.setEnabled(false);
            });
        }
    }
    
    private void processServerMessage(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        
        if (message.startsWith("[USERLIST]")) {
            SwingUtilities.invokeLater(() -> updateUserList(message.substring(10)));
        } 
        else if (message.startsWith("[ROOMLIST]")) {
            SwingUtilities.invokeLater(() -> updateRoomList(message.substring(10)));
        } 
        else if (message.startsWith("[ROOMCHANGE]")) {
            currentRoom = message.substring(12);
            SwingUtilities.invokeLater(() -> {
                appendToChatArea("Joined room: " + currentRoom, true);
                statusLabel.setText("Connected to: " + currentRoom);
            });
        }
        else {
            SwingUtilities.invokeLater(() -> appendToChatArea(message, false));
        }
    }
    
    private void appendToChatArea(String message, boolean isSystem) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        if (isSystem) {
            chatArea.append("[" + timestamp + "] " + message + "\n");
        } else {
            chatArea.append("[" + timestamp + "] " + message + "\n");
        }
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    private void updateUserList(String userListStr) {
        userListModel.clear();
        for (String user : userListStr.split(",")) {
            if (!user.isEmpty()) {
                userListModel.addElement(user);
            }
        }
    }
    
    private void updateRoomList(String roomListStr) {
        roomListModel.clear();
        for (String room : roomListStr.split(",")) {
            if (!room.isEmpty()) {
                roomListModel.addElement(room);
            }
        }
    }
    
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            out.println(message);
            inputField.setText("");
        }
    }
    
    private void reconnect() {
        try {
            if (socket != null) socket.close();
            if (in != null) in.close();
            if (out != null) out.close();
            
            connectToServer();
            inputField.setEnabled(true);
            appendToChatArea("Reconnected to server", true);
        } catch (IOException e) {
            appendToChatArea("Reconnect failed: " + e.getMessage(), true);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                new ChatClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}