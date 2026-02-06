package client;

import java.io.*;
import java.net.Socket;
import java.util.List;

// esta es la clase que simula multiples clientes
public class Main {

    // 5 partidas (10 jugadores en total)
    private static final int num_games = 5;
    private static final String host = "localhost";
    private static final int port = 6000;

    public static void main(String[] args) {
        // lanzamos 5 pares de hilos (10 jugadores)
        for (int i = 1; i <= num_games; i++) {
            String gameName = "Table -" + i;

            new Thread(new ClientTask(gameName, "Player A - " + i)).start();

            new Thread(new ClientTask(gameName, "Player B - " + i)).start();
        }
    }

     // esto es lo que realiza cada cliente:

    static class ClientTask implements Runnable {   // necesitamos implementar Runnable para gestionar los hilos
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

                // solicitar unirse para jugar
                out.writeInt(1); // el 1 significa jugar
                out.writeObject(gameName);
                out.writeObject(nickname);
                out.flush(); // para enviar todo

                // esperar la respuesta del server
                // el servidor nos manda una lista de los jugadores
                @SuppressWarnings("unchecked")  // esto es para que Java ignore la advertencia del tipo de datos
                List<Player> players = (List<Player>) in.readObject();

                // somos el host?
                boolean isHost = in.readBoolean();

                // id de partida
                String gameId = (String) in.readObject();

                // resultado de juego
                String result = (String) in.readObject();

                // informacion por consola:
                // sincronizamos la salida
                synchronized (System.out) {
                    System.out.println("-------------------------------------------");
                    System.out.println("Client: " + nickname + " (Game: " + gameName + ")");
                    System.out.println("Player list:");
                    for (Player p : players) {
                        System.out.println(" - " + p);
                    }
                    System.out.println("\nGame result:");
                    System.out.println(result);
                    System.out.println("Am I Host?: " + (isHost ? "YES (Must close game)" : "NO"));
                    System.out.println("--------------------------------------------\n");
                }

                // cierro la partida si soy host
                if (isHost) {
                    // pausa antes de que el servidor borre la partida para asegurar que el otro cliente haya recibido sus datos
                    Thread.sleep(100);
                    // abro nueva conexion para cerrar la partida
                    closeGame(gameId);
                }

            } catch (Exception e) {
                System.err.println("Error in client " + nickname + ": " + e.getMessage());
            }
        }

        private void closeGame(String gameId) {
            try (Socket socket = new Socket(host, port);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                out.writeInt(2); // el 2 es para finalizar el juego
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