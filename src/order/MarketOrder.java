package order;

import user.UserSession;

public class MarketOrder extends Order {

    /**
     * Rappresenta un ordine di mercato, estendendo la classe {@code Order}.
     * Un ordine di mercato viene eseguito immediatamente al miglior prezzo disponibile.
     *
     * @param type      Il tipo di ordine (buy/sell).
     * @param size      La quantità dell'ordine.
     * @param price     Il prezzo indicativo dell'ordine (non utilizzato per l'esecuzione).
     * @param orderId   L'identificatore univoco dell'ordine.
     * @param timestamp Il timestamp dell'ordine.
     * @param session   La sessione utente associata all'ordine.
     */
    public MarketOrder(String type, int size, int price, long orderId, long timestamp, UserSession session) {
        super(type, size, price, orderId, timestamp,session,null);
    }
}
