package gr17.noodleio.game.models

import kotlinx.serialization.Serializable

/*
* This object is created when a player joins
* When the object is created successfully the FK is added to the related Lobby
* */
@Serializable
data class SessionPlayer (
    val id: String, // UUID FK reference in SessionLobby
    val player: String, // Unique constrain
    val created_at: String? = null
)

/**
 * create table public."SessionPlayers" (
 *   id uuid not null default gen_random_uuid (),
 *   player character varying null,
 *   created_at timestamp with time zone not null default now(),
 *   constraint SessionPlayers_pkey primary key (id),
 *   constraint SessionPlayers_player_key unique (player)
 * ) TABLESPACE pg_default;
 */
