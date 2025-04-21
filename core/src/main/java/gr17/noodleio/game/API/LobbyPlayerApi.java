package gr17.noodleio.game.API;

import com.badlogic.gdx.Gdx;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.models.LobbyPlayer;
import gr17.noodleio.game.views.LobbyPlayerViews;

import java.util.List;

public class LobbyPlayerApi {
    private final LobbyPlayerViews lobbyPlayerViews;
    private String joinLobbyMessage = "";
    private String playersListMessage = "";
    private String leaveLobbyMessage = "";

    private String startGameSessionMessage = "";

    public LobbyPlayerApi(EnvironmentConfig environmentConfig) {
        this.lobbyPlayerViews = new LobbyPlayerViews(environmentConfig);
    }

    /**
     * Allows a player to join a lobby by its ID
     * @param playerName The name of the player who wants to join
     * @param lobbyId The ID of the lobby to join
     * @return Status message indicating success or failure
     */
    public String joinLobby(String playerName, String lobbyId) {
        try {
            LobbyPlayer player = lobbyPlayerViews.joinLobby(playerName, lobbyId);

            if (player != null) {
                joinLobbyMessage = "Player '" + playerName + "' successfully joined lobby with ID: " + lobbyId +
                    " | Player ID: " + player.getId();
                return joinLobbyMessage;
            } else {
                joinLobbyMessage = "Failed to join lobby. The player name might already be taken, " +
                    "the lobby might not exist, or the lobby might be full.";
                return joinLobbyMessage;
            }
        } catch (Exception e) {
            joinLobbyMessage = "Error joining lobby: " + e.getMessage();
            e.printStackTrace();
            return joinLobbyMessage;
        }
    }

    /**
     * Creates a player with random name and joins a lobby (useful for testing)
     * @param lobbyId The ID of the lobby to join
     * @return Status message indicating success or failure
     */
    public String joinLobbyWithRandomPlayer(String lobbyId) {
        try {
            // Generate a player name with timestamp to ensure uniqueness
            String playerName = "TestPlayer_" + System.currentTimeMillis();
            return joinLobby(playerName, lobbyId);
        } catch (Exception e) {
            joinLobbyMessage = "Error creating random player: " + e.getMessage();
            e.printStackTrace();
            return joinLobbyMessage;
        }
    }

    /**
     * Gets all players in a lobby
     * @param lobbyId The ID of the lobby
     * @return Status message with list of players
     */
    public String getPlayersInLobby(String lobbyId) {
        try {
            List<LobbyPlayer> players = lobbyPlayerViews.getPlayersInLobby(lobbyId);

            StringBuilder sb = new StringBuilder("Players in lobby:\n");

            if (players.isEmpty()) {
                sb.append("No players found in this lobby");
            } else {
                for (LobbyPlayer player : players) {
                    // Only include the player name, removing ID and joined_at
                    sb.append("- ")
                        .append(player.getPlayer_name())
                        .append("\n");
                }
                sb.append("Total players: ").append(players.size());
            }

            playersListMessage = sb.toString();
            return playersListMessage;
        } catch (Exception e) {
            playersListMessage = "Error retrieving players in lobby: " + e.getMessage();
            e.printStackTrace();
            return playersListMessage;
        }
    }

    /**
     * Removes a player from a lobby
     * @param playerId The ID of the player to remove
     * @return Status message indicating success or failure
     */
    public String leaveLobby(String playerId) {
        try {
            boolean success = lobbyPlayerViews.leaveLobby(playerId);

            if (success) {
                leaveLobbyMessage = "Player successfully left the lobby";
                return leaveLobbyMessage;
            } else {
                leaveLobbyMessage = "Failed to leave lobby. Player may not exist.";
                return leaveLobbyMessage;
            }
        } catch (Exception e) {
            leaveLobbyMessage = "Error leaving lobby: " + e.getMessage();
            e.printStackTrace();
            return leaveLobbyMessage;
        }
    }

