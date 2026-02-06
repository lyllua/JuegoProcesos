package servidor;


import client.Player;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Server {
    private static final int PORT = 6000;
    // Lista compartida de partidas en curso
    private static List<Game> games = new ArrayList<>();
    // Semáforo para proteger el acceso a la lista de 'games' (Exclusión Mutua)
    private static Semaphore mutex = new Semaphore(1);

    public static void main(String[] args) {
        System.out.println("SERVIDOR INICIADO EN PUERTO " + PORT);
        System.out.println("Esperando jugadores...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                // Aceptamos conexión del cliente
                Socket clientSocket = serverSocket.accept();
                // Delegamos la gestión a un hilo independiente
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Hilo encargado de gestionar la comunicación con un único cliente.
     */
    private static class ClientHandler extends Thread {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                // Inicialización de flujos
                // IMPORTANTE: Crear el Output antes que el Input para evitar bloqueos
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // Paso 2: Leer acción (1: Nuevo/Unirse Juego, 2: Finalizar Juego)
                int accion = in.readInt();

                if (accion == 1) {
                    handleStartGame();
                } else if (accion == 2) {
                    handleEndGame();
                }

            } catch (Exception e) {
                System.err.println("Error en conexión con cliente: " + e.getMessage());
            } finally {
                try {
                    if (!socket.isClosed()) socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Gestiona la lógica de iniciar o unirse a una partida (Puntos 3, 4 y 5)
         */
        private void handleStartGame() throws Exception {
            // Paso 3: Leer datos del jugador
            String tipoJuego = (String) in.readObject();
            String nickname = (String) in.readObject();

            // Obtener IP y puerto real del cliente
            String clientIp = socket.getInetAddress().getHostAddress();
            int clientPort = socket.getPort(); // Puerto remoto del cliente

            Player jugador = new Player(nickname, clientIp, clientPort);
            Game partidaAsignada = null;
            boolean soyElUltimo = false;

            // --- SECCIÓN CRÍTICA: Acceso a la lista de partidas ---
            mutex.acquire();
            try {
                // Buscamos si hay alguna partida de este tipo con hueco
                for (Game g : games) {
                    if (g.getGameType().equals(tipoJuego) && !g.isLleno()) {
                        partidaAsignada = g;
                        break;
                    }
                }

                // Si no hay partida, creamos una nueva
                if (partidaAsignada == null) {
                    partidaAsignada = new Game(tipoJuego);
                    games.add(partidaAsignada);
                    jugador.setHostStatus(true); // El primero es el Host
                    System.out.println("Nueva partida creada (ID: " + partidaAsignada.getId() + ") por " + nickname);
                } else {
                    jugador.setHostStatus(false); // El que se une no es Host
                    System.out.println("Jugador " + nickname + " se une a partida " + partidaAsignada.getId());
                }

                // Añadimos el jugador a la partida
                partidaAsignada.addJugador(jugador);

                // Verificamos si hemos llenado la partida con este jugador
                if (partidaAsignada.isLleno()) {
                    soyElUltimo = true;
                }

            } finally {
                mutex.release(); // Liberamos el semáforo
            }
            // --- FIN SECCIÓN CRÍTICA ---

            // Sincronización de Hilos: Esperar a que estén todos los jugadores
            // Usamos el objeto partidaAsignada como monitor (lock)
            synchronized (partidaAsignada) {
                if (!partidaAsignada.isLleno()) {
                    // Si soy el primero, espero
                    System.out.println("Jugador " + nickname + " esperando oponente...");
                    partidaAsignada.wait();
                } else {
                    // Si soy el último, notifico a los que esperan
                    if (soyElUltimo) {
                        // Ejecutamos la lógica del juego (dados) solo una vez
                        partidaAsignada.jugar();
                        partidaAsignada.notifyAll(); // Despierta al Host
                    }
                }
            }

            // --- ENVÍO DE RESPUESTA AL CLIENTE (Paso 5) ---
            // 1. Enviamos la lista de jugadores conectados a esa partida
            out.writeObject(partidaAsignada.getPlayers());

            // 2. Informamos si este cliente específico es el anfitrión
            out.writeBoolean(jugador.isHost());

            // 3. Enviamos el ID de la partida (para que el host pueda cerrarla luego)
            out.writeObject(partidaAsignada.getId());

            // 4. Enviamos el resultado del juego (Logs generados en Game)
            out.writeObject(partidaAsignada.getResultLog());

            out.flush();
        }

        /**
         * Gestiona la eliminación de la partida (Paso 6)
         */
        private void handleEndGame() throws Exception {
            String gameId = (String) in.readObject();
            boolean borrado = false;

            // --- SECCIÓN CRÍTICA ---
            mutex.acquire();
            try {
                // Usamos removeIf que devuelve true si eliminó algún elemento
                borrado = games.removeIf(g -> g.getId().equals(gameId));
            } finally {
                mutex.release();
            }
            // --- FIN SECCIÓN CRÍTICA ---

            if (borrado) {
                System.out.println("Partida finalizada y eliminada de memoria: " + gameId);
                out.writeUTF("Partida " + gameId + " eliminada correctamente.");
            } else {
                out.writeUTF("Error: No se encontró la partida o ya estaba cerrada.");
            }
            out.flush();
        }
    }
}