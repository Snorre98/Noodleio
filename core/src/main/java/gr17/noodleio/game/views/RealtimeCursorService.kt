package gr17.noodleio.game.views

import gr17.noodleio.game.config.EnvironmentConfig
import gr17.noodleio.game.models.CursorPosition
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcast
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


/*
*  This is only used for proof of concept!
* */
class RealtimeCursorService(private val environmentConfig: EnvironmentConfig) : CoroutineScope {
    interface CursorPositionListener {
        fun onCursorPositionReceived(position: CursorPosition)
    }
    private val listeners = mutableListOf<CursorPositionListener>()

    fun addListener(listener: CursorPositionListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: CursorPositionListener) {
        listeners.remove(listener)
    }


    // Create service manager with our config
    private val serviceManager: ServiceManager = ServiceManager(environmentConfig)

    // Channel for cursor position
    private var cursorChannel: RealtimeChannel? = null

    // Flow for cursor position updates
    private val cursorPositionFlow = MutableSharedFlow<CursorPosition>()

    // Unique ID for this user
    private val userId = "user-" + System.currentTimeMillis()

    // Connection status
    private var isConnected = false
    private var lastError: String? = null

    // Coroutine context for async operations
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + SupervisorJob()

    /**
     * Initialize and connect to the realtime channel
     * @param channelName Name of the channel to join (e.g., "cursor-room")
     * @return Status message
     */
    fun connect(channelName: String): String {
        return try {
            // Create a channel
            cursorChannel = serviceManager.realtime.channel(channelName)

            // Launch a coroutine to subscribe to the channel
            launch {
                try {
                    // Subscribe to the channel (this is a suspend function)
                    cursorChannel?.subscribe(blockUntilSubscribed = true)

                    isConnected = true
                    println("Connected to realtime channel: $channelName")

                    // Set up listening for cursor positions from other clients
                    setupCursorPositionListener()
                } catch (e: Exception) {
                    lastError = e.message
                    isConnected = false
                    println("Error subscribing to channel: ${e.message}")
                }
            }

            "Connecting to channel: $channelName"
        } catch (e: Exception) {
            lastError = e.message
            isConnected = false
            "Failed to connect: ${e.message}"
        }
    }

    /**
     * Set up listener for cursor positions from other clients
     */
    private suspend fun setupCursorPositionListener() {
        cursorChannel?.let { channel ->
            try {
                val broadcastFlow = channel.broadcastFlow<CursorPosition>(event = "cursor_position")

                broadcastFlow.onEach { position ->
                    // Skip our own cursor positions
                    if (position.userId != userId) {
                        println("Received cursor position: ${position.x}, ${position.y} from ${position.userId}")

                        // Forward to our own flow for external subscribers
                        cursorPositionFlow.emit(position)

                        // Notify listeners
                        listeners.forEach { it.onCursorPositionReceived(position) }
                    }
                }.launchIn(this)

                println("Successfully set up cursor position listener")
            } catch (e: Exception) {
                println("Error setting up cursor position listener: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Send cursor position to all connected clients
     * @param x X coordinate
     * @param y Y coordinate
     * @return Status message
     */
    fun sendCursorPosition(x: Float, y: Float): String {
        if (!isConnected || cursorChannel == null) {
            return "Not connected to channel"
        }

        val cursorPosition = CursorPosition(userId, x, y)

        return try {
            // Launch coroutine to send the message
            launch {
                cursorChannel?.broadcast(
                    event = "cursor_position",
                    message = cursorPosition
                )
            }

            "Sent cursor position: x=$x, y=$y"
        } catch (e: Exception) {
            lastError = e.message
            "Failed to send cursor position: ${e.message}"
        }
    }

    /**
     * Disconnect from the channel
     */
    /**
     * Disconnect from the channel
     */
    fun disconnect(): String {
        return try {
            // Launch coroutine to unsubscribe from the channel
            launch {
                cursorChannel?.unsubscribe()
            }
            isConnected = false
            cursorChannel = null
            "Disconnected from channel"
        } catch (e: Exception) {
            lastError = e.message
            "Failed to disconnect: ${e.message}"
        }
    }

    /**
     * Get connection status
     */
    fun getConnectionStatus(): String {
        return if (isConnected) {
            "Connected"
        } else {
            "Not connected" + (lastError?.let { ": $it" } ?: "")
        }
    }

    /**
     * Get user ID
     */
    fun getUserId(): String {
        return userId
    }
}
