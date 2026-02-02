package servidor;

import java.util.*;

//Almacena los datos de la partida y gestiona que solo haya 2 jugadores en 1 partida
public class Partida {
    private int id;
    private String tipoJuego;
    private List<Jugador> jugadores;
    private boolean completa; //true cuando ya hay 2 jugadores

    //CONSTRUCTOR
    public Partida (int id, String tipoJuego) {
        this.id = id;
        this.tipoJuego = tipoJuego;
        jugadores = new ArrayList<>();
    }

    //Método sincronizado para que no entren dos jugadores a la vez
    public synchronized boolean agregarJugador(Jugador jugador) {
        if (!completa) {
            jugadores.add(jugador);
            if(jugadores.size() == 2) { //El tamaño de la partida es de 2 jugadores
                completa = true;
                notifyAll(); /*Despierta al primer jugador que entró en la partida.
                               Cuando un hilo entra en una partida que está vacía
                               entra en el método esperarLlenado() el cual tiene un wait*/
            }
            return true;
        }
        return false;
    }


    //El primer jugador que entre a la partida esperará al segundo (agregarJugador())
    public synchronized void esperarLlenado() throws InterruptedException {
         while (!completa) { /*hay que usar un bucle porque el sistema operativo
                                puede despertar al hilo sin una instrucción (Spurious Wakeup)*/
             wait();
         }
    }

    //GETTERS
    public List<Jugador> getJugadores() {
        return jugadores;
    }

    public int getId() {
        return id;
    }

    public String getTipoJuego() {
        return tipoJuego;
    }

    public boolean isCompleta() {
        return completa;
    }
}
