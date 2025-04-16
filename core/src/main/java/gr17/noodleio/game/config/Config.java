package gr17.noodleio.game.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.io.IOException;
import java.util.Properties;

/**
 * Cross-platform configuration provider
 *
 */
public class Config {
    // Default values - these will be used for desktop/iOS builds
    private static String SUPABASE_URL = "https://xyzcompany.supabase.co";
    private static String SUPABASE_KEY = "public-anon-key";

    private static boolean initialized = false;

    /**
     * Initialize configuration
     */
    public static void initialize() {
        if (initialized) {
            return;
        }

        try {
            // Try to load from config.properties if it exists
            FileHandle configFile = Gdx.files.internal("config.properties");
            if (configFile.exists()) {
                Properties props = new Properties();
                props.load(configFile.read());

                if (props.containsKey("supabase.url")) {
                    SUPABASE_URL = props.getProperty("supabase.url");
                }

                if (props.containsKey("supabase.key")) {
                    SUPABASE_KEY = props.getProperty("supabase.key");
                }

                Gdx.app.log("Config", "Loaded configuration from config.properties");
            } else {
                Gdx.app.log("Config", "Using default configuration");
            }
        } catch (IOException e) {
            Gdx.app.error("Config", "Error loading config.properties", e);
        }

        initialized = true;
    }

    /**
     * Get Supabase URL
     */
    public static String getSupabaseUrl() {
        if (!initialized) initialize();
        return SUPABASE_URL;
    }

    /**
     * Get Supabase Key
     */
    public static String getSupabaseKey() {
        if (!initialized) initialize();
        return SUPABASE_KEY;
    }

    /**
     * Android-specific method to load from BuildConfig
     * Call this from your AndroidLauncher
     */
    public static void setupFromBuildConfig(String url, String key) {
        SUPABASE_URL = url;
        SUPABASE_KEY = key;
        initialized = true;
        Gdx.app.log("Config", "Configured from Android BuildConfig");
    }
}
