package client;

import java.io.*;
import java.net.Socket;
import java.sql.SQLOutput;
import java.util.List;

// class that simulates multiple clients
public class Main {

    // 5 games (10 players in total)
    private static final int num_games = 5;
    private static final String host = "localhost";
    private static final int port = 6000;

    public static void main(String[] args) {
        // launch 5 pairs of threads
        for (int i = 1; i <= num_games; i++) {
            String gameName = "Table-" + i;

            new Thread(new ClientTask(gameName, "Player A-" + i)).start();

            new Thread(new ClientTask(gameName, "Player B-" + i)).start();
        }
    }

    static class ClientTask implements Runnable {  
        private String gameName;
        private String nickname;

        public ClientTask(String gameName, String nickname) {
            this.gameName = gameName;
            this.nickname = nickname;
        }

        @Override
        public void run() {
            try (Socket socket = new Socket(host, port);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                // request to join and play
                out.writeInt(1); // 1 means play
                out.writeObject(gameName);
                out.writeObject(nickname);
                out.flush(); //to send everything

                // the server send us a list of players 
                @SuppressWarnings("unchecked")  
                List<Player> players = (List<Player>) in.readObject();

                boolean isHost = in.readBoolean();
                
                String gameId = (String) in.readObject();
                
                String result = (String) in.readObject();

                // synchronize the console output
                synchronized (System.out) {
                    System.out.println("-------------------------------------------");
                    System.out.println("Client: " + nickname + " (Game: " + gameName + ")");
                    System.out.println("Player list:");
                    for (Player p : players) {
                        System.out.println(" - " + p);
                    }
                    System.out.println("\nGame result:");
                    System.out.println(result);
                }

                if (isHost) {
                    System.out.println("I am host, closing the game...");
                    System.out.println("--------------------------------------------\n");
                    Thread.sleep(100);
                    closeGame(gameId);
                } else {
                    System.out.println("I am not the host.");
                    System.out.println("--------------------------------------------\n");
                }

            } catch (Exception e) {
                System.err.println("Error in client " + nickname + ": " + e.getMessage());
            }
        }

        private void closeGame(String gameId) {
            try (Socket socket = new Socket(host, port);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                out.writeInt(2); // 2 is to finish 
                out.writeObject(gameId);
                out.flush();

                String confirmation = in.readUTF();
                System.out.println("Host (" + nickname + ") -> Server: " + confirmation);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
