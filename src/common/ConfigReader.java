package common;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    private Properties properties;

    public ConfigReader(InputStream inputStream) throws IOException {
        properties = new Properties();
        /*
        try (FileReader reader = new FileReader(configFilePath)) {
            properties.load(reader);
        }*/
        properties.load(inputStream);
    }

    public String getString(String key) {
        return properties.getProperty(key);
    }

    public int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    public long getLong(String key) {
        return Long.parseLong(properties.getProperty(key));
    }

    public boolean getBoolean(String key){
        return Boolean.parseBoolean(properties.getProperty(key));
    }

}
