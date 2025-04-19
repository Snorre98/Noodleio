package gr17.noodleio.game.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * lobby_id is unique, creating a one-to-one from GameSession to Lobby.
 * winning_score is used to determine end state.
 * map_length and map_height is used to create a deterministic map boundary.
 * the GameSession table uses the realtime feature to signal when the game is over.
 * */
@Serializable
data class GameSession(
    val id: String,
    val lobby_id: String,
    val winning_score: Int,
    val map_length: Int,
    val map_height: Int,
    val started_at: Instant,
    val ended_at: Instant? = null
)


/*
create table public."GameSession" (
  id uuid not null default gen_random_uuid (),
  lobby_id uuid not null,
  winning_score bigint not null default '50'::bigint,
  map_length bigint not null default '1080'::bigint,
  map_height bigint null default '1080'::bigint,
  started_at timestamp with time zone not null default now(),
  ended_at timestamp with time zone null,
  constraint GameSession_pkey primary key (id),
  constraint GameSession_lobby_id_key unique (lobby_id),
  constraint GameSession_lobby_id_fkey foreign KEY (lobby_id) references "Lobby" (id)
) TABLESPACE pg_default;
* */


/*
---- DB function ----
none for now
*/

/*
local GameSession state includes:
- GameSession fields
- all player states for the lobby
* */


/*
------ Client function ----
it's important that lobby_id is kept locally so that we quickly can get

randomly place Food within the map over time as long as the game is in progress
if the game has a "ended_at" value players should be moved to the lobby screen
*/
