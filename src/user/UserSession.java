package user;

import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;

/**
 * La classe UserSession rappresenta la sessione di un utente connesso, associando l'utente a un canale,
 * una chiave di selezione e un indirizzo UDP.
 */
public class UserSession {
    private User user;
    private  Channel channel;
    private  SelectionKey key;
    private InetSocketAddress addressUDP;


    /**
     * Costruttore della classe UserSession.
     * Inizializza la sessione utente con i dati dell'utente, il canale e l'indirizzo UDP.
     *
     * @param user l'utente associato alla sessione.
     * @param channel il canale di rete associato alla sessione.
     * @param addressUDP l'indirizzo UDP dell'utente.
     */
    public UserSession(User user, Channel channel ,InetSocketAddress addressUDP){
        this.user = user;
        this.channel = channel;
        this.addressUDP = addressUDP;
    }

    /**
     * Restituisce l'utente associato alla sessione.
     *
     * @return l'utente associato alla sessione.
     */
    public User getUser() {
        return user;
    }

    /**
     * Restituisce il canale di rete associato alla sessione.
     *
     * @return il canale di rete associato alla sessione.
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Restituisce l'indirizzo UDP dell'utente associato alla sessione.
     *
     * @return l'indirizzo UDP dell'utente.
     */
    public InetSocketAddress getAddressUDP() {
        return addressUDP;
    }

}
