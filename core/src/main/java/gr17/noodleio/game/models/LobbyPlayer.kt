package gr17.noodleio.game.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * player_name is unique for the purpose of the leaderboard.
 * lobby_id is not unique, creating one-to-many from lobby to player.
 * */
@Serializable
data class LobbyPlayer (
    val id: String,
    val player_name: String,
    val lobby_id: String,
    val joined_at: Instant,
)

/*

create table public."LobbyPlayer" (
  id uuid not null default gen_random_uuid (),
  player_name character varying not null,
  lobby_id uuid not null,
  joined_at timestamp with time zone not null default now(),
  constraint LobbyPlayer_pkey primary key (id),
  constraint LobbyPlayer_player_name_key unique (player_name),
  constraint LobbyPlayer_lobby_id_fkey foreign KEY (lobby_id) references "Lobby" (id)
) TABLESPACE pg_default;

* */

/*
---- DB function ----
the owner (Lobby.lobby_owner == LobbyPlayer.id) can start a GameSession
with all the players in the lobby
these will be subscribed to the GameSession table and the PlayerGameState table
 */
