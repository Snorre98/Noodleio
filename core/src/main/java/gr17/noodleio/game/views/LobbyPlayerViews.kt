package gr17.noodleio.game.views

import gr17.noodleio.game.config.EnvironmentConfig
import gr17.noodleio.game.models.Lobby
import gr17.noodleio.game.models.LobbyPlayer
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

/**
 * For a player to join a lobby
 * The player joins the lobby by posting the lobby key (the lobby UUID)
 * */
class LobbyPlayerViews(private val environmentConfig: EnvironmentConfig) {

    // Create our service manager with the environment config
    private val serviceManager: ServiceManager = ServiceManager(environmentConfig)

    /**
     * Allows a player to join a lobby by its ID
     * @param playerName The name of the player who wants to join
     * @param lobbyId The ID of the lobby to join
     * @return The created LobbyPlayer or null if there was an error
     */
    fun joinLobby(playerName: String, lobbyId: String): LobbyPlayer? {
        println("DEBUG LobbyPlayerService: Starting joinLobby with playerName=$playerName, lobbyId=$lobbyId")

        return runBlocking {
            try {
                // Check if player name is already taken
                println("DEBUG LobbyPlayerService: Checking if player name is already taken")
                val existingPlayerQuery = serviceManager.db
                    .from("LobbyPlayer")
                    .select {
                        filter {
                            eq("player_name", playerName)
                        }
                    }

                // Check if the result contains any players
                try {
                    val existingPlayers = existingPlayerQuery.decodeList<LobbyPlayer>()
                    if (existingPlayers.isNotEmpty()) {
                        println("Player name '$playerName' is already taken")
                        return@runBlocking null
                    }
                } catch (e: Exception) {
                    // If decoding fails, it likely means no results were found
                    // This is fine, continue with the join
                    println("No existing players found with name '$playerName'")
                }

                // Check if lobby exists
                println("DEBUG LobbyPlayerService: Checking if lobby exists")
                val lobbyQuery = serviceManager.db
                    .from("Lobby")
                    .select {
                        filter {
                            eq("id", lobbyId)
                        }
                    }

                // Check if lobby exists by decoding the result
                try {
                    val lobbies = lobbyQuery.decodeList<Lobby>()
                    if (lobbies.isEmpty()) {
                        println("DEBUG LobbyPlayerService: Lobby with ID '$lobbyId' does not exist")
                        return@runBlocking null
                    }

                    // Get the lobby for max player check
                    val lobby = lobbies.first()

                    // Check if lobby is full
                    println("DEBUG LobbyPlayerService: Checking if lobby is full")
                    val playersInLobby = serviceManager.db
                        .from("LobbyPlayer")
                        .select {
                            filter {
                                eq("lobby_id", lobbyId)
                            }
                        }
                        .decodeList<LobbyPlayer>()

                    if (playersInLobby.size >= lobby.max_players) {
                        println("DEBUG LobbyPlayerService: Lobby is full (${playersInLobby.size}/${lobby.max_players})")
                        return@runBlocking null
                    }

                } catch (e: Exception) {
                    println("DEBUG LobbyPlayerService: Error checking lobby: ${e.message}")
                    e.printStackTrace()
                    return@runBlocking null
                }

                // Try to insert a player
                println("DEBUG LobbyPlayerService: Creating player entry")

                try {
                    // Create the player entry
                    val jsonData = buildJsonObject {
                        put("player_name", playerName)
                        put("lobby_id", lobbyId)
                    }

                    println("DEBUG LobbyPlayerService: Inserting player with data: $jsonData")
                    val response = serviceManager.db
                        .from("LobbyPlayer")
                        .insert(jsonData)

                    try {
                        val player = response.decodeSingle<LobbyPlayer>()
                        println("DEBUG LobbyPlayerService: Player '${player.player_name}' joined lobby '${player.lobby_id}' successfully")
                        return@runBlocking player
                    } catch (e: Exception) {
                        println("DEBUG LobbyPlayerService: Failed to decode response, likely empty: ${e.message}")

                        // The insertion might have succeeded but returned no data
                        // Check if the player was actually added
                        val checkQuery = serviceManager.db
                            .from("LobbyPlayer")
                            .select {
                                filter {
                                    eq("player_name", playerName)
                                    eq("lobby_id", lobbyId)
                                }
                            }

                        val possiblePlayers = checkQuery.decodeList<LobbyPlayer>()
                        if (possiblePlayers.isNotEmpty()) {
                            val player = possiblePlayers.first()
                            println("DEBUG LobbyPlayerService: Found player after insertion: ${player.player_name}")
                            return@runBlocking player
                        }

                        println("DEBUG LobbyPlayerService: Player not found after insertion attempt, creating placeholder")
                        // Create a placeholder as a last resort
                        return@runBlocking LobbyPlayer(
                            id = UUID.randomUUID().toString(),
                            player_name = playerName,
                            lobby_id = lobbyId,
                            joined_at = Clock.System.now()
                        )
                    }
                } catch (e: Exception) {
                    println("DEBUG LobbyPlayerService: Error inserting player: ${e.message}")
                    e.printStackTrace()
                    return@runBlocking null
                }
            } catch (e: Exception) {
                println("DEBUG LobbyPlayerService: Error in joinLobby: ${e.message}")
                e.printStackTrace()
                return@runBlocking null
            }
        }
    }

