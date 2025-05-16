package client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

/**
 * Questa classe riceve notifiche UDP da una porta specifica utilizzando un `DatagramChannel` non bloccante.
 * Gestisce la lettura delle notifiche in modo concorrente e le elabora utilizzando Gson per il parsing del messaggio JSON.
 */
public class NotificationReceiverUDP implements Runnable {
    private int port;
    private volatile boolean terminateFlag;
    private Selector selector;

    /**
     * Costruisce un'istanza di `NotificationReceiverUDP` con la porta e il flag per terminare il thread.
     *
     * @param terminateFlag Il flag per determinare se terminare l'esecuzione del thread.
     * @param port La porta sulla quale il socket UDP deve essere configurato.
     */
    public NotificationReceiverUDP(boolean terminateFlag, int port) {
        this.terminateFlag = terminateFlag;
        this.port = port;
    }

    /**
     * Metodo eseguito dal thread. Avvia la ricezione delle notifiche UDP.
     */
    @Override
    public void run() {
        startUDPNotification();
    }

    /**
     * Inizializza il canale UDP e gestisce la ricezione dei pacchetti in modo non bloccante.
     * Il metodo registra il canale al selettore e riceve i pacchetti, elaborando i dati ricevuti.
     */
    private void startUDPNotification() {
        try (DatagramChannel channel = DatagramChannel.open()) {
            selector = Selector.open();
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.bind(new InetSocketAddress(port));
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);

            while (!Thread.currentThread().isInterrupted() && !terminateFlag) {
                if (selector.select() > 0) {
                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();
                        keys.remove();

                        //System.out.println("Listener UDP non bloccante avviato sulla porta " + port);
                        if (key.isReadable()) {
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            channel.receive(buffer);
                            buffer.flip();
                            if (buffer.hasRemaining()) {
                                byte[] data = new byte[buffer.limit()];
                                buffer.get(data);
                                String message = new String(data);
                                parseAndPrintNotification(message);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Errore nella gestione del socket UDP: " + e.getMessage());
        } finally {
            if (selector != null && selector.isOpen()) {
                try {
                    selector.close();
                } catch (IOException e) {
                    System.err.println("Errore nella chiusura del selector: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Analizza e stampa la notifica ricevuta in formato leggibile utilizzando la libreria Gson.
     * Se sono presenti informazioni su ordini (trades), vengono estratte e stampate.
     *
     * @param message il messaggio da analizzare e stampare.
     */
    private void parseAndPrintNotification(String message) {
        Gson gson = new Gson();

        try {
            JsonObject jsonObject = gson.fromJson(message, JsonObject.class);  // Deserializza il JSON
            String notification = jsonObject.get("notification").getAsString();

            System.out.println("Notifica: " + notification);

            // Se ci sono trades, li estrai e li stampi
            if (jsonObject.has("trades")) {
                JsonArray trades = jsonObject.getAsJsonArray("trades");

                for (int i = 0; i < trades.size(); i++) {
                    JsonObject trade = trades.get(i).getAsJsonObject();
                    int orderId = trade.get("orderId").getAsInt();
                    String type = trade.get("type").getAsString();
                    String orderType = trade.get("orderType").getAsString();
                    int size = trade.get("size").getAsInt();
                    int price = trade.get("price").getAsInt();

                    System.out.println(String.format("Ordine ID: %d, Tipo: %s, Tipo Ordine: %s, Dimensione: %d, Prezzo: %d",
                            orderId, type, orderType, size, price));
                }
            }

        } catch (Exception e) {
            System.err.println("Errore nel parsing della notifica: " + e.getMessage());
        }
    }

    /**
     * Ferma l'esecuzione del thread impostando il flag `terminateFlag` su true e chiudendo il selettore.
     */
    public void terminate() {
        terminateFlag = true;
        if (selector != null && selector.isOpen()) {
            try {
                selector.close();
            } catch (IOException e) {
                System.err.println("Errore nella chiusura del selector UDP: " + e.getMessage());
            }
        }
    }
}
