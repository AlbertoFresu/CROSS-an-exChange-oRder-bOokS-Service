package client;

import java.util.Random;
import java.util.concurrent.*;

public class ClientMainTest {
/*
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(7);


        // Comandi per inserire ordini di acquisto (bid) e di vendita (ask)
        String[] firstCommand = {"login", "user1", "pass1", "insertLimitOrder", "bid", "500", "50000"};   // Ordine di acquisto: 500 unità a 50.000
        String[] secondCommand = {"login", "user2", "pass2", "insertLimitOrder", "ask", "100", "51000"};   // Ordine di vendita: 100 unità a 51.000
        String[] thirdCommand = {"login", "user3", "pass3", "insertLimitOrder", "bid", "200", "50500"};   // Ordine di acquisto: 200 unità a 50.500

        // Aggiungiamo altri ordini per fare del matching
        String[] fourthCommand = {"login", "user4", "pass4", "insertLimitOrder", "ask", "200", "50000"};   // Ordine di vendita: 200 unità a 50.000
        String[] fifthCommand = {"login", "user5", "pass5", "insertLimitOrder", "ask", "100", "50500"};   // Ordine di vendita: 100 unità a 50.500
        String[] sixthCommand = {"login", "user6", "pass6", "insertLimitOrder", "bid", "300", "50500"};   // Ordine di acquisto: 300 unità a 50.500
        String[] seventhCommand = {"login", "user7", "pass7", "insertLimitOrder", "bid", "150", "51000"};   // Ordine di acquisto: 150 unità a 51.000

        // Ordini Limit (normali)
        String[] firstCommand = {"login", "user1", "pass1", "insertLimitOrder", "bid", "400", "49500"};   // Acquisto 400 unità a 49.500
        String[] secondCommand = {"login", "user2", "pass2", "insertLimitOrder", "ask", "150", "50000"};   // Vendita 150 unità a 50.000
        String[] thirdCommand = {"login", "user3", "pass3", "insertLimitOrder", "bid", "250", "50200"};   // Acquisto 250 unità a 50.200

        // Ordini Stop (si attivano a un certo prezzo)
        String[] fourthCommand = {"login", "user4", "pass4", "insertStopOrder", "bid", "300", "49000"};   // Acquisto 300 unità quando il prezzo scende a 49.000
        String[] fifthCommand = {"login", "user5", "pass5", "insertStopOrder", "ask", "200", "50500"};   // Vendita 200 unità quando il prezzo sale a 50.500

        // Ordini che dovrebbero attivare gli StopOrders
        String[] sixthCommand = {"login", "user6", "pass6", "insertLimitOrder", "ask", "300", "48950"};   // Vendita a 48.950, attiva lo Stop Bid di user4
        String[] seventhCommand = {"login", "user7", "pass7", "insertLimitOrder", "bid", "200", "50600"};   // Acquisto a 50.600, attiva lo Stop Ask di user5



        String[] firstCommand = {"login", "user1", "pass1", "insertMarketOrder", "bid", "500", "0"};
        String[] secondCommand = {"login", "user2", "pass2", "insertStopOrder", "ask", "100", "50500", "49000"};
        String[] thirdCommand = {"login", "user3", "pass3", "insertLimitOrder", "bid", "200", "50500"};
        String[] fourthCommand = {"login", "user4", "pass4", "insertLimitOrder", "ask", "300", "49000"};
        String[] fifthCommand = {"login", "user5", "pass5", "insertMarketOrder", "bid", "150", "0"};
        String[] sixthCommand = {"login", "user6", "pass6", "insertStopOrder", "ask", "250", "50500", "51000"};

        executor.execute(() -> executeClient(firstCommand));
        executor.execute(() -> executeClient(secondCommand));
        executor.execute(() -> executeClient(thirdCommand));
        executor.execute(() -> executeClient(fourthCommand));
        executor.execute(() -> executeClient(fifthCommand));
        executor.execute(() -> executeClient(sixthCommand));
        //executor.execute(() -> executeClient(seventhCommand));


        // Chiude l'executor dopo il completamento dei test
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.out.println("Forzatura chiusura dei client ancora attivi...");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.err.println("Interruzione durante l'attesa della terminazione: " + e.getMessage());
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        System.out.println("Tutti i client terminati correttamente.");
    }

    private static void executeClient(String[] commands) {
        try {
            Random random = new Random();
            // Intervallo casuale tra 1 e 5 secondi
            int delay = 1000 + random.nextInt(4000); // Ritardo tra 1 e 5 secondi
            try{
                Thread.sleep(delay);
            }
            catch (InterruptedException e){
                System.err.println("Errore nella sleep del test");
            }

            ClientMain clientMain = new ClientMain(); //commands tolto
            clientMain.startClient();  // Avvia il clientMain per eseguire i comandi
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

 */

}

