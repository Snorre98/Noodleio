package gr17.noodleio.game.services

import gr17.noodleio.game.config.EnvironmentConfig
import gr17.noodleio.game.models.GameSession
import gr17.noodleio.game.models.PlayerGameState
import gr17.noodleio.game.services.logging.ServiceLogger
import gr17.noodleio.game.services.logging.ServiceLoggerFactory
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * Service for receiving real-time game state updates
 * Handles synchronization of player positions and game state
 */
class RealtimeGameStateService(environmentConfig: EnvironmentConfig) : CoroutineScope {

    private val logger: ServiceLogger = ServiceLoggerFactory.getLogger()
    private val serviceManager: ServiceManager = ServiceManager(environmentConfig)

    companion object {
        private const val TAG = "RealtimeGameStateService"
    }

    // Coroutine context for async operations
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()

    // Realtime channels
    private var playerStateChannel: RealtimeChannel? = null
    private var gameSessionChannel: RealtimeChannel? = null

    // Connection status
    private var isConnected = false
    private var lastError: String? = null

    // Game state data
    private val playerStates = ConcurrentHashMap<String, PlayerGameState>()
    private var currentSession: GameSession? = null

    // Local player info
    private var localPlayerId: String? = null
    private var sessionId: String? = null

    // Listeners
    interface GameStateListener {
        fun onPlayerStateChanged(playerState: PlayerGameState)
        fun onGameSessionChanged(gameSession: GameSession)
        fun onGameOver()
    }

    private val listeners = mutableListOf<GameStateListener>()

    fun addListener(listener: GameStateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: GameStateListener) {
        //TODO: remove or use
        listeners.remove(listener)
    }

    /**
     * Initialize and connect to the realtime channels for a game session
     * @param sessionId ID of the game session
     * @param playerId ID of the local player
     * @return Status message
     */
    fun connect(sessionId: String, playerId: String): String {
        return try {
            this.sessionId = sessionId
            this.localPlayerId = playerId

            // Create channels for different data types
            playerStateChannel = serviceManager.realtime.channel("player-state-$sessionId")
            gameSessionChannel = serviceManager.realtime.channel("game-session-$sessionId")

            // Subscribe to channels in a coroutine
            launch {
                try {
                    // Set up listeners BEFORE subscribing to channels
                    setupPlayerStateListener()
                    setupGameSessionListener()

                    // Now subscribe to the channels
                    playerStateChannel?.subscribe(blockUntilSubscribed = true)
                    gameSessionChannel?.subscribe(blockUntilSubscribed = true)

                    // Initial data load
                    loadInitialGameState()

                    isConnected = true
                    logger.info(TAG, "Connected to game session: $sessionId")
                } catch (e: Exception) {
                    lastError = e.message
                    isConnected = false
                    logger.error(TAG, "Error subscribing to channels", e)
                }
            }

            "Connecting to game session: $sessionId"
        } catch (e: Exception) {
            lastError = e.message
            isConnected = false
            logger.error(TAG, "Failed to connect", e)
            "Failed to connect: ${e.message}"
        }
    }

    /**
     * Load initial game state from database
     */
    private suspend fun loadInitialGameState() {
        try {
            // Load game session
            val gameSessionResponse = serviceManager.db
                .from("GameSession")
                .select {
                    filter {
                        eq("id", sessionId!!)
                    }
                }

            val gameSession = gameSessionResponse.decodeSingle<GameSession>()
            currentSession = gameSession

            // Notify listeners about initial game session
            listeners.forEach { it.onGameSessionChanged(gameSession) }

            // Load all player states
            val playerStateResponse = serviceManager.db
                .from("PlayerGameState")
                .select {
                    filter {
                        eq("session_id", sessionId!!)
                    }
                }

            val playerStateList = playerStateResponse.decodeList<PlayerGameState>()
            for (state in playerStateList) {
                playerStates[state.player_id] = state
                // Notify listeners
                listeners.forEach { it.onPlayerStateChanged(state) }
                logger.debug(TAG, "Initial player state: player_id=${state.player_id}, x=${state.x_pos}, y=${state.y_pos}")
            }

            logger.info(TAG, "Initial game state loaded")
        } catch (e: Exception) {
            logger.error(TAG, "Error loading initial game state", e)
        }
    }

