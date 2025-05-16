package order;

import user.UserSession;

public class LimitOrder extends Order {
    /**
     * Rappresenta un ordine limitato, estendendo la classe {@code Order}.
     * Un ordine limitato viene eseguito solo se il prezzo raggiunge il valore specificato.
     *
     * @param type      Il tipo di ordine (buy/sell).
     * @param size      La quantità dell'ordine.
     * @param price     Il prezzo limite dell'ordine.
     * @param orderId   L'identificatore univoco dell'ordine.
     * @param timestamp Il timestamp dell'ordine.
     * @param session   La sessione utente associata all'ordine.
     */
    public LimitOrder(String type, int size, int price, long orderId, long timestamp, UserSession session, String userPropertyName) {
        super(type, size, price, orderId, timestamp,session, userPropertyName);
    }
}
