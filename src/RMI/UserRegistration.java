package RMI;

import common.Response;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia remota per la registrazione di un utente.
 * Questa interfaccia permette di registrare un utente tramite un nome utente e una password.
 */
public interface UserRegistration extends Remote {
    /**
     * Metodo remoto per registrare un nuovo utente.
     *
     * @param username il nome utente da registrare.
     * @param password la password dell'utente.
     * @return un oggetto {@link Response} che contiene il risultato dell'operazione di registrazione.
     * @throws RemoteException se si verifica un errore durante la comunicazione remota.
     */
    Response registerUser(String username, String password) throws RemoteException;
}
