package Server;

import java.io.*;
import java.net.*;

public class ClientHandler extends Thread {
    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private final Server server;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Get username
            out.println("Enter your username:");
            username = in.readLine();
            if (username == null) return;
            System.out.println("[SERVER] " + username + " connected from " + socket.getInetAddress());
            server.broadcast(username + " joined the chat", this);

            // Handle messages
            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("/quit")) {
                    break;
                }
                String formattedMessage = username + ": " + message;
                System.out.println("[CHAT] " + formattedMessage);
                server.broadcast(formattedMessage, this);
            }
        } catch (IOException e) {
            System.out.println("[ERROR] Client " + username + ": " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("[ERROR] Closing socket: " + e.getMessage());
            }
            server.removeClient(this);
            System.out.println("[SERVER] " + username + " disconnected");
            server.broadcast(username + " left the chat", null);
        }
    }
    

    public synchronized void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void forceDisconnect() {
        try {
            if (out != null) {
                out.println("[SERVER] You have been disconnected");
                out.flush();
            }
            socket.close();
        } catch (IOException e) {
            System.out.println("Error forcing disconnect: " + e.getMessage());
        }
    }

    public String getUsername() {
        return username;
    }

    public Socket getSocket() {
        return socket;
    }

    public boolean isConnected() {
        return !socket.isClosed();
    }
}