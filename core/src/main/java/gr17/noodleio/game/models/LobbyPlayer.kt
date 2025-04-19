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
Handled separately: players in the lobby will be subscribed to the GameSession table and the PlayerGameState table
 */


/*
*
---- DB function code----

-- Function to start a game session for a lobby
-- Only the lobby owner can start a game session
CREATE OR REPLACE FUNCTION start_game_session(
  p_player_id UUID,           -- The ID of the player trying to start the game
  p_lobby_id UUID,            -- The ID of the lobby to start a game for
  p_winning_score INT DEFAULT 50, -- Score required to win (default: 50)
  p_map_length INT DEFAULT 1080,  -- Map length (default: 1080)
  p_map_height INT DEFAULT 1080   -- Map height (default: 1080)
) RETURNS TABLE (
  session_id UUID,            -- Returns the new game session ID
  lobby_id UUID,              -- Returns the lobby ID
  success BOOLEAN,            -- Indicates if the operation was successful
  message TEXT                -- Message explaining the result
) LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
  v_lobby_owner UUID;
  v_new_session_id UUID;
  v_existing_session UUID;
BEGIN
  -- Check if the lobby exists
  IF NOT EXISTS (SELECT 1 FROM "Lobby" WHERE id = p_lobby_id) THEN
    RETURN QUERY SELECT
      NULL::UUID AS session_id,
      p_lobby_id AS lobby_id,
      FALSE AS success,
      'Lobby does not exist'::TEXT AS message;
    RETURN;
  END IF;

  -- Get the lobby owner
  SELECT lobby_owner INTO v_lobby_owner
  FROM "Lobby"
  WHERE id = p_lobby_id;

  -- Check if player is the lobby owner
  IF v_lobby_owner IS NULL OR v_lobby_owner != p_player_id THEN
    RETURN QUERY SELECT
      NULL::UUID AS session_id,
      p_lobby_id AS lobby_id,
      FALSE AS success,
      'Only the lobby owner can start a game session'::TEXT AS message;
    RETURN;
  END IF;

  -- Check if there's already an active game session for this lobby
  SELECT gs.id INTO v_existing_session
  FROM "GameSession" gs
  WHERE gs.lobby_id = p_lobby_id AND gs.ended_at IS NULL;

  IF v_existing_session IS NOT NULL THEN
    RETURN QUERY SELECT
      v_existing_session AS session_id,
      p_lobby_id AS lobby_id,
      FALSE AS success,
      'An active game session already exists for this lobby'::TEXT AS message;
    RETURN;
  END IF;

  -- Start a transaction to ensure all operations complete or none do
  BEGIN
    -- Create a new game session
    INSERT INTO "GameSession" (
      lobby_id,
      winning_score,
      map_length,
      map_height
    ) VALUES (
      p_lobby_id,
      p_winning_score,
      p_map_length,
      p_map_height
    ) RETURNING id INTO v_new_session_id;

    -- For each player in the lobby, create a player game state
    INSERT INTO "PlayerGameState" (
      session_id,
      player_id,
      x_pos,
      y_pos,
      score
    )
    SELECT
      v_new_session_id,
      lp.id,
      0.0, -- Starting x position
      0.0, -- Starting y position
      0    -- Starting score
    FROM "LobbyPlayer" lp
    WHERE lp.lobby_id = p_lobby_id;

    -- Return success with the new game session details
    RETURN QUERY SELECT
      v_new_session_id AS session_id,
      p_lobby_id AS lobby_id,
      TRUE AS success,
      'Game session started successfully'::TEXT AS message;

  EXCEPTION
    WHEN OTHERS THEN
      -- If any error occurs, rollback and return failure
      RAISE LOG 'Error in start_game_session: %', SQLERRM;
      RETURN QUERY SELECT
        NULL::UUID AS session_id,
        p_lobby_id AS lobby_id,
        FALSE AS success,
        'Error starting game session: ' || SQLERRM::TEXT AS message;
  END;
END;
$$;
*
*
*
* */
