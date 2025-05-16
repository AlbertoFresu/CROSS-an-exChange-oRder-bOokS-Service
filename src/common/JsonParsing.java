package common;

import com.google.gson.*;
import order.DayPriceData;
import order.Order;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


/**
 * Classe per la gestione della serializzazione e deserializzazione JSON.
 * Fornisce metodi per convertire messaggi, ordini e dati storici in JSON e viceversa.
 */
public class JsonParsing {
    private final Gson gson = new Gson();
    private static final String ORDER_HISTORY_FILE = "resources/Storico.json";

    /**
     * Converte una stringa JSON in un oggetto {@link JsonObject}.
     *
     * @param message Il messaggio JSON da convertire.
     * @return Un oggetto {@link JsonObject} ottenuto dal JSON.
     */
    public JsonObject parseMessage(String message) {
        return gson.fromJson(message, JsonObject.class);
    }

    /**
     * Converte una lista di dati di prezzo giornalieri in un JSON di risposta.
     *
     * @param dayPrices Lista di oggetti {@link DayPriceData} contenenti i dati storici dei prezzi.
     * @return Una stringa JSON contenente i dati storici.
     */
    public String HistoryNotification(List<DayPriceData> dayPrices){
        JsonArray jsonArray = new JsonArray();

        for(DayPriceData data : dayPrices){
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("date", data.getDate().toString());//Converte LocalData in stringa
            jsonObject.addProperty("openPrice",data.getOpenPrice());
            jsonObject.addProperty("closePrice",data.getClosePrice());
            jsonObject.addProperty("maxPrice",data.getMaxPrice());
            jsonObject.addProperty("minPrice",data.getMinPrice());

            jsonArray.add(jsonObject);
        }

        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("response",200);
        responseJson.addProperty("message","price history retrieved");
        responseJson.add("data",jsonArray);

        return gson.toJson(responseJson);
    }

    /**
     * Converte una stringa JSON con la cronologia degli ordini in un formato leggibile.
     *
     * @param json         Il JSON da convertire.
     * @param responseCode Il codice di risposta HTTP associato.
     * @return Una stringa formattata contenente le informazioni della cronologia.
     */
    public String convertHistoryJsonToString(String json,int responseCode){
        JsonObject jsonObject = parseMessage(json);

        String message = jsonObject.get("message").getAsString();

        StringBuilder result = new StringBuilder();
        result.append("Response Code: ").append(responseCode).append("\n");
        result.append("Message: ").append(message).append("\n");

        if(jsonObject.has("data")){
            JsonArray dataArray = jsonObject.getAsJsonArray("data");

            for(JsonElement element : dataArray){
                JsonObject orderData = element.getAsJsonObject();
                String date = orderData.get("date").getAsString();
                int openPrice = orderData.get("openPrice").getAsInt();
                int closePrice = orderData.get("closePrice").getAsInt();
                int maxPrice = orderData.get("maxPrice").getAsInt();
                int minPrice = orderData.get("minPrice").getAsInt();

                result.append("Date: ").append(date).append("\n");
                result.append("OpenPrice: ").append(openPrice).append("\n");
                result.append("ClosePrice: ").append(closePrice).append("\n");
                result.append("Max Price: ").append(maxPrice).append("\n");
                result.append("Min Price: ").append(minPrice).append("\n");
            }
        }
        return result.toString();
    }

    /**
     * Crea una risposta JSON in base al codice di risposta e ai dati forniti.
     *
     * @param responseCode Il codice di risposta.
     * @param errorMessage Il messaggio di errore (se presente).
     * @param orderId      L'ID dell'ordine (se presente).
     * @param dayPrices    Lista dei dati storici dei prezzi (se presente).
     * @return Una stringa JSON contenente la risposta formattata.
     */
    public String createResponse(int responseCode, String errorMessage, long orderId, List<DayPriceData> dayPrices) {
        JsonObject jsonResponse = new JsonObject();
        if(dayPrices == null){
            jsonResponse.addProperty("response", responseCode);
            jsonResponse.addProperty("errorMessage", errorMessage);
            if (orderId != 0) {
                jsonResponse.addProperty("orderId", orderId);
            }
            return gson.toJson(jsonResponse);
        }
        else{
            return HistoryNotification(dayPrices);
        }
    }

