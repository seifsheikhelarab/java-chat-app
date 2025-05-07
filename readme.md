# Multi-Client Chat Application  

## **Overview**  

A Java-based **multi-client chat application** built using **socket programming**, **object-oriented principles (OOP)**, and **multi-threading**. The server handles multiple clients simultaneously, allowing users to communicate in a group chat environment with unique usernames.  

---

## **Key Technologies & Concepts**  

### **1. Socket Programming (TCP/IP Communication)**  

- **ServerSocket** listens for incoming client connections on a specified port (`12345`).  
- **Socket** establishes a two-way communication channel between the server and clients.  
- **BufferedReader** and **PrintWriter** handle text-based I/O streams for sending and receiving messages.  
- **Connection Lifecycle**:  
  - Server waits for clients (`serverSocket.accept()`).  
  - Clients connect (`new Socket("localhost", 12345)`).  
  - Messages are exchanged via input/output streams.  
  - Clean disconnection using `/quit` command.  

### **2. Object-Oriented Programming (OOP)**  

- **Encapsulation**:  
  - The `ClientHandler` class encapsulates client-specific logic (username, I/O streams).  
  - Private fields with controlled access (e.g., `socket`, `username`).  
- **Separation of Concerns**:  
  - `Server` class manages connections and broadcasting.  
  - `Client` class handles user input and server responses.  
- **Reusability**:  
  - The `ClientHandler` thread can be reused for each new client connection.  

### **3. Multi-Threading (Concurrent Client Handling)**  

- **Server-Side Threading**:  
  - Each client runs in a separate `ClientHandler` thread.  
  - The main thread continues listening for new connections.  
- **Client-Side Threading**:  
  - **Receiving Thread**: Continuously listens for incoming messages.  
  - **Sending Thread**: Handles user input and message transmission.  
- **Thread Safety**:  
  - `synchronizedSet` ensures safe access to the connected clients list.  
  - `synchronized` blocks prevent race conditions during broadcasts.  

---

## **Features**  

- **Multi-client support** (handles many users at once)  
- **Username-based identification** (no anonymous messaging)  
- **Real-time group chat** (messages broadcast to all)  
- **Graceful disconnections** (`/quit` command)  
- **Join/leave notifications** (automatic alerts)  

---

## **How It Works**  

### **Server Workflow**  

1. Starts and waits for clients (`ServerSocket.accept()`).  
2. For each new client:  
   - Creates a `ClientHandler` thread.  
   - Prompts for a username.  
   - Adds client to the active connections list.  
3. Broadcasts messages to all clients (except sender).  
4. Removes disconnected clients and notifies others.  

### **Client Workflow**  

1. Connects to the server (`Socket("localhost", 12345)`).  
2. Enters a username (sent to the server).  
3. Runs two threads:  
   - **Receiver**: Listens for messages from the server.  
   - **Sender**: Reads user input and sends messages.  
4. Disconnects cleanly with `/quit`.  

---

## **Project Structure**  

```
chat-app/  
├── Client/  
│   └── Client.java       # Manages client-side connection & messaging  
├── Server/  
│   ├── Server.java       # Main server logic  
│   └── ClientHandler.java # Thread for each client (if split into separate file)  
├── build.bat
├── runClient.bat
├── runServer.bat
└── README.md  
```

---

## **Conclusion**  

This project demonstrates **socket programming** for network communication, **OOP** for clean architecture, and **multi-threading** for handling concurrent clients. It serves as a foundation for building more advanced chat systems with additional features.  
