package gr17.noodleio.game.services

import gr17.noodleio.game.config.EnvironmentConfig
import gr17.noodleio.game.models.Lobby
import gr17.noodleio.game.models.LobbyPlayer
import gr17.noodleio.game.services.logging.ServiceLogger
import gr17.noodleio.game.services.logging.ServiceLoggerFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Service for managing lobby operations
 * Enables a player to create a lobby and start a game
 */
class LobbyService(environmentConfig: EnvironmentConfig) {

    private val logger: ServiceLogger = ServiceLoggerFactory.getLogger()
    private val serviceManager: ServiceManager = ServiceManager(environmentConfig)

    companion object {
        private const val TAG = "LobbyService"
    }

    // Data class for the response from create_lobby_with_owner RPC function
    @Serializable
    data class CreateLobbyWithOwnerResponse(
        @SerialName("lobby_id") val lobbyId: String,
        @SerialName("player_id") val playerId: String,
        @SerialName("player_name") val playerName: String,
        @SerialName("max_players") val maxPlayers: Int,
        @SerialName("success") val success: Boolean
    )

    /**
     * Creates a new lobby with a player as the owner
     * @param playerName The name of the player who will own the lobby
     * @param maxPlayers Maximum number of players allowed
     * @return A Pair containing the Lobby and LobbyPlayer objects, or null if there was an error
     */
    fun createLobbyWithOwner(playerName: String, maxPlayers: Int = 2): Pair<Lobby, LobbyPlayer>? {
        return runBlocking {
            try {
                logger.debug(TAG, "Creating lobby with owner: $playerName, max players: $maxPlayers")

                val params = buildJsonObject {
                    put("p_player_name", playerName)
                    put("p_max_players", maxPlayers)
                }

                val response = serviceManager.db.rpc("create_lobby_with_owner", params)
                val results = response.decodeList<CreateLobbyWithOwnerResponse>()

                if (results.isEmpty() || !results.first().success) {
                    logger.info(TAG, "Failed to create lobby with owner. Player name might already exist.")
                    return@runBlocking null
                }

                val result = results.first()

                // Create the Lobby and LobbyPlayer objects from the response
                val lobby = Lobby(
                    id = result.lobbyId,
                    lobby_owner = result.playerId,
                    max_players = result.maxPlayers,
                    created_at = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT)
                )

                val player = LobbyPlayer(
                    id = result.playerId,
                    player_name = result.playerName,
                    lobby_id = result.lobbyId,
                    joined_at = kotlinx.datetime.Clock.System.now()
                )

                logger.info(TAG, "Successfully created lobby ${lobby.id} with player ${player.player_name}")
                Pair(lobby, player)

            } catch (e: Exception) {
                logger.error(TAG, "Failed to create lobby with owner", e)
                null
            }
        }
    }
}
