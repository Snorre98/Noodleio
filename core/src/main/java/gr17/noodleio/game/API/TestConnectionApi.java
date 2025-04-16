package gr17.noodleio.game.API;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.services.ServiceManager;

public class TestConnectionApi {
    private final ServiceManager serviceManager;
    private String connectionStatus = "Not tested";

    public TestConnectionApi(EnvironmentConfig environmentConfig) {
        this.serviceManager = new ServiceManager(environmentConfig);
    }

    /**
     * Tests the connection to Supabase
     * @return Status message
     */
    public String testSupabaseConnection() {
        try {
            // This is just to verify the client initialization works
            // The first access to any service will trigger the lazy initialization

            // Just accessing the service property will initialize the client
            // Use the getter method when accessing Kotlin properties from Java
            var auth = serviceManager.getAuth();
            connectionStatus = "Connected to Supabase!";
            System.out.println("Supabase auth service initialized successfully");
            return connectionStatus;
        } catch (Exception e) {
            connectionStatus = "Failed to connect: " + e.getMessage();
            System.err.println("Error initializing Supabase: " + e.getMessage());
            e.printStackTrace();
            return connectionStatus;
        }
    }

    /**
     * Gets the most recent connection status
     * @return The connection status message
     */
    public String getConnectionStatus() {
        return connectionStatus;
    }

    /**
     * Gets the ServiceManager instance
     * @return The ServiceManager
     */
    public ServiceManager getServiceManager() {
        return serviceManager;
    }
}
