package client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulticastManager {
    private static final String MULTICAST_GROUP = "224.0.0.1";
    private static final int MULTICAST_PORT = 6789;
    private static MulticastSocket socket;
    private static InetAddress group;
    private static final Object lock = new Object();

    /**
     * Blocco statico per la creazione del socket multicast e la connessione al gruppo.
     * Inizializza il socket per il multicast e si unisce al gruppo specificato.
     * Viene eseguito automaticamente quando la classe {@code MulticastManager} viene caricata.
     *
     * @throws RuntimeException Se si verifica un errore durante la creazione del socket multicast o la connessione al gruppo.
     */
    public MulticastManager() {
        try {
            socket = new MulticastSocket(MULTICAST_PORT);
            socket.setReuseAddress(true);
            group = InetAddress.getByName(MULTICAST_GROUP);
            socket.joinGroup(group);
            System.out.println("MulticastManager: Socket multicast creato e unito al gruppo " + MULTICAST_GROUP);
        } catch (IOException e) {
            throw new RuntimeException("Errore nella creazione del socket multicast", e);
        }
    }

    /**
     * Restituisce il socket multicast condiviso per l'invio o la ricezione dei dati.
     *
     * @return Il socket multicast attualmente in uso.
     */
    public static MulticastSocket getSocket() {
        return socket;
    }

    /**
     * Chiude il socket multicast e lascia il gruppo multicast.
     * Questo metodo dovrebbe essere chiamato alla fine del ciclo di vita dell'applicazione per liberare le risorse.
     *
     * @throws IOException Se si verifica un errore durante la chiusura del socket o l'uscita dal gruppo.
     */
    public static void closeSocket() {
        synchronized (lock) {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.leaveGroup(group);
                    socket.close();
                    System.out.println("MulticastManager: Socket multicast chiuso.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