    /**
     * Gets a player by their ID
     * @param playerId The ID of the player to retrieve
     * @return Status message with player details if found
     */
    public String getPlayerById(String playerId) {
        try {
            LobbyPlayer player = lobbyPlayerViews.getPlayerById(playerId);

            if (player != null) {
                return "Player found: " + player.getPlayer_name() +
                    " (ID: " + player.getId() +
                    ", Lobby: " + player.getLobby_id() +
                    ", Joined: " + player.getJoined_at() + ")";
            } else {
                return "No player found with ID: " + playerId;
            }
        } catch (Exception e) {
            String errorMsg = "Error retrieving player: " + e.getMessage();
            e.printStackTrace();
            return errorMsg;
        }
    }


    /**
     * Starts a game session for a lobby
     * Only the lobby owner can start a game session
     *
     * @param playerId The ID of the player trying to start the game (must be lobby owner)
     * @param lobbyId The ID of the lobby to create a game session for
     * @param winningScore Score required to win (default: 50)
     * @param mapLength Map length (default: 1080)
     * @param mapHeight Map height (default: 1080)
     * @return Status message indicating success or failure
     */
    public String startGameSession(String playerId, String lobbyId, int winningScore, int mapLength, int mapHeight) {
        try {
            kotlin.Pair<gr17.noodleio.game.models.GameSession, String> result =
                lobbyPlayerViews.startGameSession(playerId, lobbyId, winningScore, mapLength, mapHeight);

            gr17.noodleio.game.models.GameSession gameSession = result.getFirst();
            String message = result.getSecond();

            if (gameSession != null) {
                startGameSessionMessage = "Game session started successfully: " +
                    "ID: " + gameSession.getId() +
                    ", Lobby: " + gameSession.getLobby_id() +
                    ", Winning Score: " + gameSession.getWinning_score();
                return startGameSessionMessage;
            } else {
                startGameSessionMessage = "Failed to start game session: " + message;
                return startGameSessionMessage;
            }
        } catch (Exception e) {
            startGameSessionMessage = "Error starting game session: " + e.getMessage();
            e.printStackTrace();
            return startGameSessionMessage;
        }
    }

    /**
     * Starts a game session for a lobby with default settings
     *
     * @param playerId The ID of the player trying to start the game (must be lobby owner)
     * @param lobbyId The ID of the lobby to create a game session for
     * @return Status message indicating success or failure
     */
    public String startGameSession(String playerId, String lobbyId) {
        return startGameSession(playerId, lobbyId, 10, 1080, 1080);
    }

    public String checkActiveGameSession(String lobbyId) {
        try {
            // Check if an active game session exists for this lobby
            return lobbyPlayerViews.checkActiveGameSession(lobbyId);
        } catch (Exception e) {
            Gdx.app.error("LobbyPlayerApi", "Error checking for active game session", e);
            return null;
        }
    }

    public String getPlayerIdFromName(String playerName) {
        try {
            // Get player ID from player name
            return lobbyPlayerViews.getPlayerIdFromName(playerName);
        } catch (Exception e) {
            Gdx.app.error("LobbyPlayerApi", "Error getting player ID from name", e);
            return null;
        }
    }

    public boolean isLobbyOwner(String playerId, String lobbyId) {
        try {
            return lobbyPlayerViews.isLobbyOwner(playerId, lobbyId);
        } catch (Exception e) {
            Gdx.app.error("LobbyPlayerApi", "Error checking if player is lobby owner", e);
            return false;
        }
    }

    /**
     * Gets the most recent start game session message
     * @return The start game session status message
     */
    public String getStartGameSessionMessage() {
        return startGameSessionMessage;
    }

    /**
     * Gets the most recent join lobby message
     * @return The join lobby status message
     */
    public String getJoinLobbyMessage() {
        return joinLobbyMessage;
    }

    /**
     * Gets the most recent players list message
     * @return The players list message
     */
    public String getPlayersListMessage() {
        return playersListMessage;
    }

    /**
     * Gets the most recent leave lobby message
     * @return The leave lobby status message
     */
    public String getLeaveLobbyMessage() {
        return leaveLobbyMessage;
    }

    /**
     * Gets the underlying LobbyPlayerService
     * @return The LobbyPlayerService instance
     */
    public LobbyPlayerViews getLobbyPlayerService() {
        return lobbyPlayerViews;
    }
}
