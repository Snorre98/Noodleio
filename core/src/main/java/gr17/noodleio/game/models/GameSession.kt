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

CREATE OR REPLACE FUNCTION check_winning_score() RETURNS TRIGGER AS $$
BEGIN
  -- Check if the player's score equals or exceeds the winning score
  IF NEW.score >= (SELECT winning_score FROM "GameSession" WHERE id = NEW.session_id) THEN
    -- Update the GameSession with ended_at timestamp if not already set
    UPDATE "GameSession"
    SET ended_at = NOW()
    WHERE id = NEW.session_id AND ended_at IS NULL;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create a trigger to run this function whenever a player's score is updated
CREATE TRIGGER check_win_condition
AFTER UPDATE OF score ON "PlayerGameState"
FOR EACH ROW
EXECUTE FUNCTION check_winning_score();

* */
