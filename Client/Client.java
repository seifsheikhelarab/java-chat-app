package Client;

import java.io.*;
import java.net.*;

public class Client {
    private static String username;

    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost", 12345);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

        // Get username from server and send it
        System.out.print(in.readLine() + " ");
        username = userInput.readLine();
        out.println(username);

        // Start a thread to listen for messages from server
        Thread receiveThread = new Thread(() -> {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println(serverMessage);
                }
            } catch (IOException e) {
                System.out.println("Disconnected from server.");
            }
        });
        receiveThread.start();

        // Main thread handles sending messages
        String userMessage;
        while ((userMessage = userInput.readLine()) != null) {
            if (userMessage.equalsIgnoreCase("/quit")) {
                out.println("/quit");
                break;
            }
            out.println(userMessage);
        }

        socket.close();
        System.exit(0);
    }
}