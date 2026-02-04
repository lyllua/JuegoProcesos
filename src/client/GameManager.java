package client;

import java.io.*;
import java.net.*;

public class GameManager {

    public void playAsHost(int listenPort, int matchId, DataOutputStream outCentral) {
        try (ServerSocket serverSocket = new ServerSocket(listenPort)) {
            try (Socket guestSocket = serverSocket.accept();
                 DataInputStream inGuest = new DataInputStream(guestSocket.getInputStream());
                 DataOutputStream outGuest = new DataOutputStream(guestSocket.getOutputStream())) {

                String result = Protocol.draw;

                while (result.equals(Protocol.draw)) {
                    int guestDie = inGuest.readInt();
                    int myDie = (int) (Math.random() * 6) + 1;

                    if (myDie > guestDie) {
                        result = Protocol.victory;
                    } else if (myDie < guestDie) {
                        result = Protocol.defeat;
                    } else {
                        result = Protocol.draw;
                    }

                    outGuest.writeUTF(result);
                    outGuest.flush();
                }

                outCentral.writeUTF(Protocol.end_command);
                outCentral.writeInt(matchId);
                outCentral.flush();
            }
        } catch (IOException e) {
            System.err.println("Host Error: " + e.getMessage());
        }
    }

    public void playAsGuest(String hostIp, int hostPort) {
        try (Socket hostSocket = new Socket(hostIp, hostPort);
             DataOutputStream out = new DataOutputStream(hostSocket.getOutputStream());
             DataInputStream in = new DataInputStream(hostSocket.getInputStream())) {

            String result = Protocol.draw;

            while (result.equals(Protocol.draw)) {
                int myDie = (int) (Math.random() * 6) + 1;
                out.writeInt(myDie);
                out.flush();

                result = in.readUTF();

                if (result.equals(Protocol.victory)) {
                    System.out.println("Result: HOST WON - YOU LOST");
                } else if (result.equals(Protocol.defeat)) {
                    System.out.println("Result: HOST LOST - YOU WON");
                } else {
                    System.out.println("Result: DRgit add .AW - ROLLING AGAIN");
                }
            }
        } catch (IOException e) {
            System.err.println("Guest Error: " + e.getMessage());
        }
    }
}