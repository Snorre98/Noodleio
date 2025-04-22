package gr17.noodleio.game.services

import gr17.noodleio.game.config.EnvironmentConfig
import gr17.noodleio.game.models.GameSession
import gr17.noodleio.game.models.Lobby
import gr17.noodleio.game.models.LobbyPlayer
import gr17.noodleio.game.services.logging.ServiceLogger
import gr17.noodleio.game.services.logging.ServiceLoggerFactory
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Service for managing lobby player operations
 * Allows players to join lobbies and start game sessions
 */
class LobbyPlayerService(environmentConfig: EnvironmentConfig) {

    private val logger: ServiceLogger = ServiceLoggerFactory.getLogger()
    private val serviceManager: ServiceManager = ServiceManager(environmentConfig)

    companion object {
        private const val TAG = "LobbyPlayerService"
    }

    /**
     * Allows a player to join a lobby by its ID
     * @param playerName The name of the player who wants to join
     * @param lobbyId The ID of the lobby to join
     * @return The created LobbyPlayer or null if there was an error
     */
    fun joinLobby(playerName: String, lobbyId: String): LobbyPlayer? {
        logger.debug(TAG, "Starting joinLobby with playerName=$playerName, lobbyId=$lobbyId")

        return runBlocking {
            try {
                // Check if player name is already taken
                logger.debug(TAG, "Checking if player name is already taken")
                val existingPlayerQuery = serviceManager.db
                    .from("LobbyPlayer")
                    .select {
                        filter {
                            eq("player_name", playerName)
                        }
                    }

                try {
                    val existingPlayers = existingPlayerQuery.decodeList<LobbyPlayer>()
                    if (existingPlayers.isNotEmpty()) {
                        logger.info(TAG, "Player name '$playerName' is already taken")
                        return@runBlocking null
                    }
                } catch (e: Exception) {
                    logger.debug(TAG, "No existing players found with name '$playerName'")
                }

                // Check if lobby exists
                logger.debug(TAG, "Checking if lobby exists")
                val lobbyQuery = serviceManager.db
                    .from("Lobby")
                    .select {
                        filter {
                            eq("id", lobbyId)
                        }
                    }

                try {
                    val lobbies = lobbyQuery.decodeList<Lobby>()
                    if (lobbies.isEmpty()) {
                        logger.debug(TAG, "Lobby with ID '$lobbyId' does not exist")
                        return@runBlocking null
                    }

                    val lobby = lobbies.first()

                    // Check if lobby is full
                    logger.debug(TAG, "Checking if lobby is full")
                    val playersInLobby = serviceManager.db
                        .from("LobbyPlayer")
                        .select {
                            filter {
                                eq("lobby_id", lobbyId)
                            }
                        }
                        .decodeList<LobbyPlayer>()

                    if (playersInLobby.size >= lobby.max_players) {
                        logger.debug(TAG, "Lobby is full (${playersInLobby.size}/${lobby.max_players})")
                        return@runBlocking null
                    }

                } catch (e: Exception) {
                    logger.error(TAG, "Error checking lobby: ${e.message}", e)
                    return@runBlocking null
                }

                // Try to insert a player
                logger.debug(TAG, "Creating player entry")

                try {
                    val jsonData = buildJsonObject {
                        put("player_name", playerName)
                        put("lobby_id", lobbyId)
                    }

                    logger.debug(TAG, "Inserting player with data: $jsonData")
                    val response = serviceManager.db
                        .from("LobbyPlayer")
                        .insert(jsonData)

                    try {
                        val player = response.decodeSingle<LobbyPlayer>()
                        logger.info(TAG, "Player '${player.player_name}' joined lobby '${player.lobby_id}' successfully")
                        return@runBlocking player
                    } catch (e: Exception) {
                        logger.debug(TAG, "Failed to decode response, likely empty: ${e.message}")

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
                            logger.debug(TAG, "Found player after insertion: ${player.player_name}")
                            return@runBlocking player
                        }

                        logger.debug(TAG, "Player not found after insertion attempt, creating placeholder")
                        return@runBlocking LobbyPlayer(
                            id = UUID.randomUUID().toString(),
                            player_name = playerName,
                            lobby_id = lobbyId,
                            joined_at = Clock.System.now()
                        )
                    }
                } catch (e: Exception) {
                    logger.error(TAG, "Error inserting player: ${e.message}", e)
                    return@runBlocking null
                }
            } catch (e: Exception) {
                logger.error(TAG, "Error in joinLobby: ${e.message}", e)
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
        logger.debug(TAG, "Getting players in lobby $lobbyId")

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
                    logger.debug(TAG, "Found ${players.size} players in lobby '$lobbyId'")
                    return@runBlocking players
                } catch (e: Exception) {
                    logger.error(TAG, "Error decoding players: ${e.message}", e)
                    return@runBlocking emptyList()
                }
            } catch (e: Exception) {
                logger.error(TAG, "Error getting players in lobby: ${e.message}", e)
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
        logger.debug(TAG, "Removing player $playerId from lobby")

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
                        logger.debug(TAG, "No player with ID '$playerId' found to remove")
                        return@runBlocking false
                    }

