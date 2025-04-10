package gr17.noodleio.game.models

import kotlinx.serialization.Serializable

/**
 * session_id is FK to GameSession, and is unique which creates a one-to-one relation.
 * player_id is FK and unique to LobbyPlayer, this with GameSession.lobby_id makes it so that only
 * --players for the lobby can be part of the game.
 * x_pos and y_pos tracks player position
 * The PlayerGameState uses realtime to update client with other players movement
 * */
@Serializable
data class PlayerGameState (
    val id: String,
    val session_id: String,
    val player_id: String,
    val x_pos: Float,
    val y_pos: Float,
    val score: Int,
)

/*
create table public."PlayerGameState" (
  id uuid not null default gen_random_uuid (),
  session_id uuid not null,
  player_id uuid not null,
  x_pos double precision not null,
  y_pos double precision not null,
  score bigint not null default '50'::bigint,
  constraint PlayerGameState_pkey primary key (id),
  constraint PlayerGameState_player_id_key unique (player_id),
  constraint PlayerGameState_session_id_key unique (session_id),
  constraint PlayerGameState_player_id_fkey foreign KEY (player_id) references "LobbyPlayer" (id) on delete RESTRICT,
  constraint PlayerGameState_session_id_fkey foreign KEY (session_id) references "GameSession" (id) on delete CASCADE
) TABLESPACE pg_default;
* */

/*
---- DB function (maybe client) ----
if (PlayerGameState.x_pos and PlayerGameState.y_pos) == (Food.x_pos and Food.y_pos)
when PlayerGameState.score === GameSession(session_id).winning_score the GameSession.ended_at should be set to now
 */
