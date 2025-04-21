package gr17.noodleio.game.models

import kotlinx.serialization.Serializable


/**
 * Food uses realtime to update all players on Food position
 * */
@Serializable
data class Food (
    // TODO: delete or use
    val id: String,
    val session_id: String,
    val x_pos: Int,
    val y_pos: Int,
    val was_eaten: Boolean,
)

/** Food database table definition in Supabase **/
/*
  create table public."Food" (
  id uuid not null default gen_random_uuid (),
  session_id uuid not null,
  x_pos bigint not null,
  y_pos bigint not null,
  was_eaten boolean not null default false,
  created_at timestamp with time zone not null default now(),
  constraint Food_pkey primary key (id),
  constraint Food_session_id_fkey foreign KEY (session_id) references "GameSession" (id) on delete CASCADE
) TABLESPACE pg_default;
*
* */
