package gr17.noodleio.game.services

import gr17.noodleio.game.config.EnvironmentConfig
import gr17.noodleio.game.models.LeaderboardEntry
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class Api(private val environmentConfig: EnvironmentConfig) {

    private var statusMessage = "Initializing..."

    // Service manager
    private var serviceManager: ServiceManager = ServiceManager(environmentConfig)

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

    /**
     * Gets the top N entries from the leaderboard
     * @return List of top LeaderboardEntry objects
     */
    fun getTopLeaderboard(limit: Long): List<LeaderboardEntry> {
        return runBlocking {
            try {
                // Query the leaderboard table with a limit
                val result = serviceManager.db
                    .from("Leaderboard")
                    .select(){
                        limit(limit)
                        order(column = "score", order = Order.DESCENDING)
                    }
                    .decodeList<LeaderboardEntry>()

                println("Successfully fetched top $limit leaderboard entries")
                result
            } catch (e: Exception) {
                println("Error fetching top leaderboard: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Adds a new entry to the leaderboard
     * @param playerName The name of the player
     * @param score The player's score
     * @return The created LeaderboardEntry or null if there was an error
     */
    fun addLeaderboardEntry(playerName: String, score: Int, level: Int? = null): LeaderboardEntry? {
        return runBlocking {
            try {
                // Use JsonObject to ensure proper serialization

                val jsonData = buildJsonObject {
                    put("player_name", playerName)
                    put("score", score)
                }

                // Insert the new entry
                val result = serviceManager.db
                    .from("Leaderboard")
                    .insert(jsonData)
                    .decodeSingle<LeaderboardEntry>()

                println("Successfully added new leaderboard entry for $playerName with score $score")
                result
            } catch (e: Exception) {
                println("Error adding leaderboard entry: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
}
