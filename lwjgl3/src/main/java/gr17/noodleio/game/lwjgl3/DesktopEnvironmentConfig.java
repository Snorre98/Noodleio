package gr17.noodleio.game.lwjgl3;

import java.io.FileInputStream;
import java.util.Properties;

import gr17.noodleio.game.config.EnvironmentConfig;

public class DesktopEnvironmentConfig implements EnvironmentConfig {
    private final Properties properties;

    public DesktopEnvironmentConfig() {
        properties = new Properties();
        try {
            properties.load(new FileInputStream("local.properties"));
            System.out.println("Loaded local.properties successfully");
        } catch (Exception e) {
            System.err.println("Failed to load local.properties: " + e.getMessage());
        }
    }

    @Override
    public String getSupabaseUrl() {
        String url = properties.getProperty("SUPABASE_URL");
        if (url == null) {
            // Try with lowercase format
            url = properties.getProperty("supabase_url");
        }
        System.out.println("Supabase URL: " + (url != null ? "found" : "not found"));
        return url;
    }

    @Override
    public String getSupabaseKey() {
        // Try multiple property name variations
        String key = properties.getProperty("SUPABASE_KEY");

        if (key == null) {
            key = properties.getProperty("SUPABASE_ANON_KEY");
        }

        if (key == null) {
            // Try lowercase variations
            key = properties.getProperty("supabase_key");
        }

        if (key == null) {
            key = properties.getProperty("supabase_anon_key");
        }

        // Check environment variables as fallback
        if (key == null) {
            key = System.getenv("SUPABASE_KEY");
        }

        if (key == null) {
            key = System.getenv("SUPABASE_ANON_KEY");
        }

        System.out.println("Tried multiple key names. Supabase Key: " + (key != null ? "found" : "not found"));
        if (key == null) {
            System.out.println("Available properties in local.properties:");
            for (String propName : properties.stringPropertyNames()) {
                System.out.println("  - " + propName);
            }
        }

        return key;
    }
}
