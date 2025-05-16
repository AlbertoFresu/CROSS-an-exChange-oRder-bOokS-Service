package client;

import common.ConfigReader;
import common.JsonParsing;
import common.Response;
import RMI.UserRegistration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ClientMain {
    private static final String SERVER_ADDRESS;
    private static final int SERVER_PORT;
    private static final int RMIPORT;
    private static final int MAX_THREADS;
    private static final long SHOUTDOWN_TIMEOUT;
    private static NotificationReceiverUDP notificationReceiverUDP;
    private static NotificationReceiverMulticastUDP multicastUDP;

    private static int tryReconnect;
    private static int INTERVAL_RECONNECT;
    //private Scanner scanner; //Aggiunto per testing
    private SocketChannel clientChannel;

    static{
        ConfigReader configReader = null;
        try (InputStream input = new FileInputStream("config/client.cfg")) {
            configReader = new ConfigReader(input);
            SERVER_ADDRESS = configReader.getString("server.address");
            SERVER_PORT = configReader.getInt("server.port");
            RMIPORT = configReader.getInt("rmi.port");
            MAX_THREADS = configReader.getInt("max.threads");
            SHOUTDOWN_TIMEOUT = configReader.getInt("shutdown.timeout");
            tryReconnect = configReader.getInt("max.reconnect.attempts");
            INTERVAL_RECONNECT = configReader.getInt("reconnect.interval");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS);; // Executor per gestire i thread

    public static void main(String[] args){
        startTPC();
    }


    /*public ClientMain() {
        this.scanner = new Scanner(System.in);
        this.executorService = Executors.newFixedThreadPool(MAX_THREADS);
    }*/

    /*
    // Nuovo costruttore che accetta comandi predefiniti
    public ClientMain(String[] commands) {
        this.executorService = Executors.newFixedThreadPool(MAX_THREADS);
        StringBuilder input = new StringBuilder();
        for (String command : commands) {
            input.append(command).append("\n"); // Simula l'input utente
        }
        this.scanner = new Scanner(input.toString());
    }*/

    /**
     * Avvia il client stabilendo una connessione con il server.
     * Se la connessione al canale non è aperta, viene avviato il metodo startTPC.
     */
    public void startClient() {
        if (clientChannel == null || !clientChannel.isOpen()) {
            startTPC();
        }
        //startTPC(scanner); // Passiamo lo scanner al metodo
    }

    /**
     * Avvia la ricezione di notifiche tramite UDP sulla porta locale specificata.
     * @param localPort La porta locale su cui ascoltare per le notifiche.
     */
    public static void startUDP(int localPort) {
        notificationReceiverUDP = new NotificationReceiverUDP(false, localPort);
        executorService.submit(notificationReceiverUDP);
    }

    /**
     * Avvia la ricezione di notifiche multicast UDP.
     */
    public static void startMulticast() {
        multicastUDP = new NotificationReceiverMulticastUDP();
        executorService.submit(multicastUDP);
    }

    /**
     * Arresta la ricezione di notifiche multicast UDP.
     */
    public static void stopMulticast() {
        if (multicastUDP != null) {
            multicastUDP.stopRunning();
        }
    }

    /**
     * Arresta la ricezione di notifiche tramite UDP.
     */
    public static void stopUDP() {
        if (notificationReceiverUDP != null) {
            notificationReceiverUDP.terminate();
        }
    }

    /**
     * Avvia una connessione TCP con il server. Tenta di connettersi fino a un numero massimo di tentativi.
     * Gestisce la lettura e la scrittura dal server utilizzando un selettore non bloccante.
     */
    public static void startTPC() {//Scanner aggiunto hai parametri per testing
        int attempts = 0;
        boolean connected = false;
        //Creazione dello scanner per leggere l'input dell'utente
        Scanner scanner = new Scanner(System.in);
        //Creazione del parser JSON
        JsonParsing jsonParsing = new JsonParsing();

        //Creazione del canale e del selettore
        while (attempts < tryReconnect && !connected) {
            try (SocketChannel clientChannel = SocketChannel.open();
                 Selector selector = Selector.open();) {
                //Controllo se il canale è aperto
                InetSocketAddress address = new InetSocketAddress(SERVER_ADDRESS, SERVER_PORT);
                //Connessione al server
                //Imposto il canale in modalità non bloccante
                clientChannel.configureBlocking(false);
                clientChannel.connect(address);
                while (!clientChannel.finishConnect()) {
                    System.out.println("Connessione in corso...");
                }

                // Recupera la porta locale del client
                int localPort = ((InetSocketAddress) clientChannel.getLocalAddress()).getPort();
                System.out.println("Il client è connesso alla porta TPC: " + localPort);
                //voglio generare delle porte in un range per l'ascolto

                startUDP(localPort);
                //Registrazione del canale al selettore
                SelectionKey key = clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                //Creazione del buffer
                ByteBuffer buffer = ByteBuffer.allocate(4098);
                //Attacco il buffer alla chiave
                key.attach(buffer);
                //System.out.println("ClientMain connesso al server");

                while (true) {
                    selector.select(); //Operazione bloccante
                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectedKeys.iterator();

                    //Iterazione delle chiavi
                    while (iterator.hasNext()) {
                        key = iterator.next();
                        //Rimuove la chiave dall'insieme
                        iterator.remove();

                        //Controllo se la chiave è valida
                        if (key.isReadable()) {
                            if (readFromServer(key, jsonParsing, clientChannel)) {
                                break;
                            }
                        }
                        //Controllo se la chiave è scrivibile
                        if (key.isWritable()) {

                            if (writeToServer(scanner, jsonParsing, clientChannel, key)) {
                                return;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                attempts++;
                System.err.println("Tentativo di connessione " + attempts + " fallito: " + e.getMessage());

                if (attempts < tryReconnect) {
                    System.out.println("Riprovo la connessione tra 5 secondi...");
                    try {
                        Thread.sleep(INTERVAL_RECONNECT);
                    } catch (InterruptedException ignored) {
                    }
                } else {
                    System.err.println("Impossibile connettersi al server dopo " + tryReconnect + " tentativi. Chiusura del client.");
                    shutdownClient(null, null, scanner);
                    return;
                }
            }


        }
    }

    /**
     * Termina correttamente la connessione del client, chiudendo il canale e il selettore,
     * arrestando i thread di notifica e multicast, e chiudendo l'esecutore.
     * @param clientChannel Il canale di comunicazione con il server.
     * @param selector Il selettore associato al canale.
     * @param scanner L'oggetto scanner utilizzato per l'input dell'utente.
     */
    public static void shutdownClient(SocketChannel clientChannel, Selector selector, Scanner scanner) {
        // Chiudi il canale se è ancora aperto
        if (clientChannel != null) {
            try {
                if (clientChannel.isOpen()) {
                    clientChannel.close();
                    //System.out.println("✅ SocketChannel chiuso.");
                }
            } catch (IOException e) {
                System.err.println("Errore nella chiusura del canale TCP: " + e.getMessage());
            }
        }

        if (selector != null && selector.isOpen()) {
            try {
                selector.close();
            } catch (IOException e) {
                System.err.println("Errore nella chiusura del selettore: " + e.getMessage());
            }
        }

        if (scanner != null) {
            scanner.close();
        }


        stopUDP();
        stopMulticast();


        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(SHOUTDOWN_TIMEOUT, TimeUnit.SECONDS)) {
                    System.err.println("Timeout nella chiusura del thread multicast, forzando shutdown.");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                System.err.println("Errore nell'attesa della terminazione del multicast: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("ClientMain chiuso correttamente.");
        System.exit(1);
    }

    /**
     * Gestisce la scrittura di un messaggio dal client al server. Il messaggio viene inviato solo se non è vuoto.
     * Se l'operazione non è di tipo "register", invia i dati al server e attende la risposta.
     * @param scanner L'oggetto scanner per la lettura dell'input.
     * @param jsonParsing L'oggetto per la conversione dei messaggi in formato JSON.
     * @param clientChannel Il canale di comunicazione con il server.
     * @param key La chiave associata al canale di comunicazione.
     * @return true se l'operazione è terminata, false altrimenti.
     * @throws IOException Se si verifica un errore durante la scrittura.
     */
    public static boolean writeToServer(Scanner scanner, JsonParsing jsonParsing, SocketChannel clientChannel, SelectionKey key) throws IOException {
        /*
        if (!scanner.hasNextLine()) {
            return true;
        }*/
        ByteBuffer buffer;
        //Gestione dell'input dell'utente
        String jsonMessage = handleUserInput(scanner, jsonParsing);
        if (jsonMessage == null) {
            shutdownClient(clientChannel, null, scanner);
            return true;
        }
        //Recupero il buffer associato alla chiave
        buffer = (ByteBuffer) key.attachment();

        //Pulisco il buffer
        buffer.clear();
        if (!jsonMessage.isEmpty() && !jsonMessage.contains("\"operation\":register\"")) {
            //Invia il messaggio al server
            buffer.put(jsonMessage.getBytes());
            buffer.flip();
            clientChannel.write(buffer);
            while (buffer.hasRemaining()) {
                //Scrive i dati rimanenti nel buffer
                clientChannel.write(buffer);
            }
            key.interestOps(SelectionKey.OP_READ); // Cambia l'interesse a leggere
        }
        return false;
    }

    /**
     * Gestisce la lettura della risposta dal server. Se il server è disconnesso, il client verrà arrestato.
     * La risposta viene poi stampata sulla console.
     * @param key La chiave associata al canale di comunicazione.
     * @param jsonParsing L'oggetto per la conversione dei dati in formato stringa.
     * @param clientChannel Il canale di comunicazione con il server.
     * @return true se la connessione con il server è stata chiusa, false altrimenti.
     * @throws IOException Se si verifica un errore durante la lettura.
     */
    public static boolean readFromServer(SelectionKey key, JsonParsing jsonParsing, SocketChannel clientChannel) throws IOException {
        ByteBuffer buffer;
        //Recupero il buffer associato alla chiave
        buffer = (ByteBuffer) key.attachment();
        //Pulisco il buffer
        buffer.clear();
        //Leggo i dati dal server
        int bytesRead = clientChannel.read(buffer);
        //Controllo se il server è disconnesso
        if (bytesRead == -1) {
            System.out.println("Server disconnesso");
            shutdownClient(clientChannel, null, null);
            return true;
        }
        if (bytesRead > 0) {
            //Leggo i dati dal buffer
            buffer.flip();
            //Creo un array di byte per contenere i dati
            byte[] data = new byte[buffer.limit()];
            //Leggo i dati dal buffer
            buffer.get(data);
            // Stampa la risposta del server
            String parsedMessage = jsonParsing.convertResponseToString(new String(data));
            System.out.println(parsedMessage);
            key.interestOps(SelectionKey.OP_WRITE); // Cambia l'interesse a scrivere

        }

        return false;
    }

    /**
     * Gestisce l'input dell'utente, creando il messaggio JSON corrispondente all'operazione selezionata.
     * Supporta operazioni come la registrazione, il login, il logout e altre operazioni di trading.
     * @param scanner L'oggetto scanner per la lettura dell'input.
     * @param jsonParsing L'oggetto per la conversione dei messaggi in formato JSON.
     * @return Il messaggio JSON da inviare al server, o null se l'operazione è "exit".
     */
    public static String handleUserInput(Scanner scanner, JsonParsing jsonParsing) {

        System.out.print("Inserisci l'operazione da effettuare (ricorda! prima devi essere registrato) : ");
        System.out.print("\n");
        System.out.flush();

        /*
        if (!scanner.hasNextLine()) {

            shutdownClient(null, null, null);
            return null;
        }*/
        String operation = scanner.nextLine();
        String jsonMessage = null;
        switch (operation) {
            case "register":
                System.out.print("Inserisci username: ");
                String username = scanner.nextLine();
                System.out.print("Inserisci password: ");
                String password = scanner.nextLine();

                return RegistrationRmi(username, password);
            case "login":
                System.out.print("Inserisci username: ");
                username = scanner.nextLine();
                System.out.print("Inserisci password: ");
                password = scanner.nextLine();
                jsonMessage = jsonParsing.convertMessageToJson(operation, username, password);
                startMulticast();
                break;
            case "logout":
                System.out.print("Inserisci username: ");
                username = scanner.nextLine();
                jsonMessage = jsonParsing.convertMessageToJson(operation, username);
                break;
            case "updateUserCredentials":
                System.out.print("Inserisci vecchio username: ");
                String oldUsername = scanner.nextLine();
                System.out.print("Inserisci nuovo username: ");
                String newUsername = scanner.nextLine();
                System.out.print("Inserisci nuova password: ");
                String newPassword = scanner.nextLine();
                jsonMessage = jsonParsing.convertMessageToJson(operation, oldUsername, newUsername, newPassword);
                break;
            case "insertLimitOrder":
            case "insertStopOrder":
                System.out.print("Inserisci tipo (ask/bid): ");
                String type = scanner.nextLine();
                if (!type.equals("ask") && !type.equals("bid")) {
                    System.out.println("must be ask/bid !");
                    jsonMessage = "";
                    break;
                }
                System.out.print("Inserisci dimensione: ");
                int size = Integer.parseInt(scanner.nextLine());
                System.out.print("Inserisci prezzo: ");
                int price = Integer.parseInt(scanner.nextLine());
                jsonMessage = jsonParsing.convertMessageToJson(operation, type, String.valueOf(size), String.valueOf(price));
                break;
            case "insertMarketOrder":
                System.out.print("Inserisci tipo (ask/bid): ");
                type = scanner.nextLine();
                if (!type.equals("ask") && !type.equals("bid")) {
                    System.out.println("must be ask/bid !");
                    jsonMessage = "";
                    break;
                }
                System.out.print("Inserisci dimensione: ");
                size = Integer.parseInt(scanner.nextLine());
                jsonMessage = jsonParsing.convertMessageToJson(operation, type, String.valueOf(size));
                break;
            case "cancelOrder":
                System.out.print("Inserisci id ordine: ");
                int orderId = Integer.parseInt(scanner.nextLine());
                jsonMessage = jsonParsing.convertMessageToJson(operation, String.valueOf(orderId));
                break;
            case "getPriceHistory":
                System.out.print("Inserisci mese: ");
                String month = scanner.nextLine();
                jsonMessage = jsonParsing.convertMessageToJson(operation, month);
                break;
            case "exit":
                System.out.println("Chiusura del ClientMain");
                shutdownClient(null, null, scanner);
                return null;
            default:
                System.out.println("Operazione sconosciuta");
                return "";
        }
        return jsonMessage;
    }

    /**
     * Esegue la registrazione dell'utente tramite RMI, utilizzando un'operazione remota per registrare l'utente con nome utente e password.
     * @param username Il nome utente da registrare.
     * @param password La password associata all'utente.
     * @return La risposta in formato JSON per la registrazione.
     */
    private static String RegistrationRmi(String username, String password) {
        //Eseguo la registrazione tramite RMI
        try {
            //Recupero il registro RMI
            Registry registry = LocateRegistry.getRegistry(SERVER_ADDRESS, RMIPORT);
            //Recupero l'oggetto remoto
            UserRegistration userRegistration = (UserRegistration) registry.lookup("RMI.UserRegistration");
            //Eseguo la registrazione dell'utente tramite metodo remoto
            Response response = userRegistration.registerUser(username, password);
            System.out.println("RESPONSE: " + response.getResponseCode() + ", MESSAGE: " + response.getErrorMessage());

        } catch (RemoteException | NotBoundException e) {
            System.err.println("Errore nella registrazione dell'utente: " + e.getMessage());
            e.printStackTrace();
        }

        //break; non va bene perché farebbe uscire dallo switch restituendo null
        //questo causa l'uscita dal while(true) del main chiudendo il client
        //restituisco una stringa che controlla se siamo nella fase di register oppure no
        return "{\"operation\":register\"}";
    }
}
