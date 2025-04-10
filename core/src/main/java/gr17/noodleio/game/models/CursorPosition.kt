package gr17.noodleio.game.models

import kotlinx.serialization.Serializable

/*
*  This is only used for proof of concept!
* */
@Serializable
data class CursorPosition(
    val userId: String,
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
)
