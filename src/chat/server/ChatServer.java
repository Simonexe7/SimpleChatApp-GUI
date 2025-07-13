import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();
    private static Map<String, ClientHandler> clientMap = new ConcurrentHashMap<>();
    private static Map<String, Set<ClientHandler>> roomMap = new ConcurrentHashMap<>();
    private static PrintWriter logWriter;

    private static void log(String message) {
        String timestamp = "["+ java.time.LocalDateTime.now() +"]";
        logWriter.println(timestamp + message);
    }

    public static void main(String[] args) throws IOException {
        logWriter = new PrintWriter(new FileWriter("chat-server.log", true), true);
        System.out.println("\u001B[33m[SERVER]: Server started on port " + PORT + "\u001B[0m");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\u001B[33m[SERVER]: New client connected: " + clientSocket + "\u001B[0m");
                ClientHandler handler = new ClientHandler(clientSocket);
                clientHandlers.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void broadcastToRoom(String message, ClientHandler sender, String room) {
        Set<ClientHandler> clientsInRoom = roomMap.get(room);
        if (clientsInRoom == null) return;

        for (ClientHandler client : clientsInRoom) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    static void privateMessage(String targetName, String message, ClientHandler sender) {
        ClientHandler target = clientMap.get(targetName);
        for (String name : clientMap.keySet()) {
            if (name.equalsIgnoreCase(targetName)) {
                target = clientMap.get(name);
                break;
            }
        }

        if (target != null) {
            target.sendMessage("\u001B[35m[PM from "+ sender.clientName +"]: " + message + "\u001B[0m");
            sender.sendMessage("\u001B[35m[PM to "+ targetName +"]: " + message + "\u001B[0m");
        } else {
            sender.sendMessage("\u001B[31mUser " + targetName + " not found.\u001B[0m");
        }

        System.out.println("clientMap keys: " + clientMap.keySet());
        System.out.println("targetName: '" + targetName + "'");

    }

    static void removeClient(ClientHandler client) {
        clientHandlers.remove(client);
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientName;
        private String currentRoom;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        void listRoomMembers() {
            Set<ClientHandler> clientsInRoom = roomMap.get(currentRoom);
            StringBuilder sb = new StringBuilder("\u001B[33m[SERVER]: Members in\u001B[0m \u001B[32m"+ currentRoom + "\u001B[0m: \u001B[36m");
            for (ClientHandler c : clientsInRoom) {
                sb.append(" ").append(c.clientName);
            }
            sendMessage(sb.toString()+"\u001B[0m");
        }

        void joinRoom(String newRoom) {
            if (newRoom.isEmpty()) {
                sendMessage("\u001B[31mRoom name cannot be empty.\u001B[0m");
                return;
            } 

            if (currentRoom != null) {
                Set<ClientHandler> oldRoomClients = ChatServer.roomMap.get(currentRoom);
                if (oldRoomClients != null) {
                    oldRoomClients.remove(this);
                    ChatServer.broadcastToRoom("\u001B[33m<--- \u001B[36m"+clientName + " \u001B[33mhas left the room.\u001B[0m", this, currentRoom);
                }
            }

            boolean roomExists = ChatServer.roomMap.containsKey(newRoom);

            currentRoom = newRoom;

            ChatServer.roomMap.putIfAbsent(newRoom, ConcurrentHashMap.newKeySet());
            ChatServer.roomMap.get(newRoom).add(this);

            if (roomExists) {
                sendMessage("\u001B[33m--> Joined existing room \u001B[32m\""+newRoom+"\".\u001B[0m");
            } else {
                sendMessage("\u001B[33m--> Room \u001B[32m\""+newRoom+"\" created and joined.\u001B[0m");
            }
        }

        void listAllRooms() {
            StringBuilder sb = new StringBuilder("\u001B[33m[SERVER]: Available rooms: \u001B[0m");
            for (Map.Entry<String, Set<ClientHandler>> entry : ChatServer.roomMap.entrySet()) {
                String roomName = entry.getKey();
                int memberCount = entry.getValue().size();
                sb.append("\n- \u001B[32m").append(roomName).append(" \u001B[33m(").append(memberCount).append(")\u001B[0m");
            }
            sendMessage(sb.toString());
        }

        void leaveRoom() {
            if ("Lobby".equals(currentRoom)) {
                sendMessage("\u001B[33m[SERVER]: You are already in the lobby.\u001B[0m");
                return;
            }

            Set<ClientHandler> oldRoomClients = ChatServer.roomMap.get(currentRoom);
            if (oldRoomClients != null) {
                oldRoomClients.remove(this);
                ChatServer.broadcastToRoom("\u001B[33m<-- \u001B[36m"+clientName + " \u001B[33mhas left the room\u001B[0m", this, currentRoom);
            }

            currentRoom = "Lobby";
            ChatServer.roomMap.putIfAbsent("Lobby", ConcurrentHashMap.newKeySet());
            ChatServer.roomMap.get("Lobby").add(this);

            sendMessage("\u001B[33m[SERVER]: You have left the room and returned to Lobby.\u001B[0m");
            ChatServer.broadcastToRoom("\u001B[33m--> \u001B[36m"+clientName + " \u001B[33mhas joined the room.\u001B[0m", this, currentRoom);
        }

        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                this.clientName = in.readLine();
                clientMap.put(clientName, this);

                if (currentRoom == null) {
                    currentRoom = "Lobby";
                }
                roomMap.putIfAbsent(currentRoom, ConcurrentHashMap.newKeySet());
                roomMap.get(currentRoom).add(this);

                sendMessage("\u001B[33m--> You joined room: \u001B[32mLobby\u001B[0m");
                broadcastToRoom("\u001B[33m--> \u001B[32m" + clientName + " \u001B[33mhas joined the chat\u001B[0m", this, currentRoom);
                log("--> " + clientName + " has joined the chat");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("@")) {
                        int spaceIdx = message.indexOf(" ");
                        if (spaceIdx != -1) {
                            String targetName = message.substring(1, spaceIdx);
                            String privateMsg = message.substring(spaceIdx + 1);
                            privateMessage(targetName, privateMsg, this);
                        } else {
                            out.println("\u001B[31mInvalid private message format. Use: @username message\u001B[0m");
                        }
                    } else if (message.equals("/list")) {
                        listRoomMembers();
                    } else if (message.startsWith("/join ")) {
                        String newRoom = message.substring(6).trim();

                        if (newRoom.equals(currentRoom)) {
                            sendMessage("\u001B[33m[SERVER]: You are currently in that room.\u001B[0m");
                            continue;
                        }
                        joinRoom(newRoom);
                        broadcastToRoom("\u001B[33m--> \u001B[36m" + clientName + " has joined the chat\u001B[0m", this, newRoom);
                        continue;
                    } else if (message.equals("/rooms")) {
                        listAllRooms();
                    } else if (message.equals("/leave")) {
                        leaveRoom();
                    } else {
                        if (currentRoom == null) {
                            sendMessage("\u001B[33m[SERVER]: You are not in a room. Use /join roomName to join one.\u001B[0m");
                            continue;
                        }
                        System.out.println("\u001B[36m["+ clientName +"]\u001B[0m: " + message);
                        broadcastToRoom("\u001B[36m["+ clientName +"]\u001B[0m: "+ message, this, currentRoom);
                        log("["+ clientName +"]: "+ message);
                    }

                }
            } catch (IOException e) {
                System.out.println("\u001B[33m[SERVER]: Client disconnected: " + socket + "\u001B[0m");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                removeClient(this);
                clientMap.remove(this.clientName);
                broadcastToRoom("\u001B[33m<-- \u001B[34m"+clientName+ " \u001B[33mhas left the chat\u001B[0m", this, currentRoom);
                log("<-- "+clientName+ " has left the chat");
                logWriter.close();
            }
        }

        void sendMessage(String message) {
            out.println(message);
        }
    }
}
