package gr17.noodleio.game.models

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntry(
    val id: String,
    val player_name: String,
    val score: Int,
    val duration_seconds: Double? = null,
    val updated_at: String? = null,
    val created_at: String? = null
)

/** Leaderboard database table definition in Supabase **/
/*
create table public."Leaderboard" (
  id uuid not null default gen_random_uuid (),
  player_name character varying not null,
  score bigint null,
  updated_at timestamp with time zone null,
  created_at timestamp with time zone not null default now(),
  duration_seconds double precision null,
  constraint Leaderboard_pkey primary key (id),
  constraint Leaderboard_player_name_key unique (player_name)
) TABLESPACE pg_default;
* */
