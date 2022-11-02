package configuration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

public class DBConfig {
    private static Properties properties = new Properties();

    static {
        InputStream in = null;
        try {
            in = new FileInputStream("config.properties");
            properties.load(in);
            in.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static int getIntegerProperty(String key) {
        return Integer.valueOf(properties.getProperty(key));
    }

    public static String getProperty(String key) {
        String res = properties.getProperty(key);
        if (res.equalsIgnoreCase("null")) {
            return null;
        } else {
            return res;
        }
    }

}
