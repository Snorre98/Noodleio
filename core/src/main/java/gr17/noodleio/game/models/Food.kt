package gr17.noodleio.game.models

import kotlinx.serialization.Serializable


/**
 * Food uses realtime to update all players on Food position
 * */
@Serializable
data class Food (
    val id: String,
    val session_id: String,
    val x_pos: Int,
    val y_pos: Int,
    val wasEaten: Boolean,
)

/*
---- DB function ----
delete instance if wasEaten

*/
