package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClient extends JFrame {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String currentRoom = "lobby"; // Added this line

    private JTextArea chatArea;
    private JTextField inputField;
    private JList<String> userList;
    private JList<String> roomList;
    private DefaultListModel<String> userListModel;
    private DefaultListModel<String> roomListModel;

    public ChatClient() {
        initializeUI();
        connectToServer();
    }

    private void initializeUI() {
        setTitle("Java Chat Client");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Main panel with border layout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane chatScroll = new JScrollPane(chatArea);
        mainPanel.add(chatScroll, BorderLayout.CENTER);

        // Input panel at bottom
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        inputField.addActionListener(e -> sendMessage());
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        // Side panel with users and rooms
        JPanel sidePanel = new JPanel(new GridLayout(2, 1));

        // User list
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userScroll = new JScrollPane(userList);
        JPanel userPanel = new JPanel(new BorderLayout());
        userPanel.add(new JLabel("Online Users"), BorderLayout.NORTH);
        userPanel.add(userScroll, BorderLayout.CENTER);

        // Room list
        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        JScrollPane roomScroll = new JScrollPane(roomList);
        JPanel roomPanel = new JPanel(new BorderLayout());
        roomPanel.add(new JLabel("Rooms"), BorderLayout.NORTH);
        roomPanel.add(roomScroll, BorderLayout.CENTER);
        roomList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedRoom = roomList.getSelectedValue();
                if (selectedRoom != null && !selectedRoom.equals(currentRoom)) {
                    out.println("/room " + selectedRoom);
                    currentRoom = selectedRoom; // Update current room
                    chatArea.append("Switched to room: " + selectedRoom + "\n");
                }
            }
        });

        sidePanel.add(userPanel);
        sidePanel.add(roomPanel);
        mainPanel.add(sidePanel, BorderLayout.EAST);

        // Menu bar
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Actions");
        JMenuItem connectItem = new JMenuItem("Reconnect");
        JMenuItem exitItem = new JMenuItem("Exit");

        connectItem.addActionListener(e -> reconnect());
        exitItem.addActionListener(e -> System.exit(0));

        menu.add(connectItem);
        menu.addSeparator();
        menu.add(exitItem);
        menuBar.add(menu);
        setJMenuBar(menuBar);

        add(mainPanel);
        setVisible(true);
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Get username
            username = JOptionPane.showInputDialog(this, "Enter your username:");
            if (username == null || username.trim().isEmpty()) {
                username = "Guest" + (int) (Math.random() * 1000);
            }
            out.println(username);

            // Start message reader thread
            new Thread(this::readMessages).start();

            // Request initial user and room lists
            out.println("/users");
            out.println("/rooms");

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Could not connect to server: " + e.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void readMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                final String msg = message;
                SwingUtilities.invokeLater(() -> processServerMessage(msg));
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                chatArea.append("\n Disconnected from server\n");
                inputField.setEnabled(false);
            });
        }
    }

    private void processServerMessage(String message) {
        if (message.startsWith("[USERLIST]")) {
            updateUserList(message.substring(10));
        } else if (message.startsWith("[ROOMLIST]")) {
            updateRoomList(message.substring(10));
        } else if (message.startsWith("[ROOMCHANGE]")) {
            currentRoom = message.substring(12);
            chatArea.append("Joined room: " + currentRoom + "\n");
        } else {
            chatArea.append(message + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        }
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
        String message = inputField.getText();
        if (!message.trim().isEmpty()) {
            out.println(message);
            inputField.setText("");
        }
    }

    private void reconnect() {
        try {
            if (socket != null)
                socket.close();
            if (in != null)
                in.close();
            if (out != null)
                out.close();

            connectToServer();
            inputField.setEnabled(true);
            chatArea.append("\nReconnected to server\n");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Reconnect failed: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient());
    }
}