package order;

import user.UserSession;

/**
 * Rappresenta un ordine di tipo StopOrder, che si attiva quando il prezzo raggiunge un valore specifico (triggerPrice).
 * Estende la classe Order.
 */
public class StopOrder extends Order {
    private final int triggerPrice;

    /**
     * Costruttore per creare un'istanza di StopOrder.
     *
     * @param type         Il tipo di ordine (es. "BUY" o "SELL").
     * @param size         La quantità dell'ordine.
     * @param triggerPrice Il prezzo di attivazione dell'ordine.
     * @param orderId      L'ID univoco dell'ordine.
     * @param timestamp    Il timestamp dell'ordine.
     * @param session      La sessione utente associata all'ordine.
     */
    public StopOrder(String type, int size, int triggerPrice, long orderId, long timestamp, UserSession session, String userPropertyName) {
        super(type, size, triggerPrice, orderId, timestamp,session,userPropertyName);
        this.triggerPrice = triggerPrice;
    }

    /**
     * Restituisce il prezzo di attivazione dell'ordine.
     *
     * @return Il prezzo di attivazione dell'ordine.
     */
    public int getTriggerPrice() {
        return triggerPrice;
    }
}
