package chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (
            Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));    
        ) {
            while (true) {
                System.out.print("\u001B[33mEnter your name: \u001B[36m");
                String name = stdIn.readLine();
                if (!name.isEmpty()) {
                    out.println("\u001B[36m"+name+"\u001B[0m");
                    break;
                } else {
                    System.out.println("\u001B[31m[SERVER]: You must enter your name!\u001B[0m");
                }
            }

            System.out.println("\u001B[33mConnected to chat server!\u001B[0m");

            Thread readerThread = new Thread(() -> {
                String fromServer;

                try {
                    while ((fromServer = in.readLine()) != null) {
                        System.out.println("Message: " + fromServer);
                    }
                } catch (IOException e) {
                    System.out.println("\u001B[31m[SERVER]: Connection closed.\u001B[0m");
                }
            });
            readerThread.start();

            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput);
            }
        } catch (IOException e) { 
            e.printStackTrace();
        }
    }
}