    /**
     * Converte un messaggio JSON in una stringa leggibile.
     *
     * @param message Il messaggio JSON da convertire.
     * @return Una stringa formattata contenente le informazioni del messaggio.
     */
    public String convertResponseToString(String message){
        // Prima di tutto, proviamo a parsare il messaggio in un JsonObject
        JsonObject jsonMessage = parseMessage(message);

        int responseCode = jsonMessage.get("response").getAsInt();
        StringBuilder responseString = new StringBuilder();

        // Controlla se il responseCode è 200, 0 o 202
        if (responseCode == 200) {
            // Se il codice di risposta è uno di quelli desiderati, chiama il metodo per convertire la risposta
            return convertHistoryJsonToString(message,responseCode);
        }

        // Se il responseCode non è tra i codici specificati, elabora normalmente la risposta
        long orderId = jsonMessage.has("orderId") ? jsonMessage.get("orderId").getAsLong() : 0;

        if (orderId > 0 || orderId == -1){
            responseString.append("Order ID: ").append(orderId);
        } else {
            String errorMessage = jsonMessage.get("errorMessage").getAsString();
            responseString.append("Response Code: ").append(responseCode);
            responseString.append(" Error Message: ").append(errorMessage);
        }

        return responseString.toString();

    }

    /**
     * Converte un'operazione e i relativi parametri in una stringa JSON.
     *
     * @param operation Il nome dell'operazione.
     * @param params    I parametri richiesti per l'operazione.
     * @return Una stringa JSON rappresentante l'operazione.
     */
    public String convertMessageToJson(String operation, String... params) {
        JsonObject jsonMessage = new JsonObject();
        jsonMessage.addProperty("operation", operation);
        JsonObject values = new JsonObject();

        switch (operation) {
            case "register":
            case "login":
                values.addProperty("username", params[0]);
                values.addProperty("password", params[1]);
                break;
            case "logout":
                values.addProperty("username", params[0]);
                break;
            case "updateUserCredentials":
                values.addProperty("oldUsername", params[0]);
                values.addProperty("newUsername", params[1]);
                values.addProperty("newPassword", params[2]);
                break;
            case "insertLimitOrder":
            case "insertStopOrder":
                values.addProperty("type", params[0]);
                values.addProperty("size", Integer.parseInt(params[1]));
                values.addProperty("price", Integer.parseInt(params[2]));
                break;
            case "insertMarketOrder":
                values.addProperty("type", params[0]);
                values.addProperty("size", Integer.parseInt(params[1]));
                break;
            case "cancelOrder":
                values.addProperty("orderId", Integer.parseInt(params[0]));
                break;
            case "getPriceHistory":
                values.addProperty("month", params[0]);
                break;
            default:
                JsonObject unknownOperation = new JsonObject();
                unknownOperation.addProperty("error", "Unknown operation: " + operation);
                return gson.toJson(unknownOperation);
        }
        jsonMessage.add("values", values);
        return gson.toJson(jsonMessage);
    }

    /**
     * Converte un messaggio JSON in una stringa leggibile.
     * Estrae l'operazione e i valori associati, costruendo una stringa in base ai dati forniti.
     *
     * @param json Il messaggio JSON da convertire in una stringa.
     * @return Una stringa formattata contenente l'operazione e i valori associati, o un messaggio di errore se l'operazione non è riconosciuta.
     */
    public String convertJsonToMessage(String json) {
        //Converte il messaggio JSON in una stringa
        JsonObject jsonMessage = parseMessage(json);
        //Recupera l'operazione dal messaggio
        String operation = jsonMessage.get("operation").getAsString();
        //Recupera i valori dal messaggio
        JsonObject values = jsonMessage.getAsJsonObject("values");

        //Costruisce la stringa del messaggio
        StringBuilder message = new StringBuilder(operation);

        //Aggiunge i valori al messaggio
        switch (operation) {
            case "register":
            case "login":
                //Aggiunge username e password al messaggio
                message.append(" ").append(values.get("username").getAsString());
                message.append(" ").append(values.get("password").getAsString());
                break;
            case "logout":
                //Aggiunge username al messaggio
                message.append(" ").append(values.get("username").getAsString());
                break;
            case "updateUserCredentials":
                //Aggiunge oldUsername, newUsername e newPassword al messaggio
                message.append(" ").append(values.get("oldUsername").getAsString());
                message.append(" ").append(values.get("newUsername").getAsString());
                message.append(" ").append(values.get("newPassword").getAsString());
                break;
            case "insertLimitOrder":
            case "insertStopOrder":
                //Aggiunge type, size e price al messaggio
                message.append(" ").append(values.get("type").getAsString());
                message.append(" ").append(values.get("size").getAsInt());
                message.append(" ").append(values.get("price").getAsInt());
                break;
            case "insertMarketOrder":
                //Aggiunge type e size al messaggio
                message.append(" ").append(values.get("type").getAsString());
                message.append(" ").append(values.get("size").getAsInt());
                break;
            case "cancelOrder":
                //Aggiunge orderId al messaggio
                message.append(" ").append(values.get("orderId").getAsInt());
                break;
            case "getPriceHistory":
                //Aggiunge month al messaggio
                message.append(" ").append(values.get("month").getAsString());
                break;
            default:
                return "Operazione sconosciuta: " + operation;
        }
        //Ritorna il messaggio
        return message.toString();
    }

