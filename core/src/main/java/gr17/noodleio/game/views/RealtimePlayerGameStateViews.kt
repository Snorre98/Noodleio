package gr17.noodleio.game.views

import gr17.noodleio.game.config.EnvironmentConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RealtimePlayerGameStateViews(private val environmentConfig: EnvironmentConfig) {
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

}
