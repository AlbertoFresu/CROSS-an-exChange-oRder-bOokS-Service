package user;

import common.Response;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Gestisce un database di utenti, incluse operazioni di registrazione, login, disconnessione, e gestione della sessione.
 * La classe mantiene anche un elenco di utenti connessi e gestisce gli utenti inattivi.
 */
public class UserDatabase {
    /**
     * Mappa che contiene gli utenti registrati, indicizzati per username.
     */
    private final Map<String, User> users;
    /**
     * Gestore dei dati utente che si occupa di salvare e caricare gli utenti da un file.
     */
    private final UserDataManager userDataManager;
    /**
     * Mappa concorrente dei canali connessi con le relative sessioni utente.
     */
    private final ConcurrentMap<Channel, UserSession> connectedChannels = new ConcurrentHashMap<Channel, UserSession>();
    /**
     * Mappa concorrente degli utenti loggati, indicizzati per username.
     */
    private final ConcurrentMap<String, UserSession> loggedInUsers = new ConcurrentHashMap<>();
    /**
     * Servizio esecutore pianificato per controllare gli utenti inattivi ogni minuto.
     */
    private ScheduledExecutorService scheduler;


    /**
     * Costruttore che inizializza il database degli utenti e carica i dati da un file.
     * Crea anche un thread per monitorare gli utenti inattivi ogni minuto.
     *
     * @param filePath percorso del file che contiene i dati degli utenti.
     */
    public UserDatabase(String filePath) {
        this.users = new ConcurrentHashMap<>();
        this.userDataManager = new UserDataManager(filePath);
        this.users.putAll(userDataManager.loadUsersFromFile());


        //Creo un thread che controlla gli utenti inattivi ogni minuto
        scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(this::checkInactiveUsers, 0, 1, TimeUnit.MINUTES);
        //aggiungo un shutdown hook per terminare il thread quando il server viene chiuso
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Spegnimento del server, arresto dello scheduler...");
            scheduler.shutdown();
        }));
    }


    /**
     * Salva i dati degli utenti nel file.
     */
    public void saveUsers() {
        userDataManager.saveUsersToFile(new HashMap<>(users));
    }

    /**
     * Aggiunge un canale connesso al database degli utenti.
     *
     * @param channel il canale da aggiungere.
     * @param session la sessione utente associata al canale.
     */
    public void addToConnectedChannel(Channel channel, UserSession session) {
        if (connectedChannels.putIfAbsent(channel, session) != null) {
            System.err.println("Errore nell'inserimento del canale in uso");
        }
    }

    /**
     * Disconnette un canale dal database degli utenti.
     *
     * @param channel il canale da disconnettere.
     */
    public void disconnectFromChannel(Channel channel) {
        if (!connectedChannels.containsKey(channel)) {
            System.err.println("Tentativo di disconnessione di un canale non registrato: " + channel);
            return;
        }
        if (connectedChannels.remove(channel) == null) {
            System.err.println("Errore nella disconnessione del canale in uso");
        }
    }

    /**
     * Restituisce la mappa degli utenti attualmente connessi.
     *
     * @return la mappa degli utenti connessi.
     */
    public ConcurrentMap<String, UserSession> getLoggedInUsers() {
        return loggedInUsers;
    }

    /**
     * Restituisce la mappa dei canali connessi.
     *
     * @return la mappa dei canali connessi.
     */
    public ConcurrentMap<Channel, UserSession> getConnectedChannels() {
        return connectedChannels;
    }

    /**
     * Restituisce la sessione dell'utente associato a un nome utente.
     *
     * @param username il nome utente dell'utente.
     * @return la sessione utente associata, o null se l'utente non è connesso.
     */
    public UserSession getUserSession(String username) {
        return loggedInUsers.get(username);
    }

    /**
     * Registra un nuovo utente nel sistema.
     *
     * @param username il nome utente.
     * @param password la password.
     * @return una risposta che indica il risultato dell'operazione.
     */
    public synchronized Response registerUser(String username, String password) {
        //Controllo se username e valida
        if (username == null || username.isEmpty()) {
            return new Response(103, "Username cannot be empty", 0,null);
        }
        //Controllo se la password è valida
        if (!isPasswordValid(password)) {
            return new Response(103, "Invalid password", 0,null);
        }
        //Controllo se l'utente è già registrato altrimenti lo inserisco
        if (users.putIfAbsent(username, new User(username, password)) != null) {
            return new Response(102, "Username not available", 0,null);
        }
        saveUsers();
        return new Response(100, "OK", 0,null);
    }

    private boolean isPasswordValid(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        // Controlla la lunghezza della password
        if (password.length() < 8) {
            return false;
        }
        // Controlla la presenza di lettere maiuscole, minuscole, numeri e caratteri speciali
        boolean hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Esegue il login di un utente e stabilisce la sua sessione.
     *
     * @param username il nome utente.
     * @param password la password.
     * @param channel il canale di comunicazione utilizzato dall'utente.
     * @return una risposta che indica il risultato dell'operazione.
     * @throws IOException se si verifica un errore durante la gestione del canale.
     */
    public Response loginUser(String username, String password, Channel channel) throws IOException {
        User user = users.get(username);
        //Controllo la validità dell'utente ed ella password
        if (user == null || !user.getPassword().equals(password)) {
            return new Response(101, "username/password mismatch or non existent username", 0,null);
        }
        //Verifico se l'utente è già loggato altrimenti lo inserisco
        SocketChannel userChannel = (SocketChannel) channel;
        SocketAddress socketAddress = userChannel.getRemoteAddress();
        if (socketAddress instanceof InetSocketAddress) {
            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
            UserSession userSession = new UserSession(user, channel, inetSocketAddress);
            UserSession previousSession = loggedInUsers.putIfAbsent(username, userSession);
            if(previousSession != null){
                return new Response(102, "user already logged in", 0,null);
            }
            addToConnectedChannel(channel, userSession);
        }

        user.updateLastActiveTime();
        return new Response(100, "OK", 0,null);
    }

    /**
     * Stampa gli utenti attualmente loggati nel sistema.
     */
    public void printLoggedInUsers() {
        System.out.println("Logged in users:");
        for (String username : loggedInUsers.keySet()) {
            System.out.println(username);
        }
    }

    /**
     * Verifica se un canale è connesso al sistema.
     *
     * @param channel il canale da verificare.
     * @return true se il canale è connesso, false altrimenti.
     */
    public boolean isUserConnectedToChannel(Channel channel) {
        return connectedChannels.containsKey(channel);
    }

    /**
     * Restituisce il nome utente associato a un canale.
     *
     * @param channel il canale di cui ottenere il nome utente.
     * @return il nome utente associato al canale, o null se non esiste.
     */
    public String getUsernameByChannel(Channel channel) {
        UserSession session = connectedChannels.get(channel);
        if (session != null) {
            return session.getUser().getUsername();
        } else {
            return null;
        }
    }

    /**
     * Esegue il logout di un utente.
     *
     * @param username il nome utente da disconnettere.
     * @param channel il canale dell'utente da disconnettere.
     * @return una risposta che indica il risultato dell'operazione.
     */
    public Response logoutUser(String username, Channel channel) {
        disconnectFromChannel(channel);
        if (loggedInUsers.remove(username) != null) {
            return new Response(100, "OK", 0,null);
        } else {
            return new Response(101, "user not logged in", 0,null);
        }

    }

    /**
     * Verifica e rimuove gli utenti inattivi per più di 10 secondi.
     */
    public void checkInactiveUsers() {
        //Controllo se ci sono utenti inattivi da più di 1 minuto
        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<String, UserSession>> iterator = loggedInUsers.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, UserSession> entry = iterator.next();
            String username = entry.getKey();
            UserSession session = entry.getValue();

            if (currentTime - session.getUser().getLastActiveTime() > 50000) {
                System.out.println("Utente " + username + " rimosso per inattività.");
                iterator.remove();

                Channel channel = session.getChannel();
                disconnectFromChannel(channel);


                try {
                    if (channel instanceof SocketChannel) {
                        channel.close();
                    }
                } catch (IOException e) {
                    System.err.println("Errore nella chiusura del canale di " + username + ": " + e.getMessage());
                }

            }
        }
    }

    /**
     * Aggiorna le credenziali dell'utente.
     *
     * @param oldUsername il nome utente esistente.
     * @param newUsername il nuovo nome utente.
     * @param newPassword la nuova password.
     * @return una risposta che indica il risultato dell'operazione.
     */
    public synchronized Response updateUserCredentials(String oldUsername, String newUsername, String newPassword) {

        if (oldUsername == null || oldUsername.isEmpty()) {
            return new Response(102, "username/old_password mismatch or non existent username", 0,null);
        }
        //Controllo se username e  valido
        if (newUsername == null || newUsername.isEmpty()) {
            return new Response(102, "username/old_password mismatch or non existent username", 0,null);
        }
        if (!isPasswordValid(newPassword)) {
            return new Response(103, "Invalid password", 0,null);
        }

        if(loggedInUsers.get(oldUsername) == null){
            return new Response(104, "user currently not logged in", 0,null);
        }
        /*
        if(loggedInUsers.get(oldUsername).equals(oldUsername)){
            return new Response(102, "username/old_password mismatch or non existent username", 0,null);
        }*/

        User existingUser = users.get(oldUsername);
        if (existingUser == null) {
            return new Response(105, "user not registered", 0,null);
        }
        if (users.containsKey(newUsername)) {
            return new Response(105, "username not available", 0,null);
        }
        if (newPassword.equals(existingUser.getPassword())) {
            return new Response(103, "new password equal to old one", 0,null);
        }

        if (!users.remove(oldUsername, existingUser)) {
            return new Response(105, "Error updating user credentials", 0,null);
        }
        existingUser.setUsername(newUsername);
        existingUser.setPassword(newPassword);

        users.put(newUsername, existingUser);
        saveUsers();
        return new Response(100, "OK", 0,null);

    }
}

