package gr17.noodleio.game.API;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.views.RealtimePlayerGameStateViews;
import kotlin.Pair;

public class PlayerGameStateApi {
    private final RealtimePlayerGameStateViews playerGameStateViews;
    private String movePlayerMessage = "";

    public PlayerGameStateApi(EnvironmentConfig environmentConfig) {
        this.playerGameStateViews = new RealtimePlayerGameStateViews(environmentConfig);
    }

    /**
     * Moves a player up by one position unit
     * @param playerId The ID of the player to move
     * @param sessionId The ID of the game session
     * @return Status message indicating success or failure
     */
    public String movePlayerUp(String playerId, String sessionId) {
        try {
            Pair<Boolean, String> result = playerGameStateViews.movePlayerUp(playerId, sessionId);

            boolean success = result.getFirst();
            String message = result.getSecond();

            if (success) {
                movePlayerMessage = "Player movement successful: " + message;
            } else {
                movePlayerMessage = "Player movement failed: " + message;
            }

            return movePlayerMessage;
        } catch (Exception e) {
            movePlayerMessage = "Error moving player: " + e.getMessage();
            e.printStackTrace();
            return movePlayerMessage;
        }
    }

    /**
     * Moves a player down by one position unit
     * @param playerId The ID of the player to move
     * @param sessionId The ID of the game session
     * @return Status message indicating success or failure
     */
    public String movePlayerDown(String playerId, String sessionId) {
        try {
            Pair<Boolean, String> result = playerGameStateViews.movePlayerDown(playerId, sessionId);

            boolean success = result.getFirst();
            String message = result.getSecond();

            if (success) {
                movePlayerMessage = "Player movement successful: " + message;
            } else {
                movePlayerMessage = "Player movement failed: " + message;
            }

            return movePlayerMessage;
        } catch (Exception e) {
            movePlayerMessage = "Error moving player: " + e.getMessage();
            e.printStackTrace();
            return movePlayerMessage;
        }
    }

    /**
     * Moves a player left by one position unit
     * @param playerId The ID of the player to move
     * @param sessionId The ID of the game session
     * @return Status message indicating success or failure
     */
    public String movePlayerLeft(String playerId, String sessionId) {
        try {
            Pair<Boolean, String> result = playerGameStateViews.movePlayerLeft(playerId, sessionId);

            boolean success = result.getFirst();
            String message = result.getSecond();

            if (success) {
                movePlayerMessage = "Player movement successful: " + message;
            } else {
                movePlayerMessage = "Player movement failed: " + message;
            }

            return movePlayerMessage;
        } catch (Exception e) {
            movePlayerMessage = "Error moving player: " + e.getMessage();
            e.printStackTrace();
            return movePlayerMessage;
        }
    }

    /**
     * Moves a player right by one position unit
     * @param playerId The ID of the player to move
     * @param sessionId The ID of the game session
     * @return Status message indicating success or failure
     */
    public String movePlayerRight(String playerId, String sessionId) {
        try {
            Pair<Boolean, String> result = playerGameStateViews.movePlayerRight(playerId, sessionId);

            boolean success = result.getFirst();
            String message = result.getSecond();

            if (success) {
                movePlayerMessage = "Player movement successful: " + message;
            } else {
                movePlayerMessage = "Player movement failed: " + message;
            }

            return movePlayerMessage;
        } catch (Exception e) {
            movePlayerMessage = "Error moving player: " + e.getMessage();
            e.printStackTrace();
            return movePlayerMessage;
        }
    }

    /**
     * Gets the most recent move player message
     * @return The move player status message
     */
    public String getMovePlayerMessage() {
        return movePlayerMessage;
    }

    /**
     * Gets the underlying RealtimePlayerGameStateViews
     * @return The RealtimePlayerGameStateViews instance
     */
    public RealtimePlayerGameStateViews getPlayerGameStateViews() {
        return playerGameStateViews;
    }
}
