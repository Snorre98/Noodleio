package gr17.noodleio.game.API;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.models.LobbyPlayer;
import gr17.noodleio.game.views.LobbyPlayerService;

import java.util.List;

public class LobbyPlayerApi {
    private final LobbyPlayerService lobbyPlayerService;
    private String joinLobbyMessage = "";
    private String playersListMessage = "";
    private String leaveLobbyMessage = "";

    public LobbyPlayerApi(EnvironmentConfig environmentConfig) {
        this.lobbyPlayerService = new LobbyPlayerService(environmentConfig);
    }

    /**
     * Allows a player to join a lobby by its ID
     * @param playerName The name of the player who wants to join
     * @param lobbyId The ID of the lobby to join
     * @return Status message indicating success or failure
     */
    public String joinLobby(String playerName, String lobbyId) {
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
            List<LobbyPlayer> players = lobbyPlayerService.getPlayersInLobby(lobbyId);

            StringBuilder sb = new StringBuilder("Players in lobby " + lobbyId + ":\n");

            if (players.isEmpty()) {
                sb.append("No players found in this lobby");
            } else {
                for (LobbyPlayer player : players) {
                    sb.append("- ")
                        .append(player.getPlayer_name())
                        .append(" (ID: ")
                        .append(player.getId())
                        .append(", Joined: ")
                        .append(player.getJoined_at())
                        .append(")\n");
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
            boolean success = lobbyPlayerService.leaveLobby(playerId);

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
    public LobbyPlayerService getLobbyPlayerService() {
        return lobbyPlayerService;
    }
}
