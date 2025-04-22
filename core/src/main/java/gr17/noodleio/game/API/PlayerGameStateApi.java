package gr17.noodleio.game.API;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.services.PlayerGameStateService;

public class PlayerGameStateApi {
    private final PlayerGameStateService playerGameStateService;

    public PlayerGameStateApi(EnvironmentConfig environmentConfig) {
        this.playerGameStateService = new PlayerGameStateService(environmentConfig);
    }

    /**
     * Moves a player up by one position unit
     *
     * @param playerId  The ID of the player to move
     * @param sessionId The ID of the game session
     */
    public void movePlayerUp(String playerId, String sessionId) {
        try {
            // Call playerGameStateService to update movement
            playerGameStateService.movePlayerUp(playerId, sessionId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Moves a player down by one position unit
     *
     * @param playerId  The ID of the player to move
     * @param sessionId The ID of the game session
     */
    public void movePlayerDown(String playerId, String sessionId) {
        try {
            // Call playerGameStateService to update movement
            playerGameStateService.movePlayerDown(playerId, sessionId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Moves a player left by one position unit
     *
     * @param playerId  The ID of the player to move
     * @param sessionId The ID of the game session
     */
    public void movePlayerLeft(String playerId, String sessionId) {
        try {
            // Call playerGameStateService to update movement
            playerGameStateService.movePlayerLeft(playerId, sessionId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Moves a player right by one position unit
     *
     * @param playerId  The ID of the player to move
     * @param sessionId The ID of the game session
     */
    public void movePlayerRight(String playerId, String sessionId) {
        try {
            // Call playerGameStateService to update movement
            playerGameStateService.movePlayerRight(playerId, sessionId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updatePlayerScore(String playerId, String sessionId, int newScore) {
        try {
            // Call playerGameStateService to update the player's score
            playerGameStateService.updatePlayerScore(playerId, sessionId, newScore);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
