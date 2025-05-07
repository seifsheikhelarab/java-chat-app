package Server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    public static final int PORT = 12345;
    public static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Set<ClientHandler>> rooms = new ConcurrentHashMap<>();
    private static ServerSocket serverSocket;
    public static volatile boolean isRunning = true; 

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

            // Start admin console in separate thread
            new Thread(() -> new AdminConsole()).start();

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
            System.out.println("Could not start server: " + e.getMessage());
        }
    }

    public static synchronized boolean createRoom(String roomName) {
        if (rooms.containsKey(roomName)) {
            return false;
        }
        rooms.put(roomName, ConcurrentHashMap.newKeySet());
        System.out.println("Created room: " + roomName);
        return true;
    }

    public static void broadcastSystemMessage(String message) {
        System.out.println("Broadcast: " + message);
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

    public static void shutdownServer() {
        System.out.println("Initiating server shutdown...");
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

        System.out.println("Server shutdown complete");
        System.exit(0);
    }

    public static String getUserList() {
        return String.join(",", clients.keySet());
    }

    public static String getRoomList() {
        return String.join(",", rooms.keySet());
    }
}