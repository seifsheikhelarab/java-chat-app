package Server;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 12345;
    private static Set<ClientHandler> clients = Collections.synchronizedSet(new HashSet<>());
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started. Waiting for clients...");

            // Add shutdown hook to close server socket on termination
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("Shutting down server...");
                    serverSocket.close();
                    System.out.println("Server socket closed.");
                } catch (IOException e) {
                    System.out.println("Error closing server socket: " + e.getMessage());
                }
            }));

            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("New client connected: " + socket);
                    
                    ClientHandler clientHandler = new ClientHandler(socket);
                    clients.add(clientHandler);
                    clientHandler.start();
                } catch (SocketException e) {
                    if (!serverSocket.isClosed()) {
                        System.out.println("Socket exception: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Could not start server: " + e.getMessage());
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    System.out.println("Error closing server socket: " + e.getMessage());
                }
            }
        }
    }

    public static void broadcast(String message, ClientHandler excludeClient) {
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != excludeClient && client.isConnected()) {
                    client.sendMessage(message);
                }
            }
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public boolean isConnected() {
            return socket != null && !socket.isClosed();
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Get username from client
                out.println("Enter your username:");
                username = in.readLine();
                System.out.println(username + " has joined the chat.");
                broadcast(username + " has joined the chat.", this);

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.equalsIgnoreCase("/quit")) {
                        break;
                    }
                    String message = username + ": " + inputLine;
                    System.out.println(message);
                    broadcast(message, this);
                }

                // Client disconnected
                System.out.println(username + " has left the chat.");
                broadcast(username + " has left the chat.", this);
            } catch (IOException e) {
                System.out.println("Error handling client " + username + ": " + e.getMessage());
            } finally {
                try {
                    if (socket != null) socket.close();
                } catch (IOException e) {
                    System.out.println("Couldn't close client socket: " + e.getMessage());
                }
                clients.remove(this);
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }
    }
}