package gr17.noodleio.game.models

import kotlinx.serialization.Serializable

@Serializable
data class GameSession(
    val id: String,
    val player: String,
    val x: Float,
    val y: Float,
    val lobby_id: String,
    val created_at: String? = null
)

/**
 * create table public."GameSession" (
 * id uuid not null default gen_random_uuid (),
 * player character varying not null,
 * x double precision null,
 * y bigint null,
 * lobby_id uuid null,
 * created_at timestamp with time zone not null default now(),
 * constraint GameSession_pkey primary key (id),
 * constraint GameSession_player_key unique (player),
 * constraint GameSession_lobby_id_fkey foreign KEY (lobby_id) references "Lobby" (id) on update RESTRICT on delete CASCADE,
 * constraint GameSession_player_fkey foreign KEY (player) references "LobbyPlayers" (player) on update RESTRICT on delete CASCADE
 * ) TABLESPACE pg_default;
 *
 *
 *
 * */
