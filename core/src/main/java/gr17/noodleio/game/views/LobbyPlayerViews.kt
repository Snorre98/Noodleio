package gr17.noodleio.game.views

import gr17.noodleio.game.config.EnvironmentConfig
import gr17.noodleio.game.models.GameSession
import gr17.noodleio.game.models.Lobby
import gr17.noodleio.game.models.LobbyPlayer
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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


    /**
     * Data class for the response from start_game_session RPC function
     */
    @Serializable
    data class StartGameSessionResponse(
        @SerialName("session_id") val sessionId: String?,
        @SerialName("lobby_id") val lobbyId: String,
        @SerialName("success") val success: Boolean,
        @SerialName("message") val message: String
    )

    /**
     * Allows a player to start a game session for their lobby
     * Only the lobby owner can start a game session
     *
     * @param playerId The ID of the player trying to start the game (must be lobby owner)
     * @param lobbyId The ID of the lobby to create a game session for
     * @param winningScore Score required to win (default: 50)
     * @param mapLength Map length (default: 1080)
     * @param mapHeight Map height (default: 1080)
     * @return The created GameSession or null if there was an error
     */
    fun startGameSession(
        playerId: String,
        lobbyId: String,
        winningScore: Int = 50,
        mapLength: Int = 1080,
        mapHeight: Int = 1080
    ): Pair<GameSession?, String> {
        return runBlocking {
            try {
                // Build the parameters for the RPC call
                val params = buildJsonObject {
                    put("p_player_id", playerId)
                    put("p_lobby_id", lobbyId)
                    put("p_winning_score", winningScore)
                    put("p_map_length", mapLength)
                    put("p_map_height", mapHeight)
                }

                // Call the database function using RPC
                val response = serviceManager.db.rpc("start_game_session", params)
                val results = response.decodeList<StartGameSessionResponse>()

                if (results.isEmpty()) {
                    return@runBlocking Pair(null, "Failed to start game session: No response from server")
                }

                val result = results.first()

                if (!result.success || result.sessionId == null) {
                    println("Failed to start game session: ${result.message}")
                    return@runBlocking Pair(null, result.message)
                }

                // Try to fetch the newly created game session
                try {
                    val gameSessionResponse = serviceManager.db
                        .from("GameSession")
                        .select {
                            filter {
                                eq("id", result.sessionId)
                            }
                        }

                    val gameSession = gameSessionResponse.decodeSingle<GameSession>()
                    println("Successfully started game session with ID: ${gameSession.id}")
                    return@runBlocking Pair(gameSession, "Game session started successfully")
                } catch (e: Exception) {
                    println("Game session was created but couldn't be retrieved: ${e.message}")

                    // Create a basic session object with the ID we know
                    val gameSession = GameSession(
                        id = result.sessionId,
                        lobby_id = result.lobbyId,
                        winning_score = winningScore,
                        map_length = mapLength,
                        map_height = mapHeight,
                        started_at = kotlinx.datetime.Clock.System.now()
                    )

                    return@runBlocking Pair(gameSession, "Game session started successfully, but details couldn't be retrieved")
                }
            } catch (e: Exception) {
                println("Error starting game session: ${e.message}")
                e.printStackTrace()
                return@runBlocking Pair(null, "Error starting game session: ${e.message}")
            }
        }
    }
}
