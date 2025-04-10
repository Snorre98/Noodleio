package gr17.noodleio.game.services

import gr17.noodleio.game.config.EnvironmentConfig
import gr17.noodleio.game.models.Lobby
import gr17.noodleio.game.models.LobbyPlayer
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * For a player to join a lobby
 * The player joins the lobby by posting the lobby key (the lobby UUID)
 * */
class LobbyPlayerService(private val environmentConfig: EnvironmentConfig) {

    // Create our service manager with the environment config
    private val serviceManager: ServiceManager = ServiceManager(environmentConfig)

    // Response class for join lobby operations
    @Serializable
    data class JoinLobbyResponse(
        @SerialName("player_id") val playerId: String,
        @SerialName("player_name") val playerName: String,
        @SerialName("lobby_id") val lobbyId: String,
        @SerialName("success") val success: Boolean,
        @SerialName("message") val message: String? = null
    )

    /**
     * Allows a player to join a lobby by its ID
     * @param playerName The name of the player who wants to join
     * @param lobbyId The ID of the lobby to join
     * @return The created LobbyPlayer or null if there was an error
     */
    fun joinLobby(playerName: String, lobbyId: String): LobbyPlayer? {
        return runBlocking {
            try {
                // Check if player name is already taken
                val existingPlayerQuery = serviceManager.db
                    .from("LobbyPlayer")
                    .select {
                        filter {
                            eq("player_name", playerName)
                        }
                    }

                if (existingPlayerQuery.toString() != "[]") {
                    println("Player name '$playerName' is already taken")
                    return@runBlocking null
                }

                // Check if lobby exists
                val lobbyQuery = serviceManager.db
                    .from("Lobby")
                    .select {
                        filter {
                            eq("id", lobbyId)
                        }
                    }

                if (lobbyQuery.toString() == "[]") {
                    println("Lobby with ID '$lobbyId' does not exist")
                    return@runBlocking null
                }

                // Check if lobby is full by counting players
                val playersInLobby = serviceManager.db
                    .from("LobbyPlayer")
                    .select {
                        filter {
                            eq("lobby_id", lobbyId)
                        }
                    }
                    .decodeList<LobbyPlayer>()

                val playerCount = playersInLobby.size

                // Get max_players for the lobby
                val lobby = lobbyQuery.decodeSingle<Lobby>()
                if (playerCount >= lobby.max_players) {
                    println("Lobby is full (${playerCount}/${lobby.max_players})")
                    return@runBlocking null
                }

                // Create the player entry
                val jsonData = buildJsonObject {
                    put("player_name", playerName)
                    put("lobby_id", lobbyId)
                }

                val response = serviceManager.db
                    .from("LobbyPlayer")
                    .insert(jsonData)

                val player = response.decodeSingle<LobbyPlayer>()
                println("Player '${player.player_name}' joined lobby '${player.lobby_id}' successfully")
                player
            } catch (e: Exception) {
                println("Error joining lobby: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Gets all players in a lobby
     * @param lobbyId The ID of the lobby
     * @return List of LobbyPlayer objects or empty list if none found or error
     */
    fun getPlayersInLobby(lobbyId: String): List<LobbyPlayer> {
        return runBlocking {
            try {
                val response = serviceManager.db
                    .from("LobbyPlayer")
                    .select {
                        filter {
                            eq("lobby_id", lobbyId)
                        }
                    }

                val players = response.decodeList<LobbyPlayer>()
                println("Found ${players.size} players in lobby '$lobbyId'")
                players
            } catch (e: Exception) {
                println("Error getting players in lobby: ${e.message}")
                e.printStackTrace()
                emptyList()
            }
        }
    }

    /**
     * Removes a player from a lobby
     * @param playerId The ID of the player to remove
     * @return True if successful, false otherwise
     */
    fun leaveLobby(playerId: String): Boolean {
        return runBlocking {
            try {
                val response = serviceManager.db
                    .from("LobbyPlayer")
                    .delete {
                        filter {
                            eq("id", playerId)
                        }
                    }

                // If response is empty, the player might not exist
                if (response.toString() == "[]") {
                    println("No player with ID '$playerId' found to remove")
                    return@runBlocking false
                }

                println("Player with ID '$playerId' removed from lobby")
                true
            } catch (e: Exception) {
                println("Error removing player from lobby: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Gets a player by their ID
     * @param playerId The ID of the player to retrieve
     * @return The LobbyPlayer object or null if not found
     */
    fun getPlayerById(playerId: String): LobbyPlayer? {
        return runBlocking {
            try {
                val response = serviceManager.db
                    .from("LobbyPlayer")
                    .select {
                        filter {
                            eq("id", playerId)
                        }
                    }

                if (response.toString() == "[]") {
                    println("No player found with ID '$playerId'")
                    return@runBlocking null
                }

                val player = response.decodeSingle<LobbyPlayer>()
                println("Found player '${player.player_name}' with ID '${player.id}'")
                player
            } catch (e: Exception) {
                println("Error getting player by ID: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
}
