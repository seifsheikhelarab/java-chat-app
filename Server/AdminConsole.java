package Server;

import java.util.Scanner;

public class AdminConsole {
    private final Server server;
    private final Scanner scanner;

    public AdminConsole(Server server) {
        this.server = server;
        this.scanner = new Scanner(System.in);
        start();
    }

    private void start() {
        System.out.println("\n=== ADMIN CONSOLE ===");
        System.out.println("Commands: /stop, /list, /kick <user>, /help");
        
        while (server.isRunning()) {
            System.out.print("admin> ");
            String command = scanner.nextLine().trim();
            
            if (command.startsWith("/")) {
                handleCommand(command);
            } else if (!command.isEmpty()) {
                System.out.println("Unknown command. Type /help for commands");
            }
        }
        scanner.close();
    }

    private void handleCommand(String command) {
        String[] parts = command.split(" ");
        switch (parts[0].toLowerCase()) {
            case "/stop":
                server.shutdown();
                break;
                
            case "/list":
                server.listUsers();
                break;
                
            case "/kick":
                if (parts.length > 1) {
                    server.kickUser(parts[1]);
                } else {
                    System.out.println("Usage: /kick <username>");
                }
                break;
                
            case "/help":
                printHelp();
                break;
                
            default:
                System.out.println("Unknown command. Type /help for commands");
        }
    }

    private void printHelp() {
        System.out.println("\nADMIN COMMANDS:");
        System.out.println("/stop       - Shut down the server");
        System.out.println("/list       - Show connected users");
        System.out.println("/kick <user> - Kick a user from server");
        System.out.println("/help       - Show this help");
    }
}