                    logger.info(TAG, "Player with ID '$playerId' removed from lobby")
                    return@runBlocking true
                } catch (e: Exception) {
                    logger.error(TAG, "Error decoding deletion response: ${e.message}", e)
                    return@runBlocking false
                }
            } catch (e: Exception) {
                logger.error(TAG, "Error removing player from lobby: ${e.message}", e)
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
        logger.debug(TAG, "Getting player by ID $playerId")

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
                        logger.debug(TAG, "No player found with ID '$playerId'")
                        return@runBlocking null
                    }

                    val player = players.first()
                    logger.debug(TAG, "Found player '${player.player_name}' with ID '${player.id}'")
                    return@runBlocking player
                } catch (e: Exception) {
                    logger.error(TAG, "Error decoding player query: ${e.message}", e)
                    return@runBlocking null
                }
            } catch (e: Exception) {
                logger.error(TAG, "Error getting player by ID: ${e.message}", e)
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
                val params = buildJsonObject {
                    put("p_player_id", playerId)
                    put("p_lobby_id", lobbyId)
                    put("p_winning_score", winningScore)
                    put("p_map_length", mapLength)
                    put("p_map_height", mapHeight)
                }

                val response = serviceManager.db.rpc("start_game_session", params)
                val results = response.decodeList<StartGameSessionResponse>()

                if (results.isEmpty()) {
                    return@runBlocking Pair(null, "Failed to start game session: No response from server")
                }

                val result = results.first()

                if (!result.success || result.sessionId == null) {
                    logger.info(TAG, "Failed to start game session: ${result.message}")
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
                    logger.info(TAG, "Successfully started game session with ID: ${gameSession.id}")
                    return@runBlocking Pair(gameSession, "Game session started successfully")
                } catch (e: Exception) {
                    logger.info(TAG, "Game session was created but couldn't be retrieved: ${e.message}")

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
                logger.error(TAG, "Error starting game session: ${e.message}", e)
                return@runBlocking Pair(null, "Error starting game session: ${e.message}")
            }
        }
    }

    fun checkActiveGameSession(lobbyId: String): String {
        return runBlocking {
            try {
                val response = serviceManager.db
                    .from("GameSession")
                    .select {
                        filter {
                            eq("lobby_id", lobbyId)
                            exact("ended_at", null)  // Only active sessions
                        }
                    }

                try {
                    val sessions = response.decodeList<GameSession>()
                    if (sessions.isNotEmpty()) {
                        val session = sessions.first()
                        "Active session found - session_id: ${session.id}"
                    } else {
                        "No active game session found"
                    }
                } catch (e: Exception) {
                    "Error decoding game sessions: ${e.message}"
                }
            } catch (e: Exception) {
                "Error checking for active game session: ${e.message}"
            }
        }
    }

    fun getPlayerIdFromName(playerName: String): String {
        return runBlocking {
            try {
                val response = serviceManager.db
                    .from("LobbyPlayer")
                    .select {
                        filter {
                            eq("player_name", playerName)
                        }
                    }

                try {
                    val players = response.decodeList<LobbyPlayer>()
                    if (players.isNotEmpty()) {
                        players.first().id
                    } else {
                        ""
                    }
                } catch (e: Exception) {
                    ""
                }
            } catch (e: Exception) {
                ""
            }
        }
    }

    fun isLobbyOwner(playerId: String, lobbyId: String): Boolean {
        return runBlocking {
            try {
                val lobbyResponse = serviceManager.db
                    .from("Lobby")
                    .select {
                        filter {
                            eq("id", lobbyId)
                        }
                    }

                val lobbies = lobbyResponse.decodeList<Lobby>()
                if (lobbies.isNotEmpty()) {
                    val lobby = lobbies.first()
                    lobby.lobby_owner == playerId
                } else {
                    false
                }
            } catch (e: Exception) {
                logger.error(TAG, "Error checking if player is lobby owner: ${e.message}", e)
                false
            }
        }
    }
}
