package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class MainClient {
    private static final String IP = "localhost";
    private static final int PORT = 5000;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Enter your nickname (max 10 chars): ");
        String nick = sc.nextLine();

        if (nick.length() > 10 || nick.isEmpty()) {
            System.out.println("Error: nickname must be between 1 and 10 characters.");
            return;
        }

        try (Socket socket = new Socket(IP, PORT);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            out.writeUTF(Protocol.start_command);
            out.writeUTF(Protocol.game_type);
            out.writeUTF(nick);
            out.flush();

            int matchId = in.readInt();
            String playerList = in.readUTF();
            boolean isHost = in.readBoolean();

            System.out.println("\n--- PLAYERS ---");
            System.out.println(playerList);

            GameManager game = new GameManager();

            if (isHost) {
                int listenPort = in.readInt();
                game.playAsHost(listenPort, matchId, out);
            } else {
                String hostIp = in.readUTF();
                int hostPort = in.readInt();
                game.playAsGuest(hostIp, hostPort);
            }

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}