    /**
     * Set up listener for player state changes
     */
    private fun setupPlayerStateListener() {
        playerStateChannel?.let { channel ->
            try {
                val playerChanges = channel.postgresChangeFlow<PostgresAction>(
                    schema = "public"
                ) {
                    table = "PlayerGameState"
                    filter("session_id", FilterOperator.EQ, sessionId!!)
                }

                playerChanges.onEach { action ->
                    try {
                        when (action) {
                            is PostgresAction.Insert -> {
                                val record = action.record
                                val playerState = PlayerGameState(
                                    id = record["id"].toString(),
                                    session_id = record["session_id"].toString(),
                                    player_id = record["player_id"].toString(),
                                    x_pos = extractNumberValue(record["x_pos"]).toFloat(),
                                    y_pos = extractNumberValue(record["y_pos"]).toFloat(),
                                    score = extractNumberValue(record["score"]).toInt()
                                )

                                playerStates[playerState.player_id] = playerState
                                listeners.forEach { it.onPlayerStateChanged(playerState) }

                                logger.debug(TAG, "New player joined: ${playerState.player_id}, position: (${playerState.x_pos}, ${playerState.y_pos})")
                            }

                            is PostgresAction.Update -> {
                                val record = action.record
                                val playerId = record["player_id"].toString()
                                val prevState = playerStates[playerId]

                                val playerState = PlayerGameState(
                                    id = record["id"].toString(),
                                    session_id = record["session_id"].toString(),
                                    player_id = playerId,
                                    x_pos = extractNumberValue(record["x_pos"]).toFloat(),
                                    y_pos = extractNumberValue(record["y_pos"]).toFloat(),
                                    score = extractNumberValue(record["score"]).toInt()
                                )

                                playerStates[playerId] = playerState
                                listeners.forEach { it.onPlayerStateChanged(playerState) }

                                if (logger.isDebugEnabled()) {
                                    if (prevState != null) {
                                        logger.debug(TAG, "Player state updated: $playerId, position: (${prevState.x_pos}, ${prevState.y_pos}) -> (${playerState.x_pos}, ${playerState.y_pos})")
                                    } else {
                                        logger.debug(TAG, "Player state updated: $playerId, position: (${playerState.x_pos}, ${playerState.y_pos})")
                                    }
                                }
                            }

                            is PostgresAction.Delete -> {
                                val oldRecord = action.oldRecord
                                val playerId = oldRecord["player_id"].toString()
                                playerStates.remove(playerId)
                                logger.debug(TAG, "Player left: $playerId")
                            }
                            else -> {
                                logger.debug(TAG, "Unhandled action type: ${action::class.simpleName}")
                            }
                        }
                    } catch (e: Exception) {
                        logger.error(TAG, "Error processing player state change", e)
                    }
                }.launchIn(this)

                logger.debug(TAG, "Successfully set up player state listener")
            } catch (e: Exception) {
                logger.error(TAG, "Error setting up player state listener", e)
            }
        }
    }

    /**
     * Set up listener for game session changes
     */
    private fun setupGameSessionListener() {
        gameSessionChannel?.let { channel ->
            try {
                val changes = channel.postgresChangeFlow<PostgresAction.Update>(
                    schema = "public"
                ) {
                    table = "GameSession"
                    filter("id", FilterOperator.EQ, sessionId!!)
                }

                changes.onEach { update ->
                    try {
                        val record = update.record
                        val endedAt = record["ended_at"]

                        // Check if game has ended
                        if (endedAt != null) {
                            listeners.forEach { it.onGameOver() }
                            logger.info(TAG, "Game over!")
                        }

                        // Update session details
                        val gameSession = GameSession(
                            id = record["id"].toString(),
                            lobby_id = record["lobby_id"].toString(),
                            winning_score = extractNumberValue(record["winning_score"]).toInt(),
                            map_length = extractNumberValue(record["map_length"]).toInt(),
                            map_height = extractNumberValue(record["map_height"]).toInt(),
                            started_at = currentSession?.started_at ?: kotlinx.datetime.Clock.System.now(),
                            ended_at = if (endedAt != null) kotlinx.datetime.Clock.System.now() else null
                        )

                        currentSession = gameSession
                        listeners.forEach { it.onGameSessionChanged(gameSession) }

                        logger.debug(TAG, "Game session updated")
                    } catch (e: Exception) {
                        logger.error(TAG, "Error processing game session update", e)
                    }
                }.launchIn(this)

                logger.debug(TAG, "Successfully set up game session listener")
            } catch (e: Exception) {
                logger.error(TAG, "Error setting up game session listener", e)
            }
        }
    }

    /**
     * Disconnect from all channels
     */
    fun disconnect(): String {
        return try {
            launch {
                playerStateChannel?.unsubscribe()
                gameSessionChannel?.unsubscribe()
            }

            isConnected = false
            localPlayerId = null
            sessionId = null

            // Clear state
            playerStates.clear()
            currentSession = null

            logger.info(TAG, "Disconnected from game session")
            "Disconnected from game session"
        } catch (e: Exception) {
            lastError = e.message
            logger.error(TAG, "Failed to disconnect", e)
            "Failed to disconnect: ${e.message}"
        }
    }

    /**
     * Extract numeric value from various types
     */
    private fun extractNumberValue(value: Any?): Number {
        return when (value) {
            is Number -> value
            else -> {
                try {
                    value.toString().toDoubleOrNull() ?: 0.0
                } catch (e: Exception) {
                    0.0
                }
            }
        }
    }

    /**
     * Get all current player states
     */
    fun getPlayerStates(): Map<String, PlayerGameState> {
        return playerStates.toMap()
    }
}
