package gr17.noodleio.game.models

import kotlinx.serialization.Serializable

/**
 * lobby_owner is used for validation that a player can start a game session
 * max_players is used to make sure that no more than this amount can join the lobby
 * */
@Serializable
data class Lobby (
    val id: String,
    val lobby_owner: String? = null,
    val max_players: Int,
    val created_at: String
)


/** Lobby database table definition in Supabase **/
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


/** DB server-side functions in Supabase **/
/*
-- Function to create a new lobby and add a player as the owner in one operation
create or replace function create_lobby_with_owner (
  p_player_name VARCHAR, -- The player's name
  p_max_players INT default 4 -- Maximum number of players allowed (default: 4)
) RETURNS table (
  lobby_id UUID, -- Returns the new lobby ID
  player_id UUID, -- Returns the new player's ID
  player_name VARCHAR, -- Returns the player name
  max_players INT, -- Returns the max players setting
  success BOOLEAN -- Indicates if the operation was successful
) LANGUAGE plpgsql SECURITY DEFINER as $$
DECLARE
  new_lobby_id UUID;
  new_player_id UUID;
BEGIN
  -- Check if the player name is already taken
  IF EXISTS (SELECT 1 FROM "LobbyPlayer" WHERE "LobbyPlayer".player_name = p_player_name) THEN
    -- Return indicating failure if player name already exists
    RETURN QUERY SELECT
      NULL::UUID AS lobby_id,
      NULL::UUID AS player_id,
      p_player_name AS player_name,
      p_max_players AS max_players,
      FALSE AS success;
    RETURN;
  END IF;

  -- Start a transaction to ensure all operations complete or none do
  BEGIN
    -- Create a new lobby
    INSERT INTO "Lobby" (max_players)
    VALUES (p_max_players)
    RETURNING id INTO new_lobby_id;

    -- Create a new player and add to the lobby
    INSERT INTO "LobbyPlayer" (player_name, lobby_id)
    VALUES (p_player_name, new_lobby_id)
    RETURNING id INTO new_player_id;

    -- Set the player as the lobby owner
    UPDATE "Lobby"
    SET lobby_owner = new_player_id
    WHERE id = new_lobby_id;

    -- Return success with the new lobby and player details
    RETURN QUERY SELECT
      new_lobby_id AS lobby_id,
      new_player_id AS player_id,
      p_player_name AS player_name,
      p_max_players AS max_players,
      TRUE AS success;

  EXCEPTION
    WHEN OTHERS THEN
      -- If any error occurs, rollback and return failure
      RAISE LOG 'Error in create_lobby_with_owner: %', SQLERRM;
      RETURN QUERY SELECT
        NULL::UUID AS lobby_id,
        NULL::UUID AS player_id,
        p_player_name AS player_name,
        p_max_players AS max_players,
        FALSE AS success;
  END;
END;
$$;
 */