    /**
     * Aggiunge un ordine alla cronologia degli ordini salvata in un file JSON.
     *
     * @param order L'ordine da aggiungere alla cronologia.
     */
    public synchronized void addOrderToHistory(Order order) {
        try {
            Path filePath = Path.of(ORDER_HISTORY_FILE);
            JsonObject orderHistory;
            // Leggi il file se esiste, altrimenti crea una nuova struttura
            if (!Files.exists(filePath)) {
                try {
                    if (filePath.getParent() != null) {
                        Files.createDirectories(filePath.getParent());
                    }
                    Files.createFile(filePath);
                } catch (IOException e) {
                    System.err.println("Errore nella creazione del file \"storico\" o directory: " + e.getMessage());
                    return;
                }

                orderHistory = new JsonObject();
                orderHistory.add("trades", new JsonArray());
            } else {
                try (Reader reader = new FileReader(ORDER_HISTORY_FILE)) {
                    orderHistory = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
                } catch (IOException | IllegalStateException | JsonSyntaxException e) {
                    System.err.println("Errore nella lettura del file \"storico\": " + e.getMessage());
                    orderHistory = new JsonObject();
                    orderHistory.add("trades", new JsonArray());
                }
            }

            if (!orderHistory.has("trades") || orderHistory.get("trades").isJsonNull()) {
                orderHistory.add("trades", new JsonArray());
            }

            JsonArray trades = orderHistory.getAsJsonArray("trades");
            JsonObject trade = new JsonObject();

            trade.addProperty("orderId", order.getOrderId());
            trade.addProperty("type", order.getType());
            trade.addProperty("orderType", order.getClass().getSimpleName().toLowerCase());
            trade.addProperty("size", order.getSize());
            trade.addProperty("price", order.getPrice());
            trade.addProperty("timestamp", System.currentTimeMillis() / 1000);

            trades.add(trade);

            // Scrivi il file aggiornato
            try (Writer writer = new FileWriter(ORDER_HISTORY_FILE)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(orderHistory, writer);
            } catch (IOException e) {
                System.err.println("Errore nella scrittura del file \"storico\": " + e.getMessage());
            }
        } catch (Exception ex) {
            System.err.println("Errore nella funzione storico: " + ex.getMessage());
        }
    }

    /**
     * Crea una notifica JSON per la chiusura di un'operazione di trading.
     * Include dettagli sull'operazione chiusa, come ID ordine, tipo, quantità e prezzo.
     *
     * @param type      Il tipo di operazione eseguita (buy/sell).
     * @param size      La quantità dell'operazione.
     * @param price     Il prezzo al quale è stata chiusa l'operazione.
     * @param orderId   L'ID dell'ordine chiuso.
     * @param orderType Il tipo di ordine eseguito.
     * @return Una stringa JSON contenente i dettagli della notifica.
     */
    public String createNotificationResponse(String type, int size, int price, long orderId, String orderType) {
        JsonObject notificationJson = new JsonObject();
        notificationJson.addProperty("notification", "closedTrades");

        JsonArray trades = new JsonArray();

        JsonObject trade = new JsonObject();
        trade.addProperty("orderId", orderId);
        trade.addProperty("type", type);
        trade.addProperty("orderType", orderType);
        trade.addProperty("size", size);
        trade.addProperty("price", price);
        trades.add(trade);

        notificationJson.add("trades", trades);

        return notificationJson.toString();
    }


    /**
     * Crea una notifica JSON quando viene raggiunta una soglia di prezzo.
     * Include la soglia superata nel messaggio JSON.
     *
     * @param threshold Il valore della soglia superata.
     * @return Una stringa JSON rappresentante la notifica.
     */
    public String createThresholdNotification(int threshold) {
        JsonObject notificationJson = new JsonObject();
        notificationJson.addProperty("limitPriceThresholdReached", threshold);
        return notificationJson.toString();
    }

    /**
     * Analizza una stringa JSON per estrarre la notifica della soglia di prezzo raggiunta.
     * Include il valore della soglia nel messaggio di ritorno.
     *
     * @param jsonString La stringa JSON contenente la notifica.
     * @return Una stringa rappresentante la notifica della soglia di prezzo raggiunta.
     */
    public String parseThresholdNotification(String jsonString) {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            if (jsonObject.has("limitPriceThresholdReached")) {
                int threshold = jsonObject.get("limitPriceThresholdReached").getAsInt();
                return "Threshold reached: " + threshold;
            } else {
                return "Property 'limitPriceThresholdReached' not found in JSON.";
            }
        } catch (JsonSyntaxException e) {
            return "Invalid JSON format.";
        }
    }
}
