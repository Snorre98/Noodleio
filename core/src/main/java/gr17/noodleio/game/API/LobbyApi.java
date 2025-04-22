package gr17.noodleio.game.API;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.models.Lobby;
import gr17.noodleio.game.models.LobbyPlayer;
import gr17.noodleio.game.services.LobbyService;
import kotlin.Pair;

public class LobbyApi {
    private final LobbyService lobbyService;

    public LobbyApi(EnvironmentConfig environmentConfig) {
        this.lobbyService = new LobbyService(environmentConfig);
    }

    /**
     * Creates a new lobby with a player as owner
     * @param playerName Name of the player who will own the lobby
     * @return Status message including lobby and player details if successful
     */
    public String createLobbyWithOwner(String playerName) {
        String createLobbyMessage;
        try {
            Pair<Lobby, LobbyPlayer> result = lobbyService.createLobbyWithOwner(playerName, 2);

            if (result != null) {
                Lobby lobby = result.getFirst();
                LobbyPlayer player = result.getSecond();

                // Get the short code (first 5 characters) for easier sharing
                String lobbyId = lobby.getId();
                String shortCode = lobbyId.substring(0, Math.min(5, lobbyId.length()));

                createLobbyMessage = "Lobby created with ID: " + lobbyId;
                String playerMessage = "Player '" + player.getPlayer_name() + "' added as owner with ID: " + player.getId();

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
}