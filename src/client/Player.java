package client;
import java.io.Serializable;

/**
 * Representa a un jugador en el sistema.
 * Implementa Serializable para permitir el envío del objeto a través de Sockets.
 */
public class Player implements Serializable {
    private String nickname;
    private String host;
    private int port;
    private boolean isHost;

    public Player(String nickname, String host, int port) {
        setNickname(nickname);
        this.host = host;
        this.port = port;
        this.isHost = false; // Por defecto no es anfitrión
    }

    // Getters y Setters
    public String getNickname() {
        return nickname;
    }

    /**
     * Valida que el nickname no supere los 10 caracteres.
     */
    public final void setNickname(String nickname) {
        if (nickname != null && nickname.length() > 10) {
            this.nickname = nickname.substring(0, 10);
        } else {
            this.nickname = nickname;
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHostStatus(boolean isHost) {
        this.isHost = isHost;
    }

    @Override
    public String toString() {
        return String.format("Jugador: %-10s | IP: %-15s | Puerto: %-5d | Anfitrión: %s",
                nickname, host, port, (isHost ? "SÍ" : "NO"));
    }
}