package user;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * La classe UserDataManager gestisce il salvataggio e il caricamento dei dati utente in un file JSON.
 */
public class UserDataManager {
    private final String filePath;
    private final Gson gson;

    /**
     * Costruttore della classe UserDataManager.
     * Inizializza il percorso del file e l'istanza di Gson.
     *
     * @param filePath percorso del file di dati.
     */
    public UserDataManager(String filePath) {
        this.filePath = filePath;
        this.gson = new Gson();
    }

    /**
     * Salva gli utenti nel file JSON specificato.
     *
     * @param users mappa degli utenti da salvare.
     */
    public void saveUsersToFile(Map<String, User> users){
        try(FileWriter writer = new FileWriter(filePath)){
            gson.toJson(users,writer);
        }
        catch(IOException e){
            System.err.println("Errore nel salvataggio dell'utente");
        }
    }

    /**
     * Carica gli utenti dal file JSON specificato.
     *
     * @return mappa degli utenti caricati dal file.
     */
    public Map<String,User> loadUsersFromFile() {
        try (FileReader reader = new FileReader(filePath)) {
            Type type = new TypeToken<Map<String, User>>() {}.getType();
            Map<String, User> loadedUsers = gson.fromJson(reader, type);
            if (loadedUsers != null) {
                return loadedUsers;
            } else {
                return new HashMap<>();
            }
        } catch (IOException e) {
            System.err.println("Nessun file trovato o errore nella lettura");
            return new HashMap<>();
        }
    }
}
