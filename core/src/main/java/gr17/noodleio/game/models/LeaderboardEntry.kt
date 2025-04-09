package gr17.noodleio.game.models

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntry(
    val id: String,
    val player_name: String,
    val score: Int,
    val updated_at: String? = null,
    val created_at: String? = null
)
