package Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private ServerSocket serverSocket;
    private volatile boolean isRunning = true;

    public static void main(String[] args) {
        Server server = new Server();
        
        // Start server in background thread
        new Thread(server::start).start();
        
        // Start admin console in main thread
        new AdminConsole(server);
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
            System.out.println("Waiting for clients...");

            while (isRunning) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("New client connected: " + socket.getInetAddress());
                    
                    ClientHandler clientHandler = new ClientHandler(socket, this);
                    clients.add(clientHandler);
                    clientHandler.start();
                } catch (SocketException e) {
                    if (isRunning) {
                        System.out.println("Socket error: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    public synchronized void shutdown() {
        if (!isRunning) return;
        isRunning = false;
        
        System.out.println("Shutting down server...");
        broadcast("[SERVER] Server is shutting down", null);
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing server: " + e.getMessage());
        }
        
        // Disconnect all clients
        synchronized (clients) {
            for (ClientHandler client : clients) {
                client.forceDisconnect();
            }
            clients.clear();
        }
        System.out.println("Server shutdown complete");
    }

    public synchronized void broadcast(String message, ClientHandler exclude) {
        System.out.println("[BROADCAST] " + message); 
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != exclude && client.isConnected()) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println(client.getUsername() + " disconnected");
    }

    public synchronized void listUsers() {
        System.out.println("\nConnected Users (" + clients.size() + "):");
        synchronized (clients) {
            for (ClientHandler client : clients) {
                System.out.printf("- %-15s (%s)%n", 
                    client.getUsername(), 
                    client.getSocket().getInetAddress());
            }
        }
    }

    public synchronized void kickUser(String username) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.getUsername().equalsIgnoreCase(username)) {
                    client.sendMessage("[ADMIN] You have been kicked from the server");
                    client.forceDisconnect();
                    System.out.println("Kicked user: " + username);
                    return;
                }
            }
        }
        System.out.println("User not found: " + username);
    }

    public boolean isRunning() {
        return isRunning;
    }
}