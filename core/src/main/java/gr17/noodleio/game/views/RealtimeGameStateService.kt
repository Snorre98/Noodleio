package gr17.noodleio.game.views

import gr17.noodleio.game.config.EnvironmentConfig
import gr17.noodleio.game.models.GameSession
import gr17.noodleio.game.models.PlayerGameState
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
 * This is a one-way service - client only receives updates from server
 */
class RealtimeGameStateService(private val environmentConfig: EnvironmentConfig) : CoroutineScope {

    private fun extractNumberValue(value: Any?): Number {
        return when (value) {
            is Number -> value
            else -> {
                try {
                    // Try to convert the value to a string and then to a double
                    value.toString().toDoubleOrNull() ?: 0.0
                } catch (e: Exception) {
                    0.0 // Default value if parsing fails
                }
            }
        }
    }

    // Coroutine context for async operations
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()

    // Create service manager with our config
    private val serviceManager: ServiceManager = ServiceManager(environmentConfig)

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
                    println("Connected to game session: $sessionId")
                } catch (e: Exception) {
                    lastError = e.message
                    isConnected = false
                    println("Error subscribing to channels: ${e.message}")
                    e.printStackTrace()
                }
            }

            "Connecting to game session: $sessionId"
        } catch (e: Exception) {
            lastError = e.message
            isConnected = false
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
                println("Initial player state: player_id=${state.player_id}, x=${state.x_pos}, y=${state.y_pos}")
            }

            println("Initial game state loaded")
        } catch (e: Exception) {
            println("Error loading initial game state: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Set up listener for player state changes
     */
    private fun setupPlayerStateListener() {
        playerStateChannel?.let { channel ->
            try {
                // Listen for all PostgresAction types on PlayerGameState table
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

                                // Update local state
                                playerStates[playerState.player_id] = playerState

                                // Notify listeners
                                listeners.forEach { it.onPlayerStateChanged(playerState) }

                                println("New player joined: ${playerState.player_id}, position: (${playerState.x_pos}, ${playerState.y_pos})")
                            }

                            is PostgresAction.Update -> {
                                val record = action.record

                                // Extract player_id from record
                                val playerId = record["player_id"].toString()

                                // Extract x and y positions, using correct type conversion
                                val xPos = extractNumberValue(record["x_pos"]).toFloat()
                                val yPos = extractNumberValue(record["y_pos"]).toFloat()
                                val score = extractNumberValue(record["score"]).toInt()

                                val playerState = PlayerGameState(
                                    id = record["id"].toString(),
                                    session_id = record["session_id"].toString(),
                                    player_id = playerId,
                                    x_pos = xPos,
                                    y_pos = yPos,
                                    score = score
                                )

                                // Get previous state for logging
                                val prevState = playerStates[playerId]

                                // Update local state
                                playerStates[playerId] = playerState

                                // Notify listeners
                                listeners.forEach { it.onPlayerStateChanged(playerState) }

                                // Log the update with previous and new position
                                if (prevState != null) {
                                    println("Player state updated: $playerId, position: (${prevState.x_pos}, ${prevState.y_pos}) -> (${playerState.x_pos}, ${playerState.y_pos})")
                                } else {
                                    println("Player state updated: $playerId, position: (${playerState.x_pos}, ${playerState.y_pos})")
                                }
                            }

                            is PostgresAction.Delete -> {
                                val oldRecord = action.oldRecord
                                val playerId = oldRecord["player_id"].toString()

                                // Remove from local state
                                playerStates.remove(playerId)

                                println("Player left: $playerId")
                            }
                            else -> {
                                println("Unhandled action type: ${action::class.simpleName}")
                            }
                        }
                    } catch (e: Exception) {
                        println("Error processing player state change: ${e.message}")
                        e.printStackTrace()
                    }
                }.launchIn(this)

                println("Successfully set up player state listener")
            } catch (e: Exception) {
                println("Error setting up player state listener: ${e.message}")
                e.printStackTrace()
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
                            // Game over notification
                            listeners.forEach { it.onGameOver() }
                            println("Game over!")
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

                        // Notify listeners
                        listeners.forEach { it.onGameSessionChanged(gameSession) }

                        println("Game session updated")
                    } catch (e: Exception) {
                        println("Error processing game session update: ${e.message}")
                        e.printStackTrace()
                    }
                }.launchIn(this)

                println("Successfully set up game session listener")
            } catch (e: Exception) {
                println("Error setting up game session listener: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Disconnect from all channels
     */
    fun disconnect(): String {
        return try {
            // Launch coroutine to unsubscribe from channels
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

            "Disconnected from game session"
        } catch (e: Exception) {
            lastError = e.message
            "Failed to disconnect: ${e.message}"
        }
    }

    /**
     * Get all current player states
     */
    fun getPlayerStates(): Map<String, PlayerGameState> {
        return playerStates.toMap()
    }

    /**
     * Get current game session
     */
    fun getCurrentSession(): GameSession? {
        return currentSession
    }

    /**
     * Get connection status
     */
    fun getConnectionStatus(): String {
        return if (isConnected) {
            "Connected to game session: $sessionId"
        } else {
            "Not connected" + (lastError?.let { ": $it" } ?: "")
        }
    }
}
