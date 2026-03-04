package client;
import java.io.Serializable;

public class Player implements Serializable {  // to send the object through sockets
    private String nickname;
    private String host;
    private int port;
    private boolean isHost;

    public Player(String nickname, String host, int port) {
        setNickname(nickname);
        this.host = host;
        this.port = port;
        this.isHost = false; // by default its not the host
    }

    public String getNickname() {
        return nickname;
    }

    public final void setNickname(String nickname) {
        // nickname must not exceed 10 characters:
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
        return String.format("Player: %-10s | IP: %-15s | Port: %-5d | Host: %s",
                nickname, host, port, (isHost ? "YES" : "NO"));
    }
}
