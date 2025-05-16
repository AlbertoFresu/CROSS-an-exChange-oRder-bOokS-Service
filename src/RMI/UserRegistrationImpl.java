package RMI;

import common.Response;
import user.UserDatabase;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implementazione dell'interfaccia {@link UserRegistration} per la registrazione di un utente.
 * Questa classe gestisce la logica di registrazione di un utente, interagendo con il database degli utenti.
 */
public class UserRegistrationImpl extends UnicastRemoteObject implements UserRegistration {
    private final UserDatabase userDatabase;

    /**
     * Costruttore che inizializza l'oggetto UserRegistrationImpl con il database degli utenti.
     *
     * @param userDatabase il database degli utenti da utilizzare per la registrazione.
     * @throws RemoteException se si verifica un errore durante l'inizializzazione dell'oggetto remoto.
     */
    public UserRegistrationImpl(UserDatabase userDatabase) throws RemoteException {
        super();
        this.userDatabase = userDatabase;
    }

    /**
     * Implementazione del metodo per registrare un nuovo utente.
     *
     * @param username il nome utente da registrare.
     * @param password la password dell'utente.
     * @return un oggetto {@link Response} che contiene il risultato dell'operazione di registrazione.
     * @throws RemoteException se si verifica un errore durante la comunicazione remota.
     */
    @Override
    public Response registerUser(String username, String password) throws RemoteException {
        return userDatabase.registerUser(username, password);
    }
}
