package Client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final int RECONNECT_DELAY = 5000;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            try {
                System.out.println("🔌 Connecting to server...");
                connectToServer(scanner);
                System.out.println(" Disconnected from server. Attempting to reconnect in " +
                        (RECONNECT_DELAY / 1000) + " seconds...");
                Thread.sleep(RECONNECT_DELAY);
            } catch (InterruptedException e) {
                System.out.println("  Reconnect interrupted");
                break;
            }
        }
    }

    private static void connectToServer(Scanner scanner) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            System.out.println(" Connected to server!");

            // Start message reader thread
            Thread readerThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    System.out.println("  Lost connection to server");
                }
            });
            readerThread.start();

            // Handle user input
            while (true) {
                String input = scanner.nextLine();
                out.println(input);

                if (input.equalsIgnoreCase("/quit")) {
                    System.out.println("👋 Disconnecting...");
                    break;
                }
            }

            // Wait for reader thread to finish
            readerThread.join(1000);
        } catch (ConnectException e) {
            System.out.println(" Could not connect to server. Is it running?");
        } catch (IOException e) {
            System.out.println("  Connection error: " + e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("  Thread interrupted");
        }
    }
}