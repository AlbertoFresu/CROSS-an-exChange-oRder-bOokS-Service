package client;

import common.JsonParsing;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

/**
 * Questa classe implementa il ricevimento di notifiche multicast UDP.
 * Viene eseguita in un thread separato e riceve i pacchetti multicast dal socket,
 * stampando il contenuto del messaggio ricevuto.
 */
public class NotificationReceiverMulticastUDP implements Runnable{

    private JsonParsing jsonParsing = new JsonParsing();
    private volatile boolean isRunning = true; // Flag per fermare il thread

    private MulticastManager multicastManager;
    public NotificationReceiverMulticastUDP() {
        this.multicastManager = new MulticastManager();
    }
    /**
     * Esegue la ricezione dei messaggi multicast UDP in un ciclo continuo finché
     * il flag `isRunning` è impostato su true. I messaggi vengono letti e stampati sulla console.
     */
    @Override
    public void run() {
        MulticastSocket socket = MulticastManager.getSocket();
        try {
            byte[] buffer = new byte[1024];
            while (isRunning) {

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String messageRetrieved = new String(packet.getData(), 0, packet.getLength());
                String messageParsed = jsonParsing.parseThresholdNotification(messageRetrieved);
                System.out.println("Notifica multicast: " + messageParsed);
            }
        } catch (IOException e) {
            // Gestisci solo l'errore se il socket è stato chiuso esplicitamente
            if (!isRunning) {
                System.out.println("Thread fermato, chiusura socket.");
            } else {
                System.err.println("Errore nella ricezione del messaggio multicast: " + e.getMessage());
            }
        }
        finally {
            MulticastManager.closeSocket();
        }
    }

    /**
     * Ferma l'esecuzione del thread impostando il flag `isRunning` su false.
     */
    public void stopRunning() {
        isRunning = false;
    }
}
