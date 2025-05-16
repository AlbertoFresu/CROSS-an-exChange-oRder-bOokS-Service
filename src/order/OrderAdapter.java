package order;

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * Adatta la classe Order per la serializzazione e deserializzazione JSON tramite Gson.
 * Implementa JsonSerializer<Order> e JsonDeserializer<Order>.
 */
public class OrderAdapter implements JsonSerializer<Order>, JsonDeserializer<Order> {

    /**
     * Deserializza un oggetto JSON in un'istanza di Order.
     *
     * @param json    L'elemento JSON da deserializzare.
     * @param typeOfT Il tipo dell'oggetto da deserializzare.
     * @param context Il contesto di deserializzazione.
     * @return Un'istanza di Order costruita dai dati JSON.
     * @throws JsonParseException Se il JSON non è valido o mancano campi obbligatori.
     */
    @Override
    public Order deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        if (!jsonObject.has("type") || jsonObject.get("type").isJsonNull()) {
            System.err.println("ERRORE: Campo 'type' assente o nullo nel JSON!");
        }
        if (!jsonObject.has("orderId") || jsonObject.get("orderId").isJsonNull()) {
            System.err.println("ERRORE: Campo 'OrderId' assente o nullo nel JSON!");
        }
        if (!jsonObject.has("size") || jsonObject.get("size").isJsonNull()) {
            throw new JsonParseException("ERRORE: Campo 'size' assente o nullo nel JSON!");
        }
        if (!jsonObject.has("price") || jsonObject.get("price").isJsonNull()) {
            throw new JsonParseException("ERRORE: Campo 'price' assente o nullo nel JSON!");
        }
        if (!jsonObject.has("timestamp") || jsonObject.get("timestamp").isJsonNull()) {
            throw new JsonParseException("ERRORE: Campo 'timestamp' assente o nullo nel JSON!");
        }

        return new Order(
                jsonObject.get("type").getAsString(),
                jsonObject.get("size").getAsInt(),
                jsonObject.get("price").getAsInt(),
                jsonObject.get("orderId").getAsLong(),
                jsonObject.get("timestamp").getAsLong(),
                null ,// Non deserializziamo `UserSession`
                null // Non deserializziamo `userPropertyName`
        );
    }

    /**
     * Serializza un'istanza di Order in formato JSON.
     *
     * @param order      L'oggetto Order da serializzare.
     * @param typeOfSrc  Il tipo dell'oggetto sorgente.
     * @param context    Il contesto di serializzazione.
     * @return Un elemento JSON che rappresenta l'oggetto Order.
     */
    @Override
    public JsonElement serialize(Order order, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("OrderId", order.getOrderId());
        jsonObject.addProperty("type", order.getType());
        jsonObject.addProperty("size", order.getSize());
        jsonObject.addProperty("price", order.getPrice());
        jsonObject.addProperty("timestamp", order.getTimestamp());
        // NOTA: Non serializziamo `session`
        return jsonObject;
    }
}
