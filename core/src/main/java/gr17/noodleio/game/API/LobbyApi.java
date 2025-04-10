package gr17.noodleio.game.API;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.models.Lobby;
import gr17.noodleio.game.models.LobbyPlayer;
import gr17.noodleio.game.services.LobbyService;
import kotlin.Pair;

public class LobbyApi {
    private final LobbyService lobbyService;
    private String createLobbyMessage = "";
    private String getLobbyMessage = "";
    private String playerMessage = "";

    public LobbyApi(EnvironmentConfig environmentConfig) {
        this.lobbyService = new LobbyService(environmentConfig);
    }

    /**
     * Creates a new lobby with default settings
     * @return Status message including the lobby ID if successful
     */
    public String createLobby() {
        return createLobby(4); // Default to 4 max players
    }

    /**
     * Creates a new lobby with specified maximum player count
     * @param maxPlayers The maximum number of players allowed
     * @return Status message including the lobby ID if successful
     */
    public String createLobby(int maxPlayers) {
        try {
            Lobby newLobby = lobbyService.createLobby(maxPlayers);

            if (newLobby != null) {
                createLobbyMessage = "Lobby created successfully! Lobby ID: " + newLobby.getId();
                return createLobbyMessage;
            } else {
                createLobbyMessage = "Failed to create lobby";
                return createLobbyMessage;
            }
        } catch (Exception e) {
            createLobbyMessage = "Error creating lobby: " + e.getMessage();
            e.printStackTrace();
            return createLobbyMessage;
        }
    }

    /**
     * Creates a new lobby with a player as owner
     * @param playerName Name of the player who will own the lobby
     * @param maxPlayers Maximum number of players allowed in the lobby
     * @return Status message including lobby and player details if successful
     */
    public String createLobbyWithOwner(String playerName, int maxPlayers) {
        try {
            Pair<Lobby, LobbyPlayer> result = lobbyService.createLobbyWithOwner(playerName, maxPlayers);

            if (result != null) {
                Lobby lobby = result.getFirst();
                LobbyPlayer player = result.getSecond();

                createLobbyMessage = "Lobby created with ID: " + lobby.getId();
                playerMessage = "Player '" + player.getPlayer_name() + "' added as owner with ID: " + player.getId();

                return createLobbyMessage + " | " + playerMessage;
            } else {
                createLobbyMessage = "Failed to create lobby with owner. Player name might already exist.";
                return createLobbyMessage;
            }
        } catch (Exception e) {
            createLobbyMessage = "Error creating lobby with owner: " + e.getMessage();
            e.printStackTrace();
            return createLobbyMessage;
        }
    }

    /**
     * Creates a new lobby with a random player name for testing
     * @return Status message including lobby and player details if successful
     */
    public String createTestLobbyWithOwner() {
        try {
            // Generate a random player name with timestamp to ensure uniqueness
            String playerName = "TestPlayer_" + System.currentTimeMillis();

            return createLobbyWithOwner(playerName, 4);
        } catch (Exception e) {
            createLobbyMessage = "Error creating test lobby with owner: " + e.getMessage();
            e.printStackTrace();
            return createLobbyMessage;
        }
    }

    /**
     * Gets a lobby by its ID
     * @param lobbyId The ID of the lobby to retrieve
     * @return Status message including lobby details if found
     */
    public String getLobbyById(String lobbyId) {
        try {
            Lobby lobby = lobbyService.getLobbyById(lobbyId);

            if (lobby != null) {
                getLobbyMessage = "Lobby found: ID=" + lobby.getId() +
                    ", Max Players=" + lobby.getMax_players();

                // Only add owner info if it exists
                if (lobby.getLobby_owner() != null && !lobby.getLobby_owner().isEmpty()) {
                    getLobbyMessage += ", Owner=" + lobby.getLobby_owner();
                }

                return getLobbyMessage;
            } else {
                getLobbyMessage = "No lobby found with ID: " + lobbyId;
                return getLobbyMessage;
            }
        } catch (Exception e) {
            getLobbyMessage = "Error retrieving lobby: " + e.getMessage();
            e.printStackTrace();
            return getLobbyMessage;
        }
    }

    /**
     * Gets the most recent create lobby message
     * @return The create lobby status message
     */
    public String getCreateLobbyMessage() {
        return createLobbyMessage;
    }

    /**
     * Gets the most recent get lobby message
     * @return The get lobby status message
     */
    public String getGetLobbyMessage() {
        return getLobbyMessage;
    }

    /**
     * Gets the most recent player message
     * @return The player status message
     */
    public String getPlayerMessage() {
        return playerMessage;
    }

    /**
     * Gets the underlying LobbyService
     * @return The LobbyService instance
     */
    public LobbyService getLobbyService() {
        return lobbyService;
    }
}
