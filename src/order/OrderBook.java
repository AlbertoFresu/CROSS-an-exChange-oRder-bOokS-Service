package order;

import com.google.gson.*;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import common.JsonParsing;
import common.Response;
import server.NotificationSender;
import user.UserDatabase;
import user.UserSession;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Classe OrderBook - Gestisce gli ordini di acquisto e vendita, il matching e l'invio delle notifiche.
 */
public class OrderBook {
    private NotificationSender notificationSender;
  
    private static int THRESHOLD = 40000;
    private final Queue<Order> bidOrders = new PriorityQueue<Order>(Order::compareBid); // da negare
    private final Queue<Order> askOrders = new PriorityQueue<Order>(Order::compareAsk); // ordinamento normale
    private final Queue<StopOrder> stopBuyOrders = new PriorityQueue<>(new Comparator<StopOrder>() {
        @Override
        public int compare(StopOrder o1, StopOrder o2) {
            return Integer.compare(o1.getTriggerPrice(), o2.getTriggerPrice());
        }
    });
    private final Queue<StopOrder> stopSellOrders = new PriorityQueue<>(new Comparator<StopOrder>() {
        @Override
        public int compare(StopOrder o1, StopOrder o2) {
            return Integer.compare(o2.getTriggerPrice(), o1.getTriggerPrice());
        }
    });
    private final Map<Long, Order> activeOrders;
    private final AtomicLong orderIdGenerator = new AtomicLong(1);

    private final UserDatabase userDatabase;
    private JsonParsing jsonParsing;
	private static String filePathOrders;

    /**
     * Costruttore della classe OrderBook.
     * Inizializza le code di priorità e il gestore delle notifiche.
     *
     * @throws IOException se la creazione del NotificationSender fallisce.
     */
    public OrderBook(UserDatabase userDatabase, String filePathOrders) {
        activeOrders = new ConcurrentHashMap<>();
        this.jsonParsing = new JsonParsing();
        this.filePathOrders = filePathOrders;
        this.userDatabase = userDatabase;
        try {
            this.notificationSender = new NotificationSender();
        } catch (SocketException e) {
            System.err.println("Errore nella creazione di NotificationSender");
        }
    }


