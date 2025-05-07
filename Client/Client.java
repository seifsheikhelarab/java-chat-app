package Client;

import java.io.*;
import java.net.*;

public class Client {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public static void main(String[] args) {
        new Client().startClient();
    }

    public void startClient() {
        try {
            socket = new Socket("localhost", 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Thread for receiving messages
            new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server");
                }
            }).start();

            // Handle user input
            BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
            
            // Read username prompt
            System.out.println(in.readLine());
            username = consoleReader.readLine();
            out.println(username);

            // Message sending loop
            String message;
            while ((message = consoleReader.readLine()) != null) {
                out.println(message);
                if (message.equalsIgnoreCase("/quit")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
    }
}