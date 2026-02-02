package servidor;

import java.io.*;
import java.net.*;

//Representa a un jugador conectado. Se guarda la info y se pasa al rival
public class Jugador {
    private String nickname;
    private Socket socket;
    private int puertoP2P; //El puerto donde este jugador escuchará al rival
    private InetAddress ip; //Almacena la IP del jugador actual
    private DataOutputStream salida;

    //CONSTRUCTOR
    public Jugador(String nickname, Socket socket, int puertoP2P) throws IOException {
        this.nickname = nickname;
        this.socket = socket;
        this.puertoP2P = puertoP2P;
        this.ip = socket.getInetAddress(); //Obtiene la IP desde el Socket
        this.salida = new DataOutputStream(socket.getOutputStream()); //Se configura un canal de salida para los mensajes
    }


    //Método donde el servidor envía a este jugador la información de su rival
    public void enviarDatosRival(String rol, String ipRival, int puertoRival, String nickRival) throws IOException {
        salida.writeUTF(rol); //Anfitrion o Invitado
        salida.writeUTF(ipRival);
        salida.writeInt(puertoRival);
        salida.writeUTF(nickRival);
        salida.flush(); //flush() fuerza el envío
    }


    //GETTERS
    //En servidor se necesitan para enviar los datos del contrincante
    public String getNickname() {
        return nickname;
    }

    public InetAddress getIp() {
        return ip;
    }

    public int getPuertoP2P() {
        return puertoP2P;
    }
}
