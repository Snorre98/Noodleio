package gr17.noodleio.game.models

import kotlinx.serialization.Serializable

@Serializable
data class PlayerPosition(
    val id: String,
    val player_id: String,
    val player_name: String,
    val x: Float,
    val y: Float,
    val room_id: String,
    val updated_at: String? = null,
    val created_at: String? = null
)