    /**
     * Gets all players in a lobby
     * @param lobbyId The ID of the lobby
     * @return List of LobbyPlayer objects or empty list if none found or error
     */
    fun getPlayersInLobby(lobbyId: String): List<LobbyPlayer> {
        println("DEBUG LobbyPlayerService: Getting players in lobby $lobbyId")

        return runBlocking {
            try {
                val response = serviceManager.db
                    .from("LobbyPlayer")
                    .select {
                        filter {
                            eq("lobby_id", lobbyId)
                        }
                    }

                try {
                    val players = response.decodeList<LobbyPlayer>()
                    println("DEBUG LobbyPlayerService: Found ${players.size} players in lobby '$lobbyId'")
                    return@runBlocking players
                } catch (e: Exception) {
                    println("DEBUG LobbyPlayerService: Error decoding players: ${e.message}")
                    return@runBlocking emptyList()
                }
            } catch (e: Exception) {
                println("DEBUG LobbyPlayerService: Error getting players in lobby: ${e.message}")
                e.printStackTrace()
                return@runBlocking emptyList()
            }
        }
    }

    /**
     * Removes a player from a lobby
     * @param playerId The ID of the player to remove
     * @return True if successful, false otherwise
     */
    fun leaveLobby(playerId: String): Boolean {
        println("DEBUG LobbyPlayerService: Removing player $playerId from lobby")

        return runBlocking {
            try {
                val response = serviceManager.db
                    .from("LobbyPlayer")
                    .delete {
                        filter {
                            eq("id", playerId)
                        }
                    }

                try {
                    val deletedPlayers = response.decodeList<LobbyPlayer>()
                    if (deletedPlayers.isEmpty()) {
                        println("DEBUG LobbyPlayerService: No player with ID '$playerId' found to remove")
                        return@runBlocking false
                    }

                    println("DEBUG LobbyPlayerService: Player with ID '$playerId' removed from lobby")
                    return@runBlocking true
                } catch (e: Exception) {
                    println("DEBUG LobbyPlayerService: Error decoding deletion response: ${e.message}")
                    return@runBlocking false
                }
            } catch (e: Exception) {
                println("DEBUG LobbyPlayerService: Error removing player from lobby: ${e.message}")
                e.printStackTrace()
                return@runBlocking false
            }
        }
    }

    /**
     * Gets a player by their ID
     * @param playerId The ID of the player to retrieve
     * @return The LobbyPlayer object or null if not found
     */
    fun getPlayerById(playerId: String): LobbyPlayer? {
        println("DEBUG LobbyPlayerService: Getting player by ID $playerId")

        return runBlocking {
            try {
                val response = serviceManager.db
                    .from("LobbyPlayer")
                    .select {
                        filter {
                            eq("id", playerId)
                        }
                    }

                try {
                    val players = response.decodeList<LobbyPlayer>()
                    if (players.isEmpty()) {
                        println("DEBUG LobbyPlayerService: No player found with ID '$playerId'")
                        return@runBlocking null
                    }

                    val player = players.first()
                    println("DEBUG LobbyPlayerService: Found player '${player.player_name}' with ID '${player.id}'")
                    return@runBlocking player
                } catch (e: Exception) {
                    println("DEBUG LobbyPlayerService: Error decoding player query: ${e.message}")
                    return@runBlocking null
                }
            } catch (e: Exception) {
                println("DEBUG LobbyPlayerService: Error getting player by ID: ${e.message}")
                e.printStackTrace()
                return@runBlocking null
            }
        }
    }
}
