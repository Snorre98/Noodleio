package gr17.noodleio.game.services

import gr17.noodleio.game.config.EnvironmentConfig
import gr17.noodleio.game.services.logging.ServiceLogger
import gr17.noodleio.game.services.logging.ServiceLoggerFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Service for managing player game state operations
 */
class PlayerGameStateService(environmentConfig: EnvironmentConfig) {

    private val logger: ServiceLogger = ServiceLoggerFactory.getLogger()
    private val serviceManager: ServiceManager = ServiceManager(environmentConfig)

    companion object {
        private const val TAG = "PlayerGameStateService"
    }

    /**
     * Data class for the response from movement RPC functions
     */
    @Serializable
    data class MoveResponse(
        @SerialName("success") val success: Boolean,
        @SerialName("message") val message: String,
        @SerialName("new_y_pos") val newYPos: Double? = null,
        @SerialName("new_x_pos") val newXPos: Double? = null
    )

    /**
     * Moves a player up by one position unit
     * @param playerId The ID of the player to move
     * @param sessionId The ID of the game session
     * @return Pair containing the success status and a message
     */
    fun movePlayerUp(playerId: String, sessionId: String): Pair<Boolean, String> {
        logger.debug(TAG, "Moving player $playerId up in session $sessionId")

        return runBlocking {
            try {
                val params = buildJsonObject {
                    put("p_player_id", playerId)
                    put("p_session_id", sessionId)
                }

                val response = serviceManager.db.rpc("move_up", params)
                val results = response.decodeList<MoveResponse>()

                if (results.isEmpty()) {
                    logger.debug(TAG, "No response from move_up function")
                    return@runBlocking Pair(false, "Failed to move player: No response from server")
                }

                val result = results.first()
                logger.debug(TAG, "Move up result: success=${result.success}, newPos=${result.newYPos}")

                return@runBlocking Pair(result.success, result.message +
                    (if (result.newYPos != null) " (New Y: ${result.newYPos})" else ""))
            } catch (e: Exception) {
                logger.error(TAG, "Error in movePlayerUp", e)
                return@runBlocking Pair(false, "Error moving player up: ${e.message}")
            }
        }
    }

    /**
     * Moves a player down by one position unit
     * @param playerId The ID of the player to move
     * @param sessionId The ID of the game session
     * @return Pair containing the success status and a message
     */
    fun movePlayerDown(playerId: String, sessionId: String): Pair<Boolean, String> {
        logger.debug(TAG, "Moving player $playerId down in session $sessionId")

        return runBlocking {
            try {
                val params = buildJsonObject {
                    put("p_player_id", playerId)
                    put("p_session_id", sessionId)
                }

                val response = serviceManager.db.rpc("move_down", params)
                val results = response.decodeList<MoveResponse>()

                if (results.isEmpty()) {
                    logger.debug(TAG, "No response from move_down function")
                    return@runBlocking Pair(false, "Failed to move player: No response from server")
                }

                val result = results.first()
                logger.debug(TAG, "Move down result: success=${result.success}, newPos=${result.newYPos}")

                return@runBlocking Pair(result.success, result.message +
                    (if (result.newYPos != null) " (New Y: ${result.newYPos})" else ""))
            } catch (e: Exception) {
                logger.error(TAG, "Error in movePlayerDown", e)
                return@runBlocking Pair(false, "Error moving player down: ${e.message}")
            }
        }
    }

    /**
     * Moves a player left by one position unit
     * @param playerId The ID of the player to move
     * @param sessionId The ID of the game session
     * @return Pair containing the success status and a message
     */
    fun movePlayerLeft(playerId: String, sessionId: String): Pair<Boolean, String> {
        logger.debug(TAG, "Moving player $playerId left in session $sessionId")

        return runBlocking {
            try {
                val params = buildJsonObject {
                    put("p_player_id", playerId)
                    put("p_session_id", sessionId)
                }

                val response = serviceManager.db.rpc("move_left", params)
                val results = response.decodeList<MoveResponse>()

                if (results.isEmpty()) {
                    logger.debug(TAG, "No response from move_left function")
                    return@runBlocking Pair(false, "Failed to move player: No response from server")
                }

                val result = results.first()
                logger.debug(TAG, "Move left result: success=${result.success}, newPos=${result.newXPos}")

                return@runBlocking Pair(result.success, result.message +
                    (if (result.newXPos != null) " (New X: ${result.newXPos})" else ""))
            } catch (e: Exception) {
                logger.error(TAG, "Error in movePlayerLeft", e)
                return@runBlocking Pair(false, "Error moving player left: ${e.message}")
            }
        }
    }

    /**
     * Moves a player right by one position unit
     * @param playerId The ID of the player to move
     * @param sessionId The ID of the game session
     * @return Pair containing the success status and a message
     */
    fun movePlayerRight(playerId: String, sessionId: String): Pair<Boolean, String> {
        logger.debug(TAG, "Moving player $playerId right in session $sessionId")

        return runBlocking {
            try {
                val params = buildJsonObject {
                    put("p_player_id", playerId)
                    put("p_session_id", sessionId)
                }

                val response = serviceManager.db.rpc("move_right", params)
                val results = response.decodeList<MoveResponse>()

                if (results.isEmpty()) {
                    logger.debug(TAG, "No response from move_right function")
                    return@runBlocking Pair(false, "Failed to move player: No response from server")
                }

                val result = results.first()
                logger.debug(TAG, "Move right result: success=${result.success}, newPos=${result.newXPos}")

                return@runBlocking Pair(result.success, result.message +
                    (if (result.newXPos != null) " (New X: ${result.newXPos})" else ""))
            } catch (e: Exception) {
                logger.error(TAG, "Error in movePlayerRight", e)
                return@runBlocking Pair(false, "Error moving player right: ${e.message}")
            }
        }
    }

    /**
     * Updates a player's score
     * @param playerId The ID of the player
     * @param sessionId The ID of the game session
     * @param newScore The new score value
     * @return Status message
     */
    fun updatePlayerScore(playerId: String, sessionId: String, newScore: Int): String {
        logger.debug(TAG, "Updating player $playerId score to $newScore in session $sessionId")

        return runBlocking {
            try {
                val jsonData = buildJsonObject {
                    put("score", newScore)
                }

                // Update the player's score in the PlayerGameState table
                serviceManager.db
                    .from("PlayerGameState")
                    .update(jsonData) {
                        filter {
                            eq("player_id", playerId)
                            eq("session_id", sessionId)
                        }
                    }

                logger.info(TAG, "Successfully updated player score to $newScore")
                "Player score updated successfully to $newScore"
            } catch (e: Exception) {
                logger.error(TAG, "Error updating player score", e)
                "Error updating player score: ${e.message}"
            }
        }
    }
}
