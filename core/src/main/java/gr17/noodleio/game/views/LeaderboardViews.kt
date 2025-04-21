package gr17.noodleio.game.views

import gr17.noodleio.game.config.EnvironmentConfig
import gr17.noodleio.game.models.LeaderboardEntry
import gr17.noodleio.game.models.GameSession
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put


class LeaderboardViews(environmentConfig: EnvironmentConfig) {

    // Create our own service manager with custom serializer
    private val serviceManager: ServiceManager = ServiceManager(environmentConfig)

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
     * @param durationSeconds The time in seconds it took to achieve the score
     * @return The created LeaderboardEntry or null if there was an error
     */
    fun addLeaderboardEntry(
        playerName: String,
        score: Int,
        durationSeconds: Double? = null,
        level: Int? = null
    ): LeaderboardEntry? {
        return runBlocking {
            try {
                // Use JsonObject to ensure proper serialization
                val jsonData = buildJsonObject {
                    put("player_name", playerName)
                    put("score", score)
                    if (durationSeconds != null) {
                        put("duration_seconds", durationSeconds)
                    }
                    if (level != null) {
                        put("level", level)
                    }
                }

                // Insert the new entry and handle empty response case
                val response = serviceManager.db
                    .from("Leaderboard")
                    .insert(jsonData){
                        select()
                    }

                // The insert might return an empty string if the DB is not configured to return the inserted row
                val responseText = response.toString()
                if (responseText.isBlank() || responseText == "[]") {
                    // Try to fetch the recently added entry
                    val entries = serviceManager.db
                        .from("Leaderboard")
                        .select {
                            filter {
                                eq("player_name", playerName)
                                eq("score", score)
                            }
                            order("created_at", Order.DESCENDING)
                            limit(1)
                        }
                        .decodeList<LeaderboardEntry>()

                    if (entries.isNotEmpty()) {
                        println("Successfully added new leaderboard entry for $playerName with score $score and duration ${durationSeconds ?: "unknown"} seconds")
                        return@runBlocking entries.first()
                    }

                    println("Entry was added but could not be retrieved")
                    // Create a placeholder object
                    return@runBlocking LeaderboardEntry(
                        id = "unknown",
                        player_name = playerName,
                        score = score,
                        duration_seconds = durationSeconds
                    )
                }

                val result = response.decodeSingle<LeaderboardEntry>()
                println("Successfully added new leaderboard entry for $playerName with score $score and duration ${durationSeconds ?: "unknown"} seconds")
                result
            } catch (e: Exception) {
                println("Error adding leaderboard entry: ${e.message}")
                e.printStackTrace()

                // Create a placeholder entry when we can't get the real one
                LeaderboardEntry(
                    id = "error-" + System.currentTimeMillis(),
                    player_name = playerName,
                    score = score,
                    duration_seconds = durationSeconds
                )
            }
        }
    }

    /**
     * Calculate duration in seconds between two timestamps from a GameSession
     * @param gameSession The completed game session with start and end times
     * @return Duration in seconds as Int, or null if ended_at is not set
     */
    private fun calculateGameDuration(gameSession: GameSession): Double? {
        val startTime = gameSession.started_at
        val endTime = gameSession.ended_at ?: return null

        // Calculate duration in seconds
        val durationMillis = endTime.toEpochMilliseconds() - startTime.toEpochMilliseconds()
        return (durationMillis / 1000).toDouble()
    }

    /**
     * Add a leaderboard entry directly from a game session
     * @param playerName The name of the player
     * @param score The final score
     * @param gameSession The completed game session
     * @return The created LeaderboardEntry
     */
    fun addLeaderboardEntryFromSession(
        playerName: String,
        score: Int,
        gameSession: GameSession
    ): LeaderboardEntry? {
        val durationSeconds = calculateGameDuration(gameSession)
        return addLeaderboardEntry(playerName, score, durationSeconds)
    }
}
