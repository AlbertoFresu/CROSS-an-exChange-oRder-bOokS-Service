package order;


import user.UserSession;

public class Order{
    private long OrderId;
    private final String type;
    private int size;
    private final int price;
    private final long timestamp;
    private UserSession session;
    private String userPropertyName;

    /**
     * Rappresenta un ordine generico nel sistema di trading.
     * Contiene informazioni come ID ordine, tipo, dimensione, prezzo, timestamp e sessione utente.
     *
     * @param type      Il tipo di ordine (buy/sell).
     * @param size      La quantità dell'ordine.
     * @param price     Il prezzo dell'ordine.
     * @param orderId   L'identificatore univoco dell'ordine.
     * @param timestamp Il timestamp dell'ordine.
     * @param session   La sessione utente associata all'ordine.
     */
    public Order(String type, int size, int price, long orderId, long timestamp, UserSession session,String userPropertyName) {
        this.OrderId = orderId;
        this.type = type;
        this.size = size;
        this.price = price;
        this.timestamp = timestamp;
        this.session = session;
        this.userPropertyName = userPropertyName;
    }

    /**
     * Restituisce il tipo di ordine (buy/sell).
     *
     * @return Il tipo di ordine.
     */
    public String getType() {
        return type;
    }

    /**
     * Restituisce la quantità dell'ordine.
     *
     * @return La quantità dell'ordine.
     */
    public int getSize() {
        return size;
    }

    /**
     * Restituisce il prezzo dell'ordine.
     *
     * @return Il prezzo dell'ordine.
     */
    public int getPrice() {
        return price;
    }

    /**
     * Imposta l'ID dell'ordine.
     *
     * @param orderId Il nuovo identificatore univoco dell'ordine.
     */
    public void setOrderId(long orderId){
        this.OrderId = orderId;
    }

    /**
     * Restituisce l'ID dell'ordine.
     *
     * @return L'identificatore univoco dell'ordine.
     */
    public long getOrderId() {
        return OrderId;
    }

    /**
     * Riduce la quantità dell'ordine di un determinato valore.
     *
     * @param amount L'importo da sottrarre alla quantità dell'ordine.
     */
    public void reduceSize(int amount) {
        this.size -= amount;
    }

    /**
     * Restituisce il timestamp dell'ordine.
     *
     * @return Il timestamp dell'ordine.
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Restituisce la sessione utente associata all'ordine.
     *
     * @return La sessione utente.
     */
    public UserSession getSession() {
        return session;
    }


    public String getUserPropertyName() {
        return userPropertyName;
    }
    /**
     * Confronta due ordini per la coda Ask.
     * Ordina gli ordini in ordine crescente di prezzo e, in caso di parità, per timestamp.
     *
     * @param other L'altro ordine da confrontare.
     * @return Un valore negativo se l'ordine corrente ha un prezzo inferiore,
     *         positivo se ha un prezzo maggiore, 0 se i prezzi sono uguali (ordinati per timestamp).
     */
    public int compareAsk(Order other) {
        int comparedValue = Integer.compare(this.price, other.price);
        if (comparedValue != 0) {
            return comparedValue; //Ordine crescente
        }
        return Long.compare(this.timestamp, other.timestamp);
    }
    /**
     * Confronta due ordini per la coda Bid.
     * Ordina gli ordini in ordine decrescente di prezzo e, in caso di parità, per timestamp.
     *
     * @param other L'altro ordine da confrontare.
     * @return Un valore positivo se l'ordine corrente ha un prezzo inferiore,
     *         negativo se ha un prezzo maggiore, 0 se i prezzi sono uguali (ordinati per timestamp).
     */
    public int compareBid(Order other) {
        int comparedValue = Integer.compare(other.price,this.price);
        if (comparedValue != 0) {
            return comparedValue; //Ordine crescente
        }
        return Long.compare(this.timestamp, other.timestamp);
    }

}
