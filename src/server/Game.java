package server;

import client.Player;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

//Implementa Serializable para que el objeto pueda convertirse en bytes y viajar por la red
public class Game implements Serializable {
    private String id; //ID único de la partida
    private String gameType; //Tipo de juego
    private List<Player> players; //Lista de jugadores conectados a esta mesa
    private boolean isFinished; //Para saber si el juego ha terminado
    private String resultLog; //Guardará todo lo que ha pasado en la partida (quién tiró, cuánto sacó, etc.)

    //Constante para el juego de dados (siempre 2)
    public static final int MAX_JUGADORES = 2;

    //El constructor se ejecuta cuando el servidor crea una nueva mesa (new Game())
    public Game(String gameType) {
        //randomUUID genera una cadena alfenumeruca de 32 caracteres. Se cogen los 8 primeros
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.gameType = gameType;
        this.players = new ArrayList<>();
        this.resultLog ="";
        this.isFinished = false;
    }

    /**
     * Intenta añadir un juegador a players<>
     * Si ha podido entrar devuelve true y si la mesa estaba llena false
     * Si aquí no encuentra partida el Servidor se encarga de encontrarla en el for de startGame()
     */
    public boolean addPlayer(Player player) {
        //Se verifica si caben jugadores en esa mesa
        if (players.size() < MAX_JUGADORES) {
            players.add(player);
            return true;
        }
        return false; //Mesa llena
    }

    public boolean isFull() {
        return players.size() == MAX_JUGADORES;
    }

    /**
     * Lógica principal del juego
     * Este método solo se llama cuando la mesa está llena (desde el Server).
     * Calcula toda la partida de golpe y guarda el texto en 'resultLog'.
     */
    public void play() {
        //Por si acaso se llama al método y la mesa no está llena
        if (!isFull()){
            return;
        }

        Random random = new Random();


        Player playerOne = null;
        Player playerTwo = null;

        //Empezará el invitado a jugar
        for (Player p : players) {
            if (!p.isHost()) {
                playerOne = p;
            } else {
                playerTwo = p;
            }
        }

        this.resultLog += "\n--- INICIO DE PARTIDA (ID: " + id + ") ---\n";
        this.resultLog += "Juego: " + gameType + "\n";
        this.resultLog += "Jugadores: " + playerOne.getNickname() + " vs " + playerTwo.getNickname() + "\n";

        int score1, score2; //Variables para almacenar la tirada de los dados
        int round = 1; //Contador de rondas

        //Bucle de juego (se repite si hay empate)
        do {
            if (round > 1){
                this.resultLog += "¡Empate! Se repite la tirada (Ronda " + round + ")...\n";
            }

            //Turno del jugador 1 (no Host)
            score1 = random.nextInt(6) + 1;
            this.resultLog += " > " + playerOne.getNickname() + " tira... [" + score1 + "]\n";

            // Turno del jugador 2 (Host)
            score2 = random.nextInt(6) + 1;
            this.resultLog += " > " + playerTwo.getNickname() + " tira... [" + score2 + "]\n";
            round++;
        } while (score1 == score2);

        //Si se llega aquí es que ya hay un winner. Se crea un player nuevo y se almacena el winner
        Player winner = (score1 > score2) ? playerOne : playerTwo;

        this.resultLog += "----------------------------------\n";
        this.resultLog += "¡GANADOR: " + winner.getNickname() + "!\n";
        this.resultLog += "----------------------------------\n";

        this.isFinished = true;
    }

    // Getters
    public String getId() { return id; }
    public String getGameType() { return gameType; }
    public List<Player> getPlayers() { return players; }
    public boolean isFinished() { return isFinished; }
    public String getResultLog() { return resultLog; }
}