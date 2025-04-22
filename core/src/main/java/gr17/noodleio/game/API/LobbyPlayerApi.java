package gr17.noodleio.game.API;

import java.util.List;

import com.badlogic.gdx.Gdx;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.models.LobbyPlayer;
import gr17.noodleio.game.services.LobbyPlayerService;
import gr17.noodleio.game.services.LobbyService;

public class LobbyPlayerApi {
    private final LobbyPlayerService lobbyPlayerService;
    private final LobbyService lobbyService;

    public LobbyPlayerApi(EnvironmentConfig environmentConfig) {
        this.lobbyPlayerService = new LobbyPlayerService(environmentConfig);
        this.lobbyService = new LobbyService(environmentConfig);
    }

    /**
     * Allows a player to join a lobby by its ID
     * Now supports partial lobby IDs (first 5 characters) for easier joining
     * 
     * @param playerName The name of the player who wants to join
     * @param lobbyId The ID of the lobby to join (full ID or just first 5 characters)
     * @return Status message indicating success or failure
     */
    public String joinLobby(String playerName, String lobbyId) {
        String joinLobbyMessage = "";
        try {
            LobbyPlayer player = lobbyPlayerService.joinLobby(playerName, lobbyId);

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
     * Gets all players in a lobby
     * Now supports partial lobby IDs
     * 
     * @param lobbyId The ID of the lobby (full ID or just first 5 characters)
     * @return Status message with list of players
     */
    public String getPlayersInLobby(String lobbyId) {
        String playersListMessage = "";
        try {
            List<LobbyPlayer> players = lobbyPlayerService.getPlayersInLobby(lobbyId);

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
        String leaveLobbyMessage = "";
        try {
            kotlin.Pair<Boolean, Boolean> result = lobbyPlayerService.leaveLobby(playerId);
            boolean success = result.getFirst();
            boolean wasLobbyOwner = result.getSecond();

            if (success) {
                // If the player was the lobby owner, delete the lobby
                if (wasLobbyOwner) {
                    // Get lobby ID from player
                    LobbyPlayer player = lobbyPlayerService.getPlayerById(playerId);
                    if (player != null) {
                        String lobbyId = player.getLobby_id();
                        
                        // Delete the lobby
                        boolean lobbyDeleted = lobbyService.deleteLobby(lobbyId);
                        if (lobbyDeleted) {
                            leaveLobbyMessage = "Player (owner) successfully left the lobby. Lobby has been deleted.";
                        } else {
                            leaveLobbyMessage = "Player (owner) successfully left the lobby, but failed to delete the lobby.";
                        }
                        return leaveLobbyMessage;
                    }
                }
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
     * Deletes a lobby directly
     * @param lobbyId The ID of the lobby to delete
     * @return Status message indicating success or failure
     */
    public String deleteLobby(String lobbyId) {
        try {
            boolean success = lobbyService.deleteLobby(lobbyId);
            if (success) {
                return "Lobby successfully deleted";
            } else {
                return "Failed to delete lobby. Lobby may not exist.";
            }
        } catch (Exception e) {
            String errorMsg = "Error deleting lobby: " + e.getMessage();
            e.printStackTrace();
            return errorMsg;
        }
    }

    /**
     * Gets a player by their ID
     * @param playerId The ID of the player to retrieve
     * @return Status message with player details if found
     */
    public String getPlayerById(String playerId) {
        try {
            LobbyPlayer player = lobbyPlayerService.getPlayerById(playerId);

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
     * Now supports partial lobby IDs
     *
     * @param playerId The ID of the player trying to start the game (must be lobby owner)
     * @param lobbyId The ID of the lobby to create a game session for (full ID or just first 5 characters)
     * @param winningScore Score required to win (default: 50)
     * @param mapLength Map length (default: 1080)
     * @param mapHeight Map height (default: 1080)
     * @return Status message indicating success or failure
     */
    public String startGameSession(String playerId, String lobbyId, int winningScore, int mapLength, int mapHeight) {
        String startGameSessionMessage = "";
        try {
            kotlin.Pair<gr17.noodleio.game.models.GameSession, String> result =
                lobbyPlayerService.startGameSession(playerId, lobbyId, winningScore, mapLength, mapHeight);

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
     * Now supports partial lobby IDs
     *
     * @param playerId The ID of the player trying to start the game (must be lobby owner)
     * @param lobbyId The ID of the lobby to create a game session for (full ID or just first 5 characters)
     * @return Status message indicating success or failure
     */
    public String startGameSession(String playerId, String lobbyId) {
        return startGameSession(playerId, lobbyId, 10, 1080, 1080);
    }

    /**
     * Checks if there's an active game session for a lobby
     * Now supports partial lobby IDs
     * 
     * @param lobbyId The ID of the lobby (full ID or just first 5 characters)
     * @return Status message indicating if an active game session exists
     */
    public String checkActiveGameSession(String lobbyId) {
        try {
            // Check if an active game session exists for this lobby
            return lobbyPlayerService.checkActiveGameSession(lobbyId);
        } catch (Exception e) {
            Gdx.app.error("LobbyPlayerApi", "Error checking for active game session", e);
            return null;
        }
    }

    public String getPlayerIdFromName(String playerName) {
        try {
            // Get player ID from player name
            return lobbyPlayerService.getPlayerIdFromName(playerName);
        } catch (Exception e) {
            Gdx.app.error("LobbyPlayerApi", "Error getting player ID from name", e);
            return null;
        }
    }

    /**
     * Checks if a player is the owner of a lobby
     * Now supports partial lobby IDs
     * 
     * @param playerId The ID of the player
     * @param lobbyId The ID of the lobby (full ID or just first 5 characters)
     * @return True if the player is the lobby owner, false otherwise
     */
    public boolean isLobbyOwner(String playerId, String lobbyId) {
        try {
            return lobbyPlayerService.isLobbyOwner(playerId, lobbyId);
        } catch (Exception e) {
            Gdx.app.error("LobbyPlayerApi", "Error checking if player is lobby owner", e);
            return false;
        }
    }
}