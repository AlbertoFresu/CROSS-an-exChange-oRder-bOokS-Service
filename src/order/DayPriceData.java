package order;

import java.time.LocalDate;

public class DayPriceData {
    private LocalDate date;
    private int openPrice;
    private int closePrice;
    private int maxPrice;
    private int minPrice;

    public DayPriceData(LocalDate date, int openPrice, int closePrice, int maxPrice, int minPrice) {
        this.date = date;
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.maxPrice = maxPrice;
        this.minPrice = minPrice;
    }

    /**
     * Restituisce la data corrispondente ai dati dei prezzi giornalieri.
     *
     * @return La data dei dati di prezzo.
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Restituisce il prezzo di apertura del giorno.
     *
     * @return Il prezzo di apertura.
     */
    public int getOpenPrice() {
        return openPrice;
    }

    /**
     * Restituisce il prezzo di chiusura del giorno.
     *
     * @return Il prezzo di chiusura.
     */
    public int getClosePrice() {
        return closePrice;
    }

    /**
     * Restituisce il prezzo massimo raggiunto durante la giornata.
     *
     * @return Il prezzo massimo.
     */
    public int getMaxPrice() {
        return maxPrice;
    }

    /**
     * Restituisce il prezzo minimo raggiunto durante la giornata.
     *
     * @return Il prezzo minimo.
     */
    public int getMinPrice() {
        return minPrice;
    }
}
