package gr17.noodleio.game.services

import gr17.noodleio.game.config.EnvironmentConfig

class TestConnectionService (private val environmentConfig: EnvironmentConfig) {

    // Create our own service manager with custom serializer
    private val serviceManager: ServiceManager = ServiceManager(environmentConfig)

    fun testSupabaseConnection(): String {
        // Example: Access Supabase services
        return try {
            // This is just to verify the client initialization works
            // The first access to any service will trigger the lazy initialization

            // Just accessing the service property will initialize the client
            val auth = serviceManager.auth
            println("Supabase auth service initialized successfully")
            "Connected to Supabase!"
        } catch (e: Exception) {
            val errorMessage = "Error initializing Supabase: ${e.message}"
            System.err.println(errorMessage)
            "Failed to connect: ${e.message}"
        }
    }


}
