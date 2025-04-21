package gr17.noodleio.game.services

import gr17.noodleio.game.config.EnvironmentConfig
import gr17.noodleio.game.models.Lobby
import gr17.noodleio.game.models.LobbyPlayer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Service for managing a lobby.
 * Enables a player to create a lobby and start a game.
 * When a player creates a lobby it is also added to LobbyPlayer
 * */
class LobbyService(environmentConfig: EnvironmentConfig) {

    // Create our service manager with the environment config
    private val serviceManager: ServiceManager = ServiceManager(environmentConfig)

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
                // Call the database function using RPC
                val params = buildJsonObject {
                    put("p_player_name", playerName)
                    put("p_max_players", maxPlayers)
                }

                val response = serviceManager.db.rpc("create_lobby_with_owner", params)
                val results = response.decodeList<CreateLobbyWithOwnerResponse>()

                if (results.isEmpty() || !results.first().success) {
                    println("Failed to create lobby with owner. Player name might already exist.")
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

                println("Successfully created lobby with ID ${lobby.id} and player ${player.player_name}")
                Pair(lobby, player)

            } catch (e: Exception) {
                println("Error creating lobby with owner: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
}
