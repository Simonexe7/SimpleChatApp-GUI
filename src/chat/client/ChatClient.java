package chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import GUI.ClientUI;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private ClientUI ui;

    public ChatClient(String username, ClientUI ui) {
        this.ui = ui;
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(username);

            listenForMessages();

        } catch (IOException e) {
            ui.showError("Failed to connect: " + e.getMessage());
        }
    }

    private void listenForMessages() {
        Thread t = new Thread(() -> {
            try {
                String fromServer;
                while ((fromServer = in.readLine()) != null) {
                    if (fromServer.startsWith("USERS:")) {
                        String[] users = fromServer.substring(6).split(",");
                        ui.updateUserList(users);
                    } else {
                        ui.appendMessage(fromServer);
                    }
                }
            } catch (IOException e) {
                ui.showError("Connection closed.");
            }
        });
        t.start();
    }
    
    public void sendMessage(String msg) {
        if (out != null) {
            out.println(msg);
        }
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            // TODO: handle exception
        }
    }
}