package server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NotificationSender{
    private static final String MULTICAST_GROUP = "224.0.0.1";
    private static final int PORT = 6789;
    private final DatagramSocket socket;
    private ExecutorService executorService;

    public NotificationSender() throws SocketException {
        this.socket = new DatagramSocket();
        executorService = Executors.newCachedThreadPool();
    }

    /**
     * Invia una notifica multicast a un gruppo specifico.
     * Utilizza un ExecutorService per gestire l'invio in un thread separato.
     * Configura il socket per il riutilizzo dell'indirizzo e invia il messaggio al gruppo multicast.
     *
     * @param message Il messaggio da inviare al gruppo multicast.
     */
    public void sendNotificationMulticast(String message) {
        executorService.submit(() -> {
            try{
                try{socket.setReuseAddress(true);
                    }
                catch (SocketException e){
                    System.err.println("Errore nel riutilizzo del socket multicast");
                }
                byte[] buffer = message.getBytes();
                InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, PORT);
                socket.send(packet);
                System.out.println("Notifica Inviata a " + MULTICAST_GROUP + ": " + message + " " + PORT);
            } catch (IOException e) {
                System.err.println("Errore durante l'invio della notifica: " + e.getMessage());
            }
        });
    }

    /**
     * Invia una notifica UDP a un indirizzo specifico.
     * Crea un DatagramSocket temporaneo per inviare il pacchetto dati all'indirizzo specificato.
     *
     * @param message Il messaggio da inviare.
     * @param address L'indirizzo destinatario della notifica.
     */
    public void sendNotification(String message, InetSocketAddress address) {
        executorService.submit(() -> {
            try(DatagramSocket socket = new DatagramSocket()) {
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address);
                socket.send(packet);
                System.out.println("Notifica inviata a " + address);
            } catch (IOException e) {
                System.err.println("Errore durante l'invio della notifica UDP: " + e.getMessage());
            }
        });
    }





    /**
     * Arresta il servizio di notifica chiudendo il thread pool in modo ordinato.
     * Attende la terminazione dei task in corso prima di chiudere il socket.
     */
    public void shutDown() {
        try {
            // Attendi che tutti i task siano completati prima di chiudere il socket
            executorService.shutdown();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("Timeout durante l'attesa del completamento dell'invio delle notifiche.");
            }
            System.out.println("Socket chiuso correttamente.");
        } catch (InterruptedException e) {
            System.err.println("Errore nella chiusura del socket: " + e.getMessage());
        }
    }
}

