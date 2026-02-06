package servidor;

import client.Player;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class Game implements Serializable {
    private String id;
    private String gameType;
    private List<Player> players;
    private boolean isFinish;
    private String resultLog; // guarda el resumen de la partida

    // Constante para el juego de dados
    public static final int MAX_JUGADORES = 2;

    public Game(String gameType) {
        // Generamos un ID único corto para identificar la partida
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.gameType = gameType;
        this.players = new ArrayList<>();
        this.isFinish = false;
    }

    /**
     * Añade un jugador a la partida si hay hueco.
     */
    public boolean addJugador(Player jugador) {
        if (players.size() < MAX_JUGADORES) {
            players.add(jugador);
            return true;
        }
        return false;
    }

    public boolean isLleno() {
        return players.size() == MAX_JUGADORES;
    }

    /**
     * Lógica principal del juego de dados.
     * - Identifica quién empieza (el que NO es host).
     * - Gestiona empates repitiendo tiradas.
     * - Genera un log con los resultados.
     */
    public void jugar() {
        if (!isLleno()) return;

        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        // 1. Identificar orden de turno (No-Host primero)
        Player jugadorInicial = null; // No Host
        Player jugadorSegundo = null; // Host

        for (Player p : players) {
            if (!p.isHost()) {
                jugadorInicial = p;
            } else {
                jugadorSegundo = p;
            }
        }

        // Caso de seguridad por si la asignación de host falló externamente
        if (jugadorInicial == null || jugadorSegundo == null) {
            jugadorInicial = players.get(0);
            jugadorSegundo = players.get(1);
        }

        sb.append("\n--- INICIO DE PARTIDA (ID: ").append(id).append(") ---\n");
        sb.append("Juego: ").append(gameType).append("\n");
        sb.append("Jugadores: ").append(jugadorInicial.getNickname()).append(" vs ").append(jugadorSegundo.getNickname()).append("\n");

        int puntuacion1, puntuacion2;
        int ronda = 1;

        // 2. Bucle de juego (se repite si hay empate)
        do {
            if (ronda > 1) sb.append("¡Empate! Se repite la tirada (Ronda ").append(ronda).append(")...\n");

            // Turno del No-Host
            puntuacion1 = random.nextInt(6) + 1;
            sb.append(" > ").append(jugadorInicial.getNickname()).append(" (No-Host) tira... [").append(puntuacion1).append("]\n");

            // Turno del Host
            puntuacion2 = random.nextInt(6) + 1;
            sb.append(" > ").append(jugadorSegundo.getNickname()).append(" (Host)    tira... [").append(puntuacion2).append("]\n");

            ronda++;
        } while (puntuacion1 == puntuacion2);

        // 3. Determinar ganador
        Player ganador = (puntuacion1 > puntuacion2) ? jugadorInicial : jugadorSegundo;

        sb.append("----------------------------------\n");
        sb.append("¡GANADOR: ").append(ganador.getNickname()).append("!\n");
        sb.append("----------------------------------\n");

        this.resultLog = sb.toString();
        this.isFinish = true;
    }

    // Getters
    public String getId() { return id; }
    public String getGameType() { return gameType; }
    public List<Player> getPlayers() { return players; }
    public boolean isFinish() { return isFinish; }
    public String getResultLog() { return resultLog; }
}