    /**
     * Carica e restituisce una lista di ordini (Order) da un file JSON.
     *
     * @return Una lista di oggetti Order caricati dal file "storicoOrdini.json".
     *         Se si verifica un errore o il file non è nel formato corretto,
     *         restituisce una lista vuota.
     */
    public synchronized static List<Order> loadOrders() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Order.class, new OrderAdapter())//Registra l'adapter personalizzato
                .create();
        try (FileReader reader = new FileReader(filePathOrders)) {
            JsonElement jsonElement = JsonParser.parseReader(reader);
            if (jsonElement.isJsonObject()) {
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                if (jsonObject.has("trades")) {
                    JsonArray tradesArray = jsonObject.getAsJsonArray(("trades"));
                    return gson.fromJson(tradesArray, new TypeToken<ArrayList<Order>>() {
                    }.getType());
                } else {
                    System.err.println("ERRORE: Il JSON non contiene il campo 'trades'!");
                }
            } else {
                System.err.println("ERRORE: Il file JSON non è nel formato corretto!");
            }
        } catch (IOException e) {
            System.err.println("Errore nella restituzione della lista di ordini per getHistoryPrice" + e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Aggiunge un nuovo ordine al book e tenta di eseguirlo se possibile.
     *
     * @param order L'ordine da aggiungere.
     */
    public synchronized Response addOrder(Order order) {
        if (order instanceof MarketOrder) {
            return insertMarketOrder((MarketOrder) order);
        } else if (order instanceof LimitOrder) {
            return insertLimitOrder((LimitOrder) order);
        } else if (order instanceof StopOrder) {
            return insertStopOrder((StopOrder) order);
        }
        return new Response(400, "ordine non riconosciuto", 0, null);//Ordine non riconosciuto
    }

    /**
     * Inserisce un ordine stop nel book e lo monitora fino a quando il prezzo raggiunge la soglia stabilita.
     * Se il tipo di ordine è "bid", viene aggiunto alla coda degli ordini di acquisto stop;
     * altrimenti, viene aggiunto alla coda degli ordini di vendita stop.
     *
     * @param order L'ordine stop da inserire.
     * @return Un oggetto Response contenente l'ID del nuovo ordine.
     */
    public synchronized Response insertStopOrder(StopOrder order) {
        order.getSession().getUser().updateLastActiveTime();
        //Gli order.StopOrder vengono monitorati ma attivati solo quando il prezzo raggiunge la soglia
        long newOrderId = orderIdGenerator.getAndIncrement();
        order.setOrderId(newOrderId);
        activeOrders.put(newOrderId, order);
        if (order.getType().equals("bid")) {

            stopBuyOrders.offer(order);
            processBidStopOrders();
        } else {
            stopSellOrders.offer(order);
            processAskStopOrder();
        }
        return new Response(0, null, order.getOrderId(), null);
    }

    /**
     * Elabora gli ordini di vendita stop (stop sell orders).
     * Se il miglior prezzo di acquisto corrente è maggiore o uguale al prezzo dell'ordine stop di vendita,
     * viene eseguito il matching degli ordini.
     * L'operazione riduce la quantità disponibile in entrambi gli ordini e li registra nella cronologia.
     * Se un ordine viene completamente esaurito, viene rimosso dal book.
     */
    private void processAskStopOrder() {
        Order bestBid = bidOrders.peek();
        Order bestAskStopOrder = stopSellOrders.peek();


        if (bestAskStopOrder == null) {
            return;
        }
        if ((bestBid != null) && (bestBid.getPrice() >= bestAskStopOrder.getPrice())) {
            int matchedSize = getMatchedSize(bestBid, bestAskStopOrder);
            bestBid.reduceSize(matchedSize);
            bestAskStopOrder.reduceSize(matchedSize);

            System.out.printf("Matched %d BTC at price %d USD\n", matchedSize, bestAskStopOrder.getPrice());
            Order orderBid = new StopOrder(bestBid.getType(), matchedSize, bestBid.getPrice(), bestAskStopOrder.getOrderId(), System.currentTimeMillis(), bestBid.getSession(),null);
            Order orderAsk = new StopOrder(bestAskStopOrder.getType(), matchedSize, bestAskStopOrder.getPrice(), bestAskStopOrder.getOrderId(), System.currentTimeMillis(), bestAskStopOrder.getSession(),null);
            jsonParsing.addOrderToHistory(orderBid);
            jsonParsing.addOrderToHistory(orderAsk);
            notifyClient(orderBid, "stop");
            notifyClient(orderAsk, "stop");
            sendNotificationMulticast(orderAsk, THRESHOLD);

            if (bestBid.getSize() == 0) {
                activeOrders.remove(bestBid.getOrderId());
                bidOrders.poll();
            }
            if (bestAskStopOrder.getSize() == 0) {
                activeOrders.remove(bestAskStopOrder.getOrderId());
                stopSellOrders.poll();
            }
        }
    }

    /**
     * Elabora gli ordini di acquisto stop (stop buy orders).
     * Se il miglior prezzo di vendita corrente è minore o uguale al prezzo dell'ordine stop di acquisto,
     * viene eseguito il matching degli ordini.
     * L'operazione riduce la quantità disponibile in entrambi gli ordini e li registra nella cronologia.
     * Se un ordine viene completamente esaurito, viene rimosso dal book.
     */
    private void processBidStopOrders() {
        Order bestAsk = askOrders.peek();
        Order bestBidStopOrder = stopBuyOrders.peek();

        if (bestBidStopOrder == null) {
            return;
        }
        if (bestAsk != null && bestBidStopOrder.getPrice() >= bestAsk.getPrice()) {

            int matchedSize = getMatchedSize(bestBidStopOrder, bestAsk);
            bestBidStopOrder.reduceSize(matchedSize);
            bestAsk.reduceSize(matchedSize);

            System.out.printf("Matched %d BTC at price %d USD\n", matchedSize, bestAsk.getPrice());
            Order orderAsk = new StopOrder(bestAsk.getType(), matchedSize, bestAsk.getPrice(), bestAsk.getOrderId(), System.currentTimeMillis(), bestAsk.getSession(),null);
            Order orderBid = new StopOrder(bestBidStopOrder.getType(), matchedSize, bestBidStopOrder.getPrice(), bestBidStopOrder.getOrderId(), System.currentTimeMillis(), bestBidStopOrder.getSession(),null);
            jsonParsing.addOrderToHistory(orderBid);
            jsonParsing.addOrderToHistory(orderAsk);
            notifyClient(orderBid, "stop");
            notifyClient(orderAsk, "stop");
            sendNotificationMulticast(orderAsk, THRESHOLD);

            if (bestBidStopOrder.getSize() == 0) {
                activeOrders.remove(bestBidStopOrder.getOrderId());
                stopBuyOrders.poll();
            }
            if (bestAsk.getSize() == 0) {
                activeOrders.remove(bestAsk.getOrderId());
                askOrders.poll();
            }
        }
    }

    /**
     * Inserisce un ordine limit nel book degli ordini attivi e tenta di abbinarlo immediatamente se possibile.
     * Se il book è vuoto, l'ordine viene aggiunto e il matching viene elaborato.
     * Se il nuovo ordine diventa il migliore nel book, si tenta di eseguire il matching
     * e di processare eventuali ordini stop corrispondenti.
     *
     * @param order L'ordine limit da inserire.
     * @return Un oggetto Response contenente l'ID dell'ordine appena inserito.
     */

    public synchronized Response insertLimitOrder(LimitOrder order) {
        long newOrderId = orderIdGenerator.getAndIncrement();
        order.setOrderId(newOrderId);

        activeOrders.put(newOrderId, order);
        if (order.getType().equals("bid")) {

            if (bidOrders.isEmpty()) {
                bidOrders.offer(order);
                processMatching(order);
                processAskStopOrder();
                return new Response(0, null, order.getOrderId(), null);
            }
            long id = bidOrders.peek().getOrderId();
            bidOrders.offer(order);
            if (bidOrders.peek().getOrderId() != id) {
                processMatching(order);
                //Controllo se ci sono degli ordini StopBid da evadere con stopPrice conveniente
                processAskStopOrder();
            }
        } else if (order.getType().equals("ask")) {
            if (askOrders.isEmpty()) {
                askOrders.offer(order);
                processMatching(order);
                processBidStopOrders();
                return new Response(0, null, order.getOrderId(), null);
            }

            long id = askOrders.peek().getOrderId();
            askOrders.offer(order);
            if (askOrders.peek().getOrderId() != id) {
                processMatching(order);
                //Controllo se ci sono degli ordini StopAsk da evadere con stopPrice conveniente
                processBidStopOrders();
            }
        }
        return new Response(0, null, order.getOrderId(), null);
    }

    /**
     * Inserisce un ordine di mercato e lo abbina immediatamente contro gli ordini disponibili nel book.
     * Se il book è vuoto o non c'è abbastanza liquidità per soddisfare completamente l'ordine,
     * restituisce un errore.
     * Gli ordini vengono ridotti man mano che vengono abbinati e salvati nello storico.
     * Se un ordine viene completamente esaurito, viene rimosso dal book.
     *
     * @param order L'ordine di mercato da eseguire.
     * @return Un oggetto Response che indica se l'ordine è stato completamente abbinato o meno.
     */

    public synchronized Response insertMarketOrder(MarketOrder order) {
        order.getSession().getUser().updateLastActiveTime();
        Queue<Order> targetQueue;
        if (order.getType().equals("bid")) {
            targetQueue = askOrders;
        } else {
            targetQueue = bidOrders;
        }
        int sum = 0;
        if (targetQueue.isEmpty()) {
            return new Response(0, null, -1, null);
        }
        //controllo se è possibile fare un marketOrder con tutti gli elementi nella coda
        for (Order order_ : targetQueue) {
            sum += order_.getSize();
        }
        if (sum < order.getSize()) {
            return new Response(0, null, -1, null);
        }
        int remainingSize = order.getSize();
        long newOrderId = orderIdGenerator.getAndIncrement(); // Generazione dell'ID prima del matching
        order.setOrderId(newOrderId);
        //timestamp = System.currentTimeMillis()
        while (remainingSize > 0) {
            Order bestOrder = targetQueue.peek();


            int matchedSize = Math.min(remainingSize, bestOrder.getSize());
            remainingSize -= matchedSize;
            bestOrder.reduceSize(matchedSize);

            //inserisco ordine market order nello storico
            System.out.printf("order.Order %d matched with %d BTC at price %d USD\n", order.getOrderId(), matchedSize, bestOrder.getPrice());
            Order orderToSave = new MarketOrder(order.getType(), matchedSize, bestOrder.getPrice(), newOrderId, System.currentTimeMillis(), order.getSession());
            Order orderToSend = new MarketOrder(order.getType(), matchedSize, bestOrder.getPrice(), newOrderId, System.currentTimeMillis(), bestOrder.getSession());
            jsonParsing.addOrderToHistory(orderToSave);

            notifyClient(orderToSave, "market");
            notifyClient(orderToSend, "market");
            if (orderToSave.getType().equals("ask")) {
                sendNotificationMulticast(orderToSave, THRESHOLD);
            } else if (orderToSend.getType().equals("ask")) {
                sendNotificationMulticast(orderToSend, THRESHOLD);
            }

            if (bestOrder.getSize() == 0) {
                activeOrders.remove(bestOrder.getOrderId());
                targetQueue.poll();
            }


        }
        return new Response(100, "Market order fully matched", order.getOrderId(), null);

    }

    /**
     * Esegue il matching tra gli ordini nel book, abbinando i migliori bid e ask disponibili.
     * Se il prezzo del miglior bid è maggiore o uguale a quello del miglior ask, gli ordini vengono eseguiti
     * e registrati nello storico. Gli ordini esauriti vengono rimossi dal book.
     *
     * @param orderToProcess L'ordine che ha innescato il processo di matching.
     */
    public synchronized void processMatching(Order orderToProcess) {
        while (!bidOrders.isEmpty() && !askOrders.isEmpty()) {
            Order bestBid = bidOrders.peek();
            Order bestAsk = askOrders.peek();

            if (bestBid.getPrice() >= bestAsk.getPrice()) {
                int matchedSize = getMatchedSize(bestBid, bestAsk);
                bestBid.reduceSize(matchedSize);
                bestAsk.reduceSize(matchedSize);

                System.out.printf("Matched %d BTC at price %d USD\n", matchedSize, bestAsk.getPrice());

                Order orderAsk = new LimitOrder("ask", matchedSize, bestAsk.getPrice(), orderToProcess.getOrderId(), System.currentTimeMillis(), bestAsk.getSession(),null);
                Order orderBid = new LimitOrder("bid", matchedSize, bestBid.getPrice(), orderToProcess.getOrderId(), System.currentTimeMillis(), bestBid.getSession(),null);
                jsonParsing.addOrderToHistory(orderAsk);
                jsonParsing.addOrderToHistory(orderBid);
                //Notifico l'avvenuta finalizzazione al client se interessato
                notifyClient(orderAsk, "limit");
                notifyClient(orderBid, "limit");
                sendNotificationMulticast(orderAsk, THRESHOLD);

                if (bestBid.getSize() == 0) {
                    activeOrders.remove(bestBid.getOrderId());
                    bidOrders.poll();
                }
                if (bestAsk.getSize() == 0) {
                    activeOrders.remove(bestAsk.getOrderId());
                    askOrders.poll();
                }
            } else {
                break;
            }
        }
    }

    /**
     * Determina la quantità di BTC che può essere scambiata tra il miglior bid e il miglior ask.
     * Il valore restituito è il minimo tra la quantità disponibile nei due ordini.
     *
     * @param bestBid Il miglior ordine di acquisto attuale.
     * @param bestAsk Il miglior ordine di vendita attuale.
     * @return La quantità di BTC che può essere scambiata.
     */
    private int getMatchedSize(Order bestBid, Order bestAsk) {
        int matchedSize = Math.min(bestBid.getSize(), bestAsk.getSize());
        return matchedSize;
    }

    /**
     * Cancella un ordine attivo dal book e lo rimuove dalla mappa degli ordini attivi.
     * Se l'ordine esiste e non è già stato eseguito, viene eliminato dalla coda corrispondente.
     * Se l'ordine non viene trovato, restituisce un codice di errore.
     *
     * @param orderId L'ID dell'ordine da cancellare.
     * @return Un oggetto Response che indica l'esito della cancellazione.
     */
    public synchronized Response cancelOrder(long orderId, String currentUser) {
        Order order = activeOrders.get(orderId);
        //Order order = activeOrders.remove(orderId);
        if (order == null) {
            return new Response(101, "order does not exist or belongs to different user or has already been finalized or other error cases", 0, null); //Ordine non trovato
        }
        if(!order.getUserPropertyName().equals(currentUser)){
            return new Response(101, "order does not exist or belongs to different user or has already been finalized or other error cases", 0, null); //Ordine non trovato
        }

        activeOrders.remove(orderId);
        if (order.getType().equals("bid")) {
            bidOrders.remove(order);
        } else {
            askOrders.remove(order);
        }
        System.out.printf("order.Order %d cancelled successfully\n", orderId);
        return new Response(100, "OK", 0, null);
    }

    /**
     * Restituisce la cronologia dei prezzi per un determinato mese e anno.
     * Gli ordini vengono raggruppati per giorno e analizzati per ottenere i dati di prezzo giornalieri.
     * Se non ci sono ordini disponibili per il mese richiesto, viene restituito un codice di errore.
     *
     * @param month Il mese di riferimento (1-12).
     * @param year  L'anno di riferimento.
     * @return Un oggetto Response contenente i dati della cronologia dei prezzi giornalieri o un codice di errore.
     */
    public synchronized Response getPriceHistory(int month, int year) {
        List<DayPriceData> dayPrices = new ArrayList<>();
        Map<LocalDate, List<Order>> ordersByDay = new HashMap<>();

        List<Order> orders = loadOrders();
        for (Order order : orders) {
            ZonedDateTime orderDate = Instant.ofEpochSecond(order.getTimestamp()).atZone(ZoneOffset.UTC);

            if (orderDate.getMonthValue() == month && orderDate.getYear() == year) {
                LocalDate orderDay = orderDate.toLocalDate();
                if (!ordersByDay.containsKey(orderDay)) {
                    ordersByDay.put(orderDay, new ArrayList<>()); //mette alla chiave del giorno un array degli ordini evasi quel giorno
                }
                ordersByDay.get(orderDay).add(order);
            }
        }
        if (ordersByDay.isEmpty()) {
            return new Response(202, "no price history found for the given month", 0, null);
        }

        for (Map.Entry<LocalDate, List<Order>> entry : ordersByDay.entrySet()) {
            DayPriceData data = getDayPriceData(entry);
            dayPrices.add(data);

        }
        return new Response(0, null, 0, dayPrices);
    }


    /**
     * Calcola i dati giornalieri di prezzo per un determinato giorno, basandosi sugli ordini disponibili.
     * Determina il prezzo di apertura, chiusura, massimo e minimo della giornata.
     *
     * @param entry Una coppia chiave-valore contenente la data e la lista degli ordini eseguiti in quel giorno.
     * @return Un oggetto DayPriceData che rappresenta il riepilogo dei prezzi per il giorno specificato.
     */
    private DayPriceData getDayPriceData(Map.Entry<LocalDate, List<Order>> entry) {
        LocalDate day = entry.getKey();
        List<Order> dayOrders = entry.getValue();

        int openPrice = dayOrders.get(0).getPrice();
        int closePrice = dayOrders.get(dayOrders.size() - 1).getPrice();

        int maxPrice = Integer.MIN_VALUE;
        int minPrice = Integer.MAX_VALUE;

        for (Order order : dayOrders) {
            int price = order.getPrice();
            if (price > maxPrice) {
                maxPrice = price;
            }
            if (price < minPrice) {
                minPrice = price;
            }
        }

        //Creo un oggetto order.DayPriceData per rappresentare il "candelstick" del giorno
        DayPriceData data = new DayPriceData(day, openPrice, closePrice, maxPrice, minPrice);
        return data;
    }

    /**
     * Notifica tutti gli utenti attivi riguardo a un ordine eseguito.
     * Il messaggio di notifica viene generato e inviato via UDP agli utenti attualmente loggati.
     * Viene introdotta una breve pausa per evitare sovraccarico nella trasmissione delle notifiche.
     *
     * @param order     L'ordine per cui notificare i client.
     * @param orderType Il tipo di ordine (es. "market", "limit").
     */
    private void notifyClient(Order order, String orderType) {
        String message = jsonParsing.createNotificationResponse(order.getType(), order.getSize(), order.getPrice(), order.getOrderId(), orderType);

        ConcurrentMap<String, UserSession> activeUsers = userDatabase.getLoggedInUsers();
        for (Map.Entry<String, UserSession> entry : activeUsers.entrySet()) {
            UserSession session = entry.getValue();
            if (session.getAddressUDP() != null) {
                InetSocketAddress addressUDP = session.getAddressUDP();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    System.err.println("Errore nella sleep di notifyClient");
                }
                notificationSender.sendNotification(message, addressUDP);

            }
        }

    }

    /**
     * Invia una notifica multicast agli utenti quando il prezzo di un ordine "ask" supera una soglia specificata.
     * Se il prezzo di vendita raggiunge o supera il valore di soglia, viene generato e inviato un messaggio di notifica.
     *
     * @param order     L'ordine da valutare per l'invio della notifica.
     * @param threshold La soglia di prezzo oltre la quale inviare la notifica.
     */
    private void sendNotificationMulticast(Order order, int threshold) {
        if (order.getType().equals("ask")) {
            if (order.getPrice() >= threshold) {

                String message = jsonParsing.createThresholdNotification(threshold);
                notificationSender.sendNotificationMulticast(message);
            }
        }
    }
}
