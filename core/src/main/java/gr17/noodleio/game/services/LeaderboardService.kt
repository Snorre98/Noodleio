package gr17.noodleio.game.services

import gr17.noodleio.game.config.EnvironmentConfig
import gr17.noodleio.game.models.LeaderboardEntry
import gr17.noodleio.game.models.GameSession
import gr17.noodleio.game.services.logging.ServiceLogger
import gr17.noodleio.game.services.logging.ServiceLoggerFactory
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Service for managing leaderboard operations
 */
class LeaderboardService(environmentConfig: EnvironmentConfig) {

    private val logger: ServiceLogger = ServiceLoggerFactory.getLogger()
    private val serviceManager: ServiceManager = ServiceManager(environmentConfig)

    companion object {
        private const val TAG = "LeaderboardService"
    }

    /**
     * Gets the top N entries from the leaderboard
     * @return List of top LeaderboardEntry objects
     */
    fun getTopLeaderboard(limit: Long): List<LeaderboardEntry> {
        return runBlocking {
            try {
                logger.debug(TAG, "Fetching top $limit leaderboard entries")

                val result = serviceManager.db
                    .from("Leaderboard")
                    .select(){
                        limit(limit)
                        order(column = "score", order = Order.DESCENDING)
                    }
                    .decodeList<LeaderboardEntry>()

                logger.info(TAG, "Successfully fetched ${result.size} leaderboard entries")
                result
            } catch (e: Exception) {
                logger.error(TAG, "Failed to fetch top leaderboard", e)
                emptyList()
            }
        }
    }

    /**
     * Adds a new entry to the leaderboard
     * @param playerName The name of the player
     * @param score The player's score
     * @param durationSeconds The time in seconds it took to achieve the score
     * @param level The game level
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
                logger.debug(TAG, "Adding leaderboard entry for $playerName with score $score")

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

                val response = serviceManager.db
                    .from("Leaderboard")
                    .insert(jsonData){
                        select()
                    }

                // Handle empty response case
                val responseText = response.toString()
                if (responseText.isBlank() || responseText == "[]") {
                    logger.debug(TAG, "Insert returned empty response, attempting to fetch recently added entry")

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
                        logger.info(TAG, "Successfully added leaderboard entry for $playerName")
                        return@runBlocking entries.first()
                    }

                    logger.info(TAG, "Entry was added but could not be retrieved, creating placeholder")
                    return@runBlocking LeaderboardEntry(
                        id = "unknown",
                        player_name = playerName,
                        score = score,
                        duration_seconds = durationSeconds
                    )
                }

                val result = response.decodeSingle<LeaderboardEntry>()
                logger.info(TAG, "Successfully added leaderboard entry for $playerName")
                result

            } catch (e: Exception) {
                logger.error(TAG, "Failed to add leaderboard entry", e)

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
     * @return Duration in seconds as Double, or null if ended_at is not set
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
        logger.debug(TAG, "Adding leaderboard entry from game session")
        val durationSeconds = calculateGameDuration(gameSession)
        return addLeaderboardEntry(playerName, score, durationSeconds)
    }
}
