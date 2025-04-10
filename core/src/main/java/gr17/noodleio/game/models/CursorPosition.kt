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
/*
{
    // Add getters for Java interoperability
    fun getUserId(): String = userId
    fun getX(): Float = x
    fun getY(): Float = y
    fun getTimestamp(): Long = timestamp
}*/
