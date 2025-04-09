package gr17.noodleio.game.models

import kotlinx.serialization.Serializable

/* Only used for testing */
@Serializable
data class CursorPosition(
    val userId: String,
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
)
