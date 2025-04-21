package gr17.noodleio.game.views

import gr17.noodleio.game.config.EnvironmentConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class PlayerGameStateViews(private val environmentConfig: EnvironmentConfig) {
    // Create our service manager with the environment config
    private val serviceManager: ServiceManager = ServiceManager(environmentConfig)

    /**
     * Data class for the response from move_up RPC function
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
        println("DEBUG PlayerMovementViews: Moving player $playerId up in session $sessionId")

        return runBlocking {
            try {
                // Build the parameters for the RPC call
                val params = buildJsonObject {
                    put("p_player_id", playerId)
                    put("p_session_id", sessionId)
                }

                // Call the database function using RPC
                val response = serviceManager.db.rpc("move_up", params)
                val results = response.decodeList<MoveResponse>()

                if (results.isEmpty()) {
                    println("DEBUG PlayerMovementViews: No response from move_up function")
                    return@runBlocking Pair(false, "Failed to move player: No response from server")
                }

                val result = results.first()
                println("DEBUG PlayerMovementViews: Move up result: success=${result.success}, message=${result.message}, new position=${result.newYPos}")

                return@runBlocking Pair(result.success, result.message + (if (result.newYPos != null) " (New Y: ${result.newYPos})" else ""))
            } catch (e: Exception) {
                println("DEBUG PlayerMovementViews: Error in movePlayerUp: ${e.message}")
                e.printStackTrace()
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
        println("DEBUG PlayerMovementViews: Moving player $playerId down in session $sessionId")

        return runBlocking {
            try {
                // Build the parameters for the RPC call
                val params = buildJsonObject {
                    put("p_player_id", playerId)
                    put("p_session_id", sessionId)
                }

                // Call the database function using RPC
                val response = serviceManager.db.rpc("move_down", params)
                val results = response.decodeList<MoveResponse>()

                if (results.isEmpty()) {
                    println("DEBUG PlayerMovementViews: No response from move_down function")
                    return@runBlocking Pair(false, "Failed to move player: No response from server")
                }

                val result = results.first()
                println("DEBUG PlayerMovementViews: Move down result: success=${result.success}, message=${result.message}, new position=${result.newYPos}")

                return@runBlocking Pair(result.success, result.message + (if (result.newYPos != null) " (New Y: ${result.newYPos})" else ""))
            } catch (e: Exception) {
                println("DEBUG PlayerMovementViews: Error in movePlayerDown: ${e.message}")
                e.printStackTrace()
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
        println("DEBUG PlayerMovementViews: Moving player $playerId left in session $sessionId")

        return runBlocking {
            try {
                // Build the parameters for the RPC call
                val params = buildJsonObject {
                    put("p_player_id", playerId)
                    put("p_session_id", sessionId)
                }

                // Call the database function using RPC
                val response = serviceManager.db.rpc("move_left", params)
                val results = response.decodeList<MoveResponse>()

                if (results.isEmpty()) {
                    println("DEBUG PlayerMovementViews: No response from move_left function")
                    return@runBlocking Pair(false, "Failed to move player: No response from server")
                }

                val result = results.first()
                println("DEBUG PlayerMovementViews: Move left result: success=${result.success}, message=${result.message}, new position=${result.newXPos}")

                return@runBlocking Pair(result.success, result.message + (if (result.newXPos != null) " (New X: ${result.newXPos})" else ""))
            } catch (e: Exception) {
                println("DEBUG PlayerMovementViews: Error in movePlayerLeft: ${e.message}")
                e.printStackTrace()
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
        println("DEBUG PlayerMovementViews: Moving player $playerId right in session $sessionId")

        return runBlocking {
            try {
                // Build the parameters for the RPC call
                val params = buildJsonObject {
                    put("p_player_id", playerId)
                    put("p_session_id", sessionId)
                }

                // Call the database function using RPC
                val response = serviceManager.db.rpc("move_right", params)
                val results = response.decodeList<MoveResponse>()

                if (results.isEmpty()) {
                    println("DEBUG PlayerMovementViews: No response from move_right function")
                    return@runBlocking Pair(false, "Failed to move player: No response from server")
                }

                val result = results.first()
                println("DEBUG PlayerMovementViews: Move right result: success=${result.success}, message=${result.message}, new position=${result.newXPos}")

                return@runBlocking Pair(result.success, result.message + (if (result.newXPos != null) " (New X: ${result.newXPos})" else ""))
            } catch (e: Exception) {
                println("DEBUG PlayerMovementViews: Error in movePlayerRight: ${e.message}")
                e.printStackTrace()
                return@runBlocking Pair(false, "Error moving player right: ${e.message}")
            }
        }
    }

    fun updatePlayerScore(playerId: String, sessionId: String, newScore: Int): String {
        println("DEBUG PlayerGameStateViews: Updating player $playerId score to $newScore in session $sessionId")

        return runBlocking {
            try {
                // Build the parameters for the update
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

                "Player score updated successfully to $newScore"
            } catch (e: Exception) {
                println("DEBUG PlayerGameStateViews: Error updating player score: ${e.message}")
                e.printStackTrace()
                "Error updating player score: ${e.message}"
            }
        }
    }

}
