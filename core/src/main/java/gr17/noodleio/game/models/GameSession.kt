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
  constraint GameSession_lobby_id_fkey foreign KEY (lobby_id) references "Lobby" (id) on delete CASCADE
) TABLESPACE pg_default;
* */




/*
local GameSession state includes:
- GameSession fields
- all player states for the lobby
* */


/*
------ Client function ----
It's important that lobby_id is kept locally so that we quickly can get game session and players by lobby_id.

Spawn enough food for all players to be able to win. If two players: spawn 2*50 food in random places.
- the random food generation is done on the lobby_owner client, and the data is pushed to the database by calling a db function spawn_food
    - this happens when the lobby_owner creates a game session
*/

/*
---- DB function ----
spawn_food takes a list of random food positions
*/
