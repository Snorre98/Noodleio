package gr17.noodleio.game.models

import kotlinx.serialization.Serializable

/**
 * lobby_owner is used for validation that a player can start a game session
 * max_players is used to make sure that no more than this amount can join the lobby
 * */
@Serializable
data class Lobby (
    val id: String,
    val lobby_owner: String,
    val max_players: Int,
    val created_at: String
)

/*

create table public."Lobby" (
  id uuid not null default gen_random_uuid (),
  max_players bigint not null default '4'::bigint,
  created_at timestamp with time zone not null default now(),
  lobby_owner uuid null,
  constraint Lobby_pkey primary key (id),
  constraint Lobby_lobby_owner_fkey foreign KEY (lobby_owner) references "LobbyPlayer" (id) on update RESTRICT
) TABLESPACE pg_default;

*/

/*
---- DB function ----
creating a Lobby automatically creates a LobbyPLayer (the creator)
 */

