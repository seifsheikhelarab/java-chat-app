package Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ClientHandler extends Thread {
    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private String currentRoom = "lobby";
    private boolean isConnected = true;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            setupStreams();
            authenticateUser();
            handleClientCommunication();
        } catch (IOException e) {
            System.out.println("  " + (username != null ? username : "Unknown") + " disconnected unexpectedly");
        } finally {
            cleanup();
        }
    }

    private void setupStreams() throws IOException {
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void authenticateUser() throws IOException {
        sendWelcomeMessage();

        while (true) {
            out.println("🔑 Enter your username (3-12 characters, letters/numbers only):");
            username = in.readLine();

            if (username == null)
                throw new IOException("Connection closed");

            if (isValidUsername(username)) {
                if (Server.clients.putIfAbsent(username.toLowerCase(), this) == null) {
                    break;
                }
                out.println(" Username already taken. Please choose another.");
            } else {
                out.println(" Invalid username. Must be 3-12 alphanumeric characters.");
            }
        }

        joinRoom(currentRoom);
        Server.broadcastSystemMessage(username + " has joined the chat (Room: " + currentRoom + ")");
        sendHelpMessage();
    }

    private void handleClientCommunication() throws IOException {
        String input;
        while (isConnected && (input = in.readLine()) != null) {
            if (input.startsWith("/")) {
                handleCommand(input);
            } else {
                Server.broadcastRoomMessage(input, currentRoom, this);
            }
        }
    }

    private void handleCommand(String command) throws IOException {
        String[] parts = command.split(" ");
        switch (parts[0].toLowerCase()) {
            case "/quit":
                out.println("👋 Goodbye!");
                throw new IOException("Client quit");

            case "/help":
                sendHelpMessage();
                break;

            case "/room":
                handleRoomCommand(parts);
                out.println("[ROOMCHANGE]" + currentRoom); // Add this line
                break;

            case "/pm":
                handlePrivateMessage(parts);
                break;

            case "/users":
                out.println("[USERLIST]" + Server.getUserList());
                break;

            case "/rooms":
                out.println("[ROOMLIST]" + Server.getRoomList());
                break;

            default:
                out.println(" Unknown command. Type /help for available commands");
        }
    }

    private void handleRoomCommand(String[] parts) {
        if (parts.length < 2) {
            out.println("Usage: /room <roomname>");
            return;
        }

        String newRoom = parts[1];
        if (newRoom.equalsIgnoreCase(currentRoom)) {
            out.println("ℹ️  You're already in " + currentRoom);
            return;
        }

        // Create room if it doesn't exist
        if (!Server.rooms.containsKey(newRoom)) {
            Server.createRoom(newRoom);
            out.println(" Created new room: " + newRoom);
        }

        // Leave current room and join new one
        leaveRoom(currentRoom);
        currentRoom = newRoom;
        joinRoom(newRoom);
        out.println("🚪 Joined room: " + newRoom);
    }

    private void handlePrivateMessage(String[] parts) {
        if (parts.length < 3) {
            out.println("Usage: /pm <username> <message>");
            return;
        }

        String target = parts[1];
        String message = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));

        ClientHandler recipient = Server.clients.get(target.toLowerCase());
        if (recipient != null) {
            recipient.sendMessage(String.format("[PM from %s] %s", username, message));
            out.println(String.format("[PM to %s] %s", target, message));
        } else {
            out.println(" User not found: " + target);
        }
    }

    private void listUsers() {
        out.println("👥 Online users (" + Server.clients.size() + "):");
        Server.clients.forEach((name, client) -> out.println("- " + client.getUsername() +
                " (Room: " + client.getCurrentRoom() + ")"));
    }

    private void listRooms() {
        out.println("🚪 Available rooms (" + Server.rooms.size() + "):");
        Server.rooms.keySet()
                .forEach(room -> out.println("- " + room + " (" + Server.rooms.get(room).size() + " users)"));
    }

    private void sendWelcomeMessage() {
        out.println("\n🌟 Welcome to Java Chat! 🌟");
        out.println("=================================");
    }

    private void sendHelpMessage() {
        out.println("\n🛠  Available Commands:");
        out.println("/help - Show this help message");
        out.println("/quit - Exit the chat");
        out.println("/room <name> - Switch or create room");
        out.println("/pm <user> <msg> - Private message");
        out.println("/users - List online users");
        out.println("/rooms - List available rooms");
        out.println("=================================");
    }

    private boolean isValidUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9]{3,12}$");
    }

    public void changeRoom(String newRoom) {
        if (!newRoom.equals(currentRoom)) {
            leaveRoom(currentRoom);
            currentRoom = newRoom;
            joinRoom(newRoom);
            sendMessage("🚪 Joined room: " + newRoom);
        }
    }

    public Socket getSocket() {
        return socket;
    }

    private void joinRoom(String room) {
        Server.rooms.get(room).add(this);
    }

    private void leaveRoom(String room) {
        Server.rooms.get(room).remove(this);
        Server.broadcastRoomMessage(username + " left the room", room, this);
    }

    public void forceDisconnect() {
        try {
            isConnected = false;
            socket.close();
        } catch (IOException e) {
            System.out.println("  Error closing client socket: " + e.getMessage());
        }
    }

    private void cleanup() {
        if (username != null) {
            leaveRoom(currentRoom);
            Server.clients.remove(username.toLowerCase());
            Server.broadcastSystemMessage(username + " has left the chat");
        }
        forceDisconnect();
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String getUsername() {
        return username;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }
}