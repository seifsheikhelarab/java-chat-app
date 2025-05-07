package Server;

import java.io.IOException;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AdminConsole {
    public AdminConsole() {
        startAdminConsole();
    }

    private void startAdminConsole() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("\nAdmin Console Activated");
            System.out.println("Commands: /stop, /list, /kick <user>, /ban <user>, /rooms, /delroom <room>");

            while (Server.isRunning) {
                System.out.print("\nadmin> ");
                String command = scanner.nextLine();

                if (command.startsWith("/")) {
                    handleAdminCommand(command);
                } else {
                    System.out.println("Unknown command. Type /help for commands");
                }
            }
        }
    }

    private void handleAdminCommand(String command) {
        String[] parts = command.split(" ");
        switch (parts[0].toLowerCase()) {
            case "/stop":
                Server.shutdownServer();
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
                System.out.println("Unknown command");
        }
    }

    private void printAdminHelp() {
        System.out.println("\nADMIN COMMANDS:");
        System.out.println("/stop - Shutdown server");
        System.out.println("/list - List all users");
        System.out.println("/kick <user> - Disconnect user");
        System.out.println("/ban <user> - Ban user");
        System.out.println("/rooms - List all rooms");
        System.out.println("/delroom <room> - Delete a room (users moved to lobby)");
        System.out.println("/help - Show this help");
    }

    private void deleteRoom(String roomName) {
        if (roomName.equalsIgnoreCase("lobby")) {
            System.out.println("Cannot delete the lobby");
            return;
        }

        Set<ClientHandler> roomClients = Server.rooms.get(roomName);
        if (roomClients != null) {
            // Move all users to lobby
            for (ClientHandler client : roomClients) {
                client.changeRoom("lobby");
                client.sendMessage("[ADMIN] Your room was deleted. Moved to lobby.");
            }
            Server.rooms.remove(roomName);
            System.out.println("Deleted room: " + roomName);
            Server.broadcastSystemMessage("Room '" + roomName + "' was deleted by admin");
        } else {
            System.out.println("Room not found: " + roomName);
        }
    }

    private void kickUser(String username) {
        ClientHandler client = Server.clients.get(username.toLowerCase());
        if (client != null) {
            client.sendMessage("[ADMIN] You have been kicked by an admin");
            client.forceDisconnect();
            System.out.println("Kicked user: " + username);
            Server.broadcastSystemMessage(username + " was kicked by admin");
        } else {
            System.out.println("User not found: " + username);
        }
    }

    private void banUser(String username) {
        kickUser(username);
        System.out.println("Ban not yet persisted (TODO)");
    }

    private void listUsers() {
        System.out.println("\nOnline Users (" + Server.clients.size() + "):");
        Server.clients.forEach((name, client) -> 
            System.out.printf("- %-15s (Room: %-10s IP: %s)%n",
                client.getUsername(),
                client.getCurrentRoom(),
                client.getSocket().getInetAddress()));
    }

    private void listRooms() {
        System.out.println("\nActive Rooms (" + Server.rooms.size() + "):");
        Server.rooms.forEach((name, members) -> 
            System.out.printf("- %-15s (%d users)%n", name, members.size()));
    }
}