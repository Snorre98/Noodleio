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
---- DB function (maybe client) ----
if (PlayerGameState.x_pos and PlayerGameState.y_pos) == (Food.x_pos and Food.y_pos) --> PlayerGamestate.score ++
when PlayerGameState.score === GameSession(session_id).winning_score the GameSession.ended_at should be set to now
 */

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



/* DB Function
-- Function to move a player up (decrease y position by 1)
-- Only moves if the new position is within map boundaries
CREATE OR REPLACE FUNCTION move_up(
  p_player_id UUID,           -- The ID of the player to move
  p_session_id UUID           -- The ID of the game session
) RETURNS TABLE (
  success BOOLEAN,            -- Indicates if the operation was successful
  message TEXT,               -- Message explaining the result
  new_y_pos DOUBLE PRECISION  -- The new y position after movement
) LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
  v_current_y DOUBLE PRECISION;
  v_map_height BIGINT;
  v_player_state_id UUID;
BEGIN
  -- Check if player exists in this game session
  SELECT
    pgs.id,
    pgs.y_pos
  INTO
    v_player_state_id,
    v_current_y
  FROM "PlayerGameState" pgs
  WHERE pgs.player_id = p_player_id AND pgs.session_id = p_session_id;

  IF v_player_state_id IS NULL THEN
    RETURN QUERY SELECT
      FALSE AS success,
      'Player not found in this game session'::TEXT AS message,
      NULL::DOUBLE PRECISION AS new_y_pos;
    RETURN;
  END IF;

  -- Get map dimensions from game session
  SELECT gs.map_height INTO v_map_height
  FROM "GameSession" gs
  WHERE gs.id = p_session_id;

  -- Calculate new position (moving up decreases y position)
  -- Only move if the new position is within map boundaries
  -- Map coordinates start at (0,0) in bottom-left corner
  IF v_current_y > 0 THEN
    -- Update player position
    UPDATE "PlayerGameState"
    SET y_pos = y_pos - 1
    WHERE id = v_player_state_id
    RETURNING y_pos INTO v_current_y;

    RETURN QUERY SELECT
      TRUE AS success,
      'Moved up successfully'::TEXT AS message,
      v_current_y AS new_y_pos;
  ELSE
    -- Player is already at the top edge of the map
    RETURN QUERY SELECT
      FALSE AS success,
      'Cannot move up: player is at the map boundary'::TEXT AS message,
      v_current_y AS new_y_pos;
  END IF;
END;
$$;

* */


