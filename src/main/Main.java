package main;

import client.Player;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class Main {

    // Configuramos 5 partidas simultáneas (10 jugadores)
    private static final int NUM_PARTIDAS = 5;
    private static final String HOST = "localhost";
    private static final int PORT = 6000;

    public static void main(String[] args) {
        // Lanzamos 5 pares de hilos (10 jugadores en total)
        for (int i = 1; i <= NUM_PARTIDAS; i++) {
            String nombrePartida = "Mesa-" + i;

            // Jugador A para la Mesa i
            new Thread(new ClienteTask(nombrePartida, "JugadorA-" + i)).start();

            // Jugador B para la Mesa i
            new Thread(new ClienteTask(nombrePartida, "JugadorB-" + i)).start();
        }
    }

    /**
     * Tarea que realiza cada cliente (hilo) de forma independiente.
     */
    static class ClienteTask implements Runnable {
        private String nombrePartida;
        private String nickname;

        public ClienteTask(String nombrePartida, String nickname) {
            this.nombrePartida = nombrePartida;
            this.nickname = nickname;
        }

        @Override
        public void run() {
            try (Socket socket = new Socket(HOST, PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                // --- PASO 1: Solicitud de inicio/unión (Acción 1) ---
                out.writeInt(1); // 1 = Jugar
                out.writeObject(nombrePartida);
                out.writeObject(nickname);
                out.flush();

                // --- PASO 2: Esperar respuesta del servidor ---
                // El servidor nos envía la lista de jugadores conectados
                @SuppressWarnings("unchecked")
                List<Player> jugadores = (List<Player>) in.readObject();

                // El servidor nos dice si somos el Host
                boolean soyHost = in.readBoolean();

                // El servidor nos da el ID único de la partida
                String gameId = (String) in.readObject();

                // El servidor nos da el resultado del juego (Log)
                String resultado = (String) in.readObject();

                // --- PASO 3: Mostrar información por consola ---
                // Sincronizamos la salida para que no se mezclen las líneas en la consola
                synchronized (System.out) {
                    System.out.println("******************************************");
                    System.out.println("CLIENTE: " + nickname + " (Partida: " + nombrePartida + ")");
                    System.out.println("Lista de Jugadores:");
                    for (Player p : jugadores) {
                        System.out.println(" - " + p);
                    }
                    System.out.println("\nRESULTADO DEL JUEGO:");
                    System.out.println(resultado);
                    System.out.println("¿Soy Anfitrión?: " + (soyHost ? "SÍ (Debo cerrar la partida)" : "NO"));
                    System.out.println("******************************************\n");
                }

                // --- PASO 4: Cerrar partida si soy Host (Punto 6) ---
                if (soyHost) {
                    // Pequeña pausa para asegurar que el otro cliente haya recibido sus datos
                    // antes de que el servidor borre la partida (opcional, pero recomendado en simulación local)
                    Thread.sleep(100);

                    // Re-utilizamos la conexión para enviar la orden de cierre
                    // Nota: En un caso real complejo, podría ser una conexión nueva,
                    // pero aquí el servidor espera en el bucle del ClientHandler.

                    // IMPORTANTE: El servidor en mi código anterior procesaba UNA acción y cerraba.
                    // Para soportar esto, necesitamos que el cliente abra un nuevo socket
                    // O que el servidor tenga un bucle.
                    // Dado el diseño anterior 'stateless' por petición (un hilo muere al acabar):
                    // Abrimos una NUEVA conexión para mandar la orden de cierre.

                    cerrarPartida(gameId);
                }

            } catch (Exception e) {
                System.err.println("Error en cliente " + nickname + ": " + e.getMessage());
            }
        }

        private void cerrarPartida(String gameId) {
            try (Socket socket = new Socket(HOST, PORT);
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

                out.writeInt(2); // 2 = Finalizar Juego
                out.writeObject(gameId);
                out.flush();

                String confirmacion = in.readUTF();
                System.out.println("HOST (" + nickname + ") -> Server: " + confirmacion);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
