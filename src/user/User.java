package user;

/**
 * Rappresenta un utente con le informazioni di accesso e il tempo dell'ultima attività.
 * La classe gestisce l'autenticazione e il tracciamento dell'ultima attività dell'utente.
 */
public class User {
    private String username;
    private String password;
    private long lastActiveTime;

    /**
     * Costruttore per inizializzare un nuovo utente con il nome utente e la password.
     * L'ultima attività viene impostata al momento della creazione dell'oggetto.
     *
     * @param username il nome utente dell'utente.
     * @param password la password dell'utente.
     */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.lastActiveTime = System.currentTimeMillis();
    }

    /**
     * Imposta il nome utente dell'utente.
     *
     * @param username il nuovo nome utente da impostare.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Imposta la password dell'utente.
     *
     * @param password la nuova password da impostare.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Restituisce il nome utente dell'utente.
     *
     * @return il nome utente dell'utente.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Restituisce la password dell'utente.
     *
     * @return la password dell'utente.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Restituisce il timestamp dell'ultima attività dell'utente.
     *
     * @return il tempo (in millisecondi) dell'ultima attività dell'utente.
     */
    public long getLastActiveTime() {
        return lastActiveTime;
    }

    /**
     * Aggiorna il timestamp dell'ultima attività dell'utente con il tempo corrente.
     */
    public void updateLastActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }
}
