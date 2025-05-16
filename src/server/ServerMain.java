package server;

import RMI.UserRegistrationImpl;
import common.ConfigReader;
import order.OrderBook;
import user.UserDatabase;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.*;

/**
 * Classe principale del server che gestisce connessioni TCP e un servizio RMI.
 * Utilizza un thread pool per gestire in modo concorrente le richieste dei client.
 */
public class ServerMain {
    private static final int PORT;
    private static final int RMIPORT ;
    private static final String filePathOrders;
    //Parametri di configurazione della thread pool
    private static final int CORE_POLL_SIZE ; //Minimo numero di thread
    private static final int MAX_POLL_SIZE ; //Massimo numero di thread
    private static final long KEEP_ALIVE_TIME; //Tempo di inattività prima di terminare i thread extra
    private static final int QUEUE_CAPACITY ; //Dimensione massima della coda
    private static final String filePath ;

    private final ThreadPoolExecutor pool;
    private final UserDatabase userDatabase = new UserDatabase(filePath);
    private final OrderBook orderBook = new OrderBook(userDatabase,filePathOrders);

    static {
        ConfigReader configReader = null;
        try (InputStream input = new FileInputStream("config/server.cfg")) {
            // Carica il file di configurazione
            configReader = new ConfigReader(input);
            PORT = configReader.getInt("server.port");
            RMIPORT = configReader.getInt("server.rmiPort");
            CORE_POLL_SIZE = configReader.getInt("server.corePoolSize");
            MAX_POLL_SIZE = configReader.getInt("server.maxPoolSize");
            KEEP_ALIVE_TIME = configReader.getLong("server.keepAliveTime");
            QUEUE_CAPACITY = configReader.getInt("server.queueCapacity");
            filePath = configReader.getString("server.userDatabaseFilePath");
            filePathOrders = configReader.getString("server.orderBookFilePath");
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Errore nel caricamento della configurazione: " + e.getMessage());
        }
    }
    /**
     * Costruttore della classe ServerMain.
     * Inizializza il thread pool con una strategia di gestione della coda che tenta di accodare i task
     * prima di rifiutarli in caso di sovraccarico.
     */
    public ServerMain() {
        //Creo il thread pool personalizzato quando creo l'istanza di server.ServerMain
        pool = new ThreadPoolExecutor(CORE_POLL_SIZE, MAX_POLL_SIZE, KEEP_ALIVE_TIME, TimeUnit.SECONDS, new ArrayBlockingQueue<>(QUEUE_CAPACITY), //Coda limitata
                new RejectedExecutionHandler() {
                    @Override
                    public void rejectedExecution(Runnable r, ThreadPoolExecutor pool) {
                        try {
                            boolean added = pool.getQueue().offer(r, 5, TimeUnit.SECONDS);
                            if (!added) {
                                System.out.println("Task rifiutato: tempo di attesa scaduto");
                            }
                        } catch (InterruptedException e) {
                            System.err.println("Errore nell'inserimento del task nella coda: " + e.getMessage());
                        }
                    }
                });
    }

    /**
     * Metodo principale che avvia il server.
     * Crea un'istanza di ServerMain e avvia i server RMI e TCP.
     */
    public static void main(String[] args) {

        new ServerMain().start();
    }

    /**
     * Avvia sia il server RMI che il server TCP.
     */
    private void start() {
        startRmiServer();
        startTcpServer();
    }

    /**
     * Avvia il server TCP utilizzando un ServerSocketChannel non bloccante.
     * Registra il canale nel Selector per gestire le connessioni in modo asincrono.
     * Accetta nuove connessioni e delega la gestione dei messaggi client al thread pool.
     */
    private void startTcpServer() {
        try (Selector selector = Selector.open(); ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {

            InetSocketAddress address = new InetSocketAddress("localhost", PORT);
            //Configurazione del server socket
            serverSocketChannel.bind(address);
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server in ascolto nella porta " + PORT);

            while (true) {
                selector.select(); //Attende eventi sui canali registrati
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        acceptConnection(selector, key);
                    } else if (key.isReadable()) {
                        //Gestisce i messaggi del client delegandoli al thread pool
                        handleClientMessage(key);
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Errore nel server: " + e.getMessage());
        } finally {
            shutdownThreadPool();
        }
    }

    /**
     * Avvia il server RMI e registra il servizio di registrazione utenti.
     */
    private void startRmiServer() {
        try {
            UserRegistrationImpl userRegistration = new UserRegistrationImpl(userDatabase);
            Registry registry = LocateRegistry.createRegistry(RMIPORT);
            registry.rebind("RMI.UserRegistration", userRegistration);
            System.out.println("Server RMI pronto nella porta: " + RMIPORT);
        } catch (RemoteException e) {
            System.err.println("Errore nell'avvio del server RMI: " + e.getMessage());
        }
    }

    /**
     * Arresta in modo sicuro il thread pool.
     * Attende che i thread completino l'esecuzione prima di forzare l'arresto se necessario.
     */
    private void shutdownThreadPool() {
        //Attendo la terminazione della thread pool in modo safe
        pool.shutdown();
        try {
            if (!pool.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                pool.shutdownNow();
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
        }
    }

    /**
     * Accetta una nuova connessione da un client.
     * Configura il canale del client come non bloccante e lo registra nel Selector per operazioni di lettura.
     */
    private void acceptConnection(Selector selector, SelectionKey key) {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel(); //Recupero il socket channel dalla chiave
        try {
            SocketChannel clientChannel = serverChannel.accept();
            clientChannel.configureBlocking(false);
            clientChannel.socket().setKeepAlive(true); // per controllare se un client è inattivo o se ci sono problemi di rete con il client
            //Registra il canale del client per operazioni di lettura
            clientChannel.register(selector, SelectionKey.OP_READ);
            System.out.println("Nuovo client connesso: " + clientChannel.getRemoteAddress());
        } catch (IOException e) {
            System.err.println("Errore nell'accettazione del server: " + e.getMessage());
        }

    }


    /**
     * Gestisce i messaggi ricevuti dai client.
     * Se ci sono dati disponibili, delega la gestione al thread pool eseguendo un handler specifico.
     */
    private void handleClientMessage(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        if (!key.isValid() || !channel.isOpen()) {
            System.err.println("Chiave selezionata non più valida, chiudo il canale associato");
            closeChannel(key, channel);
            return;
        }

        try {
            int availableBytes = channel.socket().getInputStream().available(); //so quanti byte sono pronti per essere letti
            if (availableBytes > 0) {
                pool.execute(new ServerMessageHandler(key, userDatabase, orderBook));
            }
        } catch (IOException e) {
            System.err.println("Errore nella verifica del canale " + e.getMessage());
            closeChannel(key, channel);
        }


    }

    /**
     * Chiude un canale client non più valido o con errori.
     * Cancella la chiave di selezione e chiude la connessione.
     */
    private void closeChannel(SelectionKey key, SocketChannel channel) {
        try {
            key.cancel();
            channel.close();
        } catch (IOException e) {
            System.err.println("Errore nella chiusura del client, chiave invalida");
        }
    }
}