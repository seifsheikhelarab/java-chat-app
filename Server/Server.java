package Server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    private static final int PORT = 12345;
    public static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Set<ClientHandler>> rooms = new ConcurrentHashMap<>();
    private static ServerSocket serverSocket;
    private static boolean isRunning = true;

    public static void main(String[] args) {
        initializeServer();
    }

    private static void initializeServer() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);

            // Create default rooms
            createRoom("lobby");
            createRoom("general");
            createRoom("admin");

            // Start admin console thread
            new Thread(Server::adminConsole).start();

            // Accept client connections
            while (isRunning) {
                try {
                    Socket socket = serverSocket.accept();
                    ClientHandler clientHandler = new ClientHandler(socket);
                    clientHandler.start();
                } catch (SocketException e) {
                    if (isRunning) {
                        System.out.println("Socket error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println(" Could not start server: " + e.getMessage());
        }
    }

    private static void adminConsole() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("\nAdmin Console Activated");
            System.out.println("Commands: /stop, /list, /kick <user>, /ban <user>, /rooms, /delroom <room>");

            while (isRunning) {
                System.out.print("\nadmin> ");
                String command = scanner.nextLine();

                if (command.startsWith("/")) {
                    handleAdminCommand(command);
                } else {
                    System.out.println(" Unknown command. Type /help for commands");
                }
            }
        }
    }

    private static void handleAdminCommand(String command) {
        String[] parts = command.split(" ");
        switch (parts[0].toLowerCase()) {
            case "/stop":
                shutdownServer();
                break;

            case "/list":
                listUsers();
                break;

            case "/kick":
                if (parts.length > 1)
                    kickUser(parts[1]);
                else
                    System.out.println("Usage: /kick <username>");
                break;

            case "/ban":
                if (parts.length > 1)
                    banUser(parts[1]);
                else
                    System.out.println("Usage: /ban <username>");
                break;

            case "/rooms":
                listRooms();
                break;

            case "/delroom":
                if (parts.length > 1)
                    deleteRoom(parts[1]);
                else
                    System.out.println("Usage: /delroom <roomname>");
                break;

            case "/help":
                printAdminHelp();
                break;

            default:
                System.out.println(" Unknown command");
        }
    }

    private static void printAdminHelp() {
        System.out.println("\nADMIN COMMANDS:");
        System.out.println("/stop - Shutdown server");
        System.out.println("/list - List all users");
        System.out.println("/kick <user> - Disconnect user");
        System.out.println("/ban <user> - Ban user (TODO: Implement persistence)");
        System.out.println("/rooms - List all rooms");
        System.out.println("/delroom <room> - Delete a room (users moved to lobby)");
        System.out.println("/help - Show this help");
    }

    public static synchronized boolean createRoom(String roomName) {
        if (rooms.containsKey(roomName)) {
            return false;
        }
        rooms.put(roomName, ConcurrentHashMap.newKeySet());
        System.out.println(" Created room: " + roomName);
        return true;
    }

    private static void deleteRoom(String roomName) {
        if (roomName.equalsIgnoreCase("lobby")) {
            System.out.println(" Cannot delete the lobby");
            return;
        }

        Set<ClientHandler> roomClients = rooms.get(roomName);
        if (roomClients != null) {
            // Move all users to lobby
            for (ClientHandler client : roomClients) {
                client.changeRoom("lobby");
                client.sendMessage("[ADMIN] Your room was deleted. Moved to lobby.");
            }
            rooms.remove(roomName);
            System.out.println(" Deleted room: " + roomName);
            broadcastSystemMessage("Room '" + roomName + "' was deleted by admin");
        } else {
            System.out.println(" Room not found: " + roomName);
        }
    }

    private static void kickUser(String username) {
        ClientHandler client = clients.get(username.toLowerCase());
        if (client != null) {
            client.sendMessage("[ADMIN] You have been kicked by an admin");
            client.forceDisconnect();
            System.out.println(" Kicked user: " + username);
            broadcastSystemMessage(username + " was kicked by admin");
        } else {
            System.out.println(" User not found: " + username);
        }
    }

    private static void banUser(String username) {
        kickUser(username);
        System.out.println("Ban not yet persisted (TODO)");
    }

    public static String getUserList() {
        return String.join(",", clients.keySet());
    }

    public static String getRoomList() {
        return String.join(",", rooms.keySet());
    }

    private static void listUsers() {
        System.out.println("\nOnline Users (" + clients.size() + "):");
        clients.forEach((name, client) -> System.out.printf("- %-15s (Room: %-10s IP: %s)%n",
                client.getUsername(),
                client.getCurrentRoom(),
                client.getSocket().getInetAddress()));
    }

    private static void listRooms() {
        System.out.println("\nActive Rooms (" + rooms.size() + "):");
        rooms.forEach((name, members) -> System.out.printf("- %-15s (%d users)%n", name, members.size()));
    }

    private static void shutdownServer() {
        System.out.println(" Initiating server shutdown...");
        isRunning = false;
        broadcastSystemMessage("Server is shutting down NOW. Goodbye!");

        // Close all client connections
        for (ClientHandler client : clients.values()) {
            client.forceDisconnect();
        }

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing server socket: " + e.getMessage());
        }

        System.out.println(" Server shutdown complete");
        System.exit(0);
    }

    public static void broadcastSystemMessage(String message) {
        System.out.println("Broadcast " + message);
        for (ClientHandler client : clients.values()) {
            client.sendMessage("[SYSTEM] " + message);
        }
    }

    public static void broadcastRoomMessage(String message, String roomName, ClientHandler sender) {
        Set<ClientHandler> roomClients = rooms.get(roomName);
        if (roomClients != null) {
            String formattedMessage = String.format("[%s] %s: %s", roomName, sender.getUsername(), message);
            System.out.println(formattedMessage);

            for (ClientHandler client : roomClients) {
                client.sendMessage(formattedMessage);
            }
        }
    }
}