package server;


import client.Player;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Server {
    private static final int PORT = 6000;
    //ArrayList que almacena las partidas en curso
    private static List<Game> activeGames = new ArrayList<>();
    //Semáforo para permitir que solo pase 1 hilo a la vez
    private static Semaphore semaphore = new Semaphore(1);

    public static void main(String[] args) {
        System.out.println("SERVER STARTED ON PORT: " + PORT);
        System.out.println("Waiting for players...");

        //Se abre el puerto en un try catch
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            //El servidor siempre esperará clientes
            while (true) {
                //El programa se detiene hasta que un cliente se conecte
                Socket clientSocket = serverSocket.accept();
                //Crea un nuevo hilo por cada jugador que se una
                new PlayerConnection(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Hilo encargado de gestionar la comunicación con un único cliente.
     * Crea un hilo para poder ejecutarse en paralelo
     */
    private static class PlayerConnection extends Thread {
        private Socket socket; //Conexion con el cliente
        /*
        Objet...Stream permite enviar y recibir objetos enteros
        A diferencia de Data...Stream puedes envia todos los datos sin necesidad de hacer bucles
         */
        private ObjectOutputStream out; //Canal para enviar datos
        private ObjectInputStream in;   //Canal para recibir datos

        //Constructor para indicar la conexión con el cliente
        public PlayerConnection(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                //Inicialización de los canales.

                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                //Lee un entero que llega desde el cliente
                //1 = Quiere empezar/unirse a una partida.
                //2 = Quiere cerrar una partida
                int action = in.readInt();

                if (action == 1) {
                    startGame();
                } else if (action == 2) {
                    endGame();
                }

            } catch (Exception e) {
                System.err.println("Error in connection with client: " + e.getMessage());
            } finally {
                //Un finally para que siempre cierre el cliente
                try {
                    if (!socket.isClosed()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Gestiona la lógica de iniciar o unirse a una partida
         */
        private void startGame() throws Exception {
            //Lee el tipo de juego y el nickname usando ObjectInputStream
            /*Desde el cliente se sigue este orden al enviar los datos del objeto:
            1. Envía un int para jugar o no
            2. El nombre de la partida
            3. El player
            Por eso hay que ''pedir'' en ese orden al objeto
            */
            String gameType = (String) in.readObject();
            String nickname = (String) in.readObject();

            //Obtener IP y puerto desde donde se conecta el cliente
            String clientIp = socket.getInetAddress().getHostAddress();
            int clientPort = socket.getPort();

            //Se crea un objeto player con los datos que se acaban de sacar
            Player player = new Player(nickname, clientIp, clientPort);

            //Inicializo una variable Game fuera del try para que no muera dentro
            Game assignedGame = null;
            //Variable booleana para luego comprobar si es el ultimo player en entra a la sala
            boolean isLastPlayer = false;

            //si hay otro hilo dentro del semáforo el hilo que quiera entrar espera aquí
            semaphore.acquire();
            try {
                //busca si hay alguna partida con hueco
                for (Game game : activeGames) {
                    //Comprueba si es el mismo juego que busca y que no esté lleno
                    if (game.getGameType().equals(gameType) && !game.isFull()) {
                        assignedGame = game;
                        break;
                    }
                }

                //Si no ha encontrado una partida a medio llenar crea una nueva
                if (assignedGame == null) {
                    assignedGame = new Game(gameType);
                    activeGames.add(assignedGame);
                    //El primero que se une siempre será el host
                    player.setHostStatus(true);
                    System.out.println("New game created (ID: " + assignedGame.getId() + ") by " + nickname);
                } else {
                    //El segundo nunca será el host
                    player.setHostStatus(false);
                    System.out.println("Player " + nickname + " joins game " + assignedGame.getId());
                }

                // Añadimos el jugador a la partida
                assignedGame.addPlayer(player);

                // Verificamos si hemos llenado la partida con este player
                if (assignedGame.isFull()) {
                    isLastPlayer = true;
                }

            } finally {
                //Importante que siempre libere el semáforo haya o no una excepcion
                semaphore.release();
            }

            //Ahora se necesita coordinar a los dos jugadores para que el juego empiece, solo cuando ambos estén listos.
            //Para ello se usa un cerrojo por partida, para que se puedan jugar varias partidas a la vez
            synchronized (assignedGame) {
                if (!assignedGame.isFull()) {
                    //El primero espera al segundo con .wait()
                    System.out.println("Player " + nickname + " waiting for opponent...");
                    assignedGame.wait();
                } else {
                    //A la que entre el último jugador directamente juegan y despierta al host
                    if (isLastPlayer) {
                        //Se ejecuta la lógica del juego
                        assignedGame.play();
                        assignedGame.notifyAll(); // Despierta al Host
                    }
                }
            }

            //Si se llega a esta parte del código es que ya hay 2 jugadores en una partida y notifica al cliente de:

            //Envía la lista de jugadores (para que vean contra quién juega)
            out.writeObject(assignedGame.getPlayers());

            //Informa si este cliente es el anfitrion
            out.writeBoolean(player.isHost());

            //Se envía el ID de la partida (para que el host pueda cerrarla luego)
            out.writeObject(assignedGame.getId());

            //Se envía el resultado del juego (Logs generados en Game)
            out.writeObject(assignedGame.getResultLog());

            //Fuerza el envío de los datos inmediatamente
            out.flush();
        }

        /**
         * Gestiona la eliminación de la partida
         * Este método lo llama el anfitrión cuando recibe lso resultados
         */
        private void endGame() throws Exception {
            //Lee el ID de la partida a borrar
            String gameId = (String) in.readObject();
            boolean isDeleted = false;

            //Al tener que modificar la lista de juegos (activeGame) se necesita el semáforo
            semaphore.acquire();
            try {
                // removeIf() es un método de las listas que borra un elemento si cumple una condición. y devuelve un booleano
                // "g -> g.getId().equals(gameId)" es una función lambda que busca la partida por ID.
                isDeleted = activeGames.removeIf(g -> g.getId().equals(gameId));
            } finally {
                //Siempre libera el semáforo
                semaphore.release();
            }

            //Esto sirve para enviar confirmación al cliente:
            if (isDeleted) {
                System.out.println("Game finished and removed from list: " + gameId);
                out.writeUTF("Game " + gameId + " removed successfully.");
            } else {
                //Puede pasar si ha habido alguna excepcion durante el codigo
                out.writeUTF("Error: Game not found or already closed.");
            }
            out.flush();
        }
    }
}