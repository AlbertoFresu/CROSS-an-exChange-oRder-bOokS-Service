package common;

import order.DayPriceData;

import java.io.Serializable;
import java.util.List;

/**
 * Rappresenta una risposta che può contenere un codice di risposta, un messaggio di errore,
 * un identificatore di ordine e una lista di dati relativi ai prezzi giornalieri.
 *
 * @see DayPriceData
 */
public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private int responseCode;
    private String errorMessage;
    private long orderId;
    private List<DayPriceData> dayPrices;

    /**
     * Costruisce un'istanza di {@code Response} con i dati specificati.
     *
     * @param responseCode Il codice di risposta della richiesta.
     * @param errorMessage Il messaggio di errore (se presente).
     * @param orderId L'ID dell'ordine associato alla risposta.
     * @param dayPrices Una lista di dati relativi ai prezzi giornalieri.
     */
    public Response(int responseCode, String errorMessage, long orderId, List<DayPriceData> dayPrices){
        this.responseCode = responseCode;
        this.errorMessage = errorMessage;
        this.orderId = orderId;
        this.dayPrices = dayPrices;
    }

    /**
     * Restituisce il codice di risposta della richiesta.
     *
     * @return Il codice di risposta.
     */
    public int getResponseCode() {
        return responseCode;
    }

    /**
     * Restituisce il messaggio di errore associato alla risposta, se presente.
     *
     * @return Il messaggio di errore, oppure {@code null} se non presente.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Restituisce l'ID dell'ordine associato alla risposta.
     *
     * @return L'ID dell'ordine.
     */
    public long getOrderId() {
        return orderId;
    }

    /**
     * Restituisce la lista dei dati relativi ai prezzi giornalieri.
     *
     * @return Una lista di {@code DayPriceData}.
     */
    public List<DayPriceData> getDayPrices() {
        return dayPrices;
    }
}
