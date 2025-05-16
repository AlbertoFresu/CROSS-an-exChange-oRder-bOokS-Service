package server;

import common.JsonParsing;
import common.Response;
import order.LimitOrder;
import order.MarketOrder;
import order.OrderBook;
import order.StopOrder;
import user.UserDatabase;
import user.UserSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class ServerMessageHandler implements Runnable {
    private final SelectionKey key;
    private final UserDatabase userDatabase;
    private final OrderBook orderBook;

    /**
     * Costruttore della classe ServerMessageHandler
     * @param key SelectionKey associata al client
     * @param userDatabase Database degli utenti
     * @param orderBook Order book per la gestione degli ordini
     */
    public ServerMessageHandler(SelectionKey key, UserDatabase userDatabase, OrderBook orderBook) {
        this.key = key;
        this.userDatabase = userDatabase;
        this.orderBook = orderBook;
    }

    /**
     * Metodo eseguito nel thread che gestisce la comunicazione con il client
     */
    @Override
    public void run() {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        //Ogni thread creerà un istanza di common.JsonParsing per evitare overhead sulla sincronizzazione
        JsonParsing jsonParsing = new JsonParsing();
        ByteBuffer buffer = ByteBuffer.allocate(4098);

        try {
            StringBuilder messageBuilder = new StringBuilder();
            int bytesRead = clientChannel.read(buffer);
	    if(bytesRead == -1){
                System.out.println("Client disconnesso");
                clientChannel.close();
                key.cancel();
                return;
            }
            while (bytesRead > 0) {
                buffer.flip(); //Preparo il buffer per la lettura
                while (buffer.hasRemaining()) {
                    messageBuilder.append((char) buffer.get());
                }
                buffer.clear();
                bytesRead = clientChannel.read(buffer);
            }


            String message = messageBuilder.toString().trim();
            if (!message.isEmpty()) {
                if ("exit".equalsIgnoreCase(message)) {
                    System.out.println("Chiusura del server");
                    clientChannel.close();
                    key.cancel();
                    return;
                }

                //Leggo il messaggio dal client
                System.out.println("Processing message from " + clientChannel.getRemoteAddress() + ": " + message);
                //Converto il messaggio in una stringa
                String convertedMessage = jsonParsing.convertJsonToMessage(message);
                //System.out.println("Converted message: " + convertedMessage);
                //Passo l'operazione e i valori al metodo che si occuperà di gestire la richiesta
                Response response = handleOperation(convertedMessage, clientChannel);

                //riconverto la stringa in formato json per inviarla al client
                String jsonResponse = jsonParsing.createResponse(response.getResponseCode(), response.getErrorMessage(), response.getOrderId(), response.getDayPrices());
                ByteBuffer responseBuffer = ByteBuffer.wrap(jsonResponse.getBytes());
                key.attach(responseBuffer);
                key.interestOps(SelectionKey.OP_WRITE);
                key.selector().wakeup();

                try {
                    while (responseBuffer.hasRemaining()) {
                        clientChannel.write(responseBuffer);
                    }
                } catch (IOException e) {
                    System.err.println("Errore durante l'invio della risposta al client: " + e.getMessage());
                    try {
                        clientChannel.close();
                        key.cancel();
                    } catch (IOException ex) {
                        System.err.println("Errore nella chiusura del canale: " + ex.getMessage());
                    }
                }
                key.interestOps(SelectionKey.OP_READ);
                key.selector().wakeup();
            }
            //}
        } catch (IOException e) {
            System.err.println("Errore nella lettura del messaggio del client: " + e.getMessage());
            try {
                clientChannel.close();
                key.cancel();
                key.attach(null); //Così il riferimento al buffer viene rimosso dal GC quando il client non è più connesso
            } catch (IOException ex) {
                System.err.println("Errore nella chiusura del canale: " + ex.getMessage());
            }
        }

    }

    /**
     * Gestisce le operazioni richieste dal client
     * @param message Messaggio ricevuto dal client
     * @param clientChannel Canale del client
     * @return Response con il risultato dell'operazione
     * @throws IOException Se si verifica un errore di I/O
     */
    private Response handleOperation(String message, Channel clientChannel) throws IOException {
        String[] parts = message.split(" ");
        String operation = parts[0];
        Response response = null;
        String userNameSession = null;
        // Controlla se l'utente è loggato solo per le operazioni che richiedono ordini
        if ("insertLimitOrder".equals(operation) || "insertMarketOrder".equals(operation) || "insertStopOrder".equals(operation)) {
            if (!userDatabase.isUserConnectedToChannel(clientChannel)) {
                return new Response(101, "user not logged in", 0,null);
            }

            userNameSession = userDatabase.getUsernameByChannel(clientChannel);

            if (userNameSession == null) {
                return new Response(101, "user.User not logged in or invalid channel", 0,null);
            }
        }

        switch (operation) {
            case "register":
                String username = parts[1];
                String password = parts[2];
                //Aggiungo l'utente al database
                response = userDatabase.registerUser(username, password);
                updateLastActiveTime(username);
                break;
            case "login":
                username = parts[1];
                password = parts[2];
                //Effettuo il login dell'utente
                response = userDatabase.loginUser(username, password, clientChannel);
                updateLastActiveTime(username);
                break;
            case "logout":
                username = parts[1];
                //Effettuo il logout dell'utente
                response = userDatabase.logoutUser(username,clientChannel);
                break;
            case "updateUserCredentials":
                String oldUsername = parts[1];
                String newUsername = parts[2];
                String newPassword = parts[3];
                //Aggiorno le credenziali dell'utente
                response = userDatabase.updateUserCredentials(oldUsername, newUsername, newPassword);
                updateLastActiveTime(newUsername);
                break;
            case "insertLimitOrder":
                String type = parts[1];
                int size = Integer.parseInt(parts[2]);
                int price = Integer.parseInt(parts[3]);
                //Inserisco l'ordine nel sistema
                response = orderBook.addOrder(new LimitOrder(type, size, price, 0, System.currentTimeMillis(), userDatabase.getUserSession(userNameSession),userNameSession));
                updateLastActiveTime(userNameSession);
                break;
            case "insertMarketOrder":
                type = parts[1];
                size = Integer.parseInt(parts[2]);
                //Inserisco l'ordine di mercato nel sistema
                response = orderBook.addOrder(new MarketOrder(type, size, 0, 0, System.currentTimeMillis(), userDatabase.getUserSession(userNameSession)));
                updateLastActiveTime(userNameSession);
                break;
            case "insertStopOrder":
                type = parts[1];
                size = Integer.parseInt(parts[2]);
                price = Integer.parseInt(parts[3]);
                response = orderBook.addOrder(new StopOrder(type, size, price, 0, System.currentTimeMillis(), userDatabase.getUserSession(userNameSession), userNameSession));
                updateLastActiveTime(userNameSession);
                break;
            case "cancelOrder":
                int orderId = Integer.parseInt(parts[1]);
                String name = userDatabase.getUsernameByChannel(clientChannel);
                updateLastActiveTime(name);
                response = orderBook.cancelOrder(orderId,name);
                break;
            case "getPriceHistory":
                updateLastActiveTime(userDatabase.getUsernameByChannel(clientChannel));
                // Estrarre il mese e l'anno dalla stringa MMYYYY
                String dateString = parts[1];
                int month = Integer.parseInt(dateString.substring(0, 2));
                int year = Integer.parseInt(dateString.substring(2));

                System.out.println("Month: " + month + ", Year: " + year);
                response = orderBook.getPriceHistory(month,year);
                break;
            default:
                response = new Response(103, "Unknown operation: " + operation, 0,null);
        }
        return response;
    }

    /**
     * Aggiorna il tempo dell'ultima attività di un utente
     * @param username Nome utente
     */
    public void updateLastActiveTime(String username) {
        if (username != null) {
            UserSession session = userDatabase.getUserSession(username);
            if (session != null && session.getUser() != null) {
                session.getUser().updateLastActiveTime();
            }
        }
    }


}

