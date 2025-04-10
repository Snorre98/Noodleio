package gr17.noodleio.game.models

import kotlinx.serialization.Serializable

/*
* This object is created when a user creates a lobby.
* The creator shares the lobby UUID.
* */
@Serializable
data class Lobby (
    val id: String, // lobby UUID
    val players: String? = null, // FK to SessionPlayers UUID, added when a player joins
    val created_at: String? = null
)

/**
    create table public."Lobby" (
        id uuid not null default gen_random_uuid (),
        players uuid null,
        created_at timestamp with time zone not null default now(),
        constraint SessionLobby_pkey primary key (id),
        constraint SessionLobby_players_fkey foreign KEY (players) references "LobbyPlayer" (id) on update RESTRICT on delete CASCADE
    ) TABLESPACE pg_default;
 */
