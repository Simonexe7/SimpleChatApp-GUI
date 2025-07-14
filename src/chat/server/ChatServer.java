import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;
    private static Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();
    private static Map<String, ClientHandler> clientMap = new ConcurrentHashMap<>();
    private static Map<String, Set<ClientHandler>> roomMap = new ConcurrentHashMap<>();
    private static Map<String, LinkedList<String>> roomHistory = new ConcurrentHashMap<>();
    private static PrintWriter logWriter;

    private static void log(String message) {
        String timestamp = "["+ java.time.LocalDateTime.now() +"]";
        logWriter.println(timestamp + message);
    }

    public static void main(String[] args) throws IOException {
        logWriter = new PrintWriter(new FileWriter("chat-server.log", true), true);
        System.out.println("[SERVER]: Server started on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[SERVER]: New client connected: " + clientSocket);
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

        roomHistory.putIfAbsent(room, new LinkedList<>());
        LinkedList<String> history = roomHistory.get(room);
        synchronized (history) {
            history.add(message);
            if (history.size() > 20) {
                history.removeFirst();
            }
        }

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
            target.sendMessage("[PM from "+ sender.clientName +"]: " + message);
            sender.sendMessage("[PM to "+ targetName +"]: " + message);
        } else {
            sender.sendMessage("(Err) User " + targetName + " not found.");
        }
    }

    private static String timestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return "["+LocalTime.now().format(formatter)+"] ";
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

        void broadcastUserListToRoom() {
            Set<ClientHandler> clientsInRoom = roomMap.get(currentRoom);
            if (clientsInRoom == null) return;

            StringBuilder sb = new StringBuilder("USERS:");
            for (ClientHandler c : clientsInRoom) {
                sb.append(c.clientName).append(",");
            }
            String userListMsg = sb.toString();

            for (ClientHandler c : clientsInRoom) {
                c.sendMessage(userListMsg);
            }
        }

        void listRoomMembers() {
            Set<ClientHandler> clientsInRoom = roomMap.get(currentRoom);
            StringBuilder sb = new StringBuilder("[SERVER]: Members in "+ currentRoom + ": ");
            for (ClientHandler c : clientsInRoom) {
                sb.append(" ").append(c.clientName);
            }
            sendMessage(sb.toString()+"");
        }

        void joinRoom(String newRoom) {
            LinkedList<String> history = ChatServer.roomHistory.get(newRoom);
            if (history != null) {
                sendMessage("======== Last 20 messages in " + newRoom + " =======");
                for (String msg : history) {
                    sendMessage(msg);
                }
                sendMessage("========== End of history ========");
            }
            if (newRoom.isEmpty()) {
                sendMessage("(Err) Room name cannot be empty.");
                return;
            } 

            if (currentRoom != null) {
                Set<ClientHandler> oldRoomClients = ChatServer.roomMap.get(currentRoom);
                if (oldRoomClients != null) {
                    oldRoomClients.remove(this);
                    ChatServer.broadcastToRoom("<--- "+clientName + " has left the room.", this, currentRoom);
                    broadcastUserListToRoom();
                }
            }

            boolean roomExists = ChatServer.roomMap.containsKey(newRoom);

            currentRoom = newRoom;

            ChatServer.roomMap.putIfAbsent(newRoom, ConcurrentHashMap.newKeySet());
            ChatServer.roomMap.get(newRoom).add(this);

            if (roomExists) {
                sendMessage("--> Joined existing room \""+newRoom+"\".");
            } else {
                sendMessage("--> Room \""+newRoom+"\" created and joined.");
            }
        }

        void listAllRooms() {
            StringBuilder sb = new StringBuilder("[SERVER]: Available rooms: ");
            for (Map.Entry<String, Set<ClientHandler>> entry : ChatServer.roomMap.entrySet()) {
                String roomName = entry.getKey();
                int memberCount = entry.getValue().size();
                sb.append("\n- ").append(roomName).append(" (").append(memberCount).append(")");
            }
            sendMessage(sb.toString());
        }

        void leaveRoom() {
            if ("Lobby".equals(currentRoom)) {
                sendMessage("[SERVER]: You are already in the lobby.");
                return;
            }

            Set<ClientHandler> oldRoomClients = ChatServer.roomMap.get(currentRoom);
            if (oldRoomClients != null) {
                oldRoomClients.remove(this);
                ChatServer.broadcastToRoom("<-- "+clientName + " has left the room", this, currentRoom);
                broadcastUserListToRoom();
            }

            currentRoom = "Lobby";
            ChatServer.roomMap.putIfAbsent("Lobby", ConcurrentHashMap.newKeySet());
            ChatServer.roomMap.get("Lobby").add(this);

            sendMessage("[SERVER]: You have left the room and returned to Lobby.");
            ChatServer.broadcastToRoom("--> "+clientName + " has joined the room.", this, currentRoom);
        }

        public void run() {
            try {
                String time = timestamp();
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                this.clientName = in.readLine();
                clientMap.put(clientName, this);

                if (currentRoom == null) {
                    currentRoom = "Lobby";
                }
                roomMap.putIfAbsent(currentRoom, ConcurrentHashMap.newKeySet());
                roomMap.get(currentRoom).add(this);

                sendMessage("--> You joined room: Lobby");
                broadcastToRoom("--> " + clientName + " has joined the chat", this, currentRoom);
                log("--> " + clientName + " has joined the chat");

                broadcastUserListToRoom();

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("@")) {
                        int spaceIdx = message.indexOf(" ");
                        if (spaceIdx != -1) {
                            String targetName = message.substring(1, spaceIdx);
                            String privateMsg = message.substring(spaceIdx + 1);
                            privateMessage(targetName, privateMsg, this);
                        } else {
                            out.println("(Err) Invalid private message format. Use: @username message");
                        }
                    } else if (message.equals("/list")) {
                        listRoomMembers();
                    } else if (message.startsWith("/join ")) {
                        String newRoom = message.substring(6).trim();

                        if (newRoom.equals(currentRoom)) {
                            sendMessage("[SERVER]: You are currently in that room.");
                            continue;
                        }
                        joinRoom(newRoom);
                        broadcastToRoom("--> " + clientName + " has joined the chat", this, newRoom);
                        broadcastUserListToRoom();
                        continue;
                    } else if (message.equals("/rooms")) {
                        listAllRooms();
                    } else if (message.equals("/leave")) {
                        leaveRoom();
                        broadcastUserListToRoom();
                    } else {
                        if (currentRoom == null) {
                            sendMessage("[SERVER]: You are not in a room. Use /join roomName to join one.");
                            continue;
                        }
                        System.out.println(time + "["+ clientName +"]: " + message);
                        broadcastToRoom(time + "["+ clientName +"]: "+ message, this, currentRoom);
                        broadcastUserListToRoom();
                        log("["+ clientName +"]: "+ message);
                    }

                }
            } catch (IOException e) {
                System.out.println("[SERVER]: Client disconnected: " + socket);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                removeClient(this);
                clientMap.remove(this.clientName);
                broadcastToRoom("<-- "+clientName+ " has left the chat", this, currentRoom);
                broadcastUserListToRoom();
                log("<-- "+clientName+ " has left the chat");
                logWriter.close();
            }
        }

        void sendMessage(String message) {
            out.println(message);
        }
    }
}
