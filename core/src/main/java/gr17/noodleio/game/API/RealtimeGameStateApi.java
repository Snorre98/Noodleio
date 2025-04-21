package gr17.noodleio.game.API;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.models.GameSession;
import gr17.noodleio.game.models.PlayerGameState;
import gr17.noodleio.game.services.RealtimeGameStateService;
import gr17.noodleio.game.services.RealtimeGameStateService.GameStateListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;

/**
 * API for receiving real-time game state updates from the server
 * This is a one-way service - client only receives updates
 */
public class RealtimeGameStateApi {
    private volatile boolean isShuttingDown = false;
    private final RealtimeGameStateService gameStateService;
    private String statusMessage = "Initializing...";

    // List to store registered listeners
    private final List<GameStateCallback> listeners = new ArrayList<>();

    /**
     * Callback interface for game state updates
     */
    public interface GameStateCallback {
        void onPlayerStateChanged(PlayerGameState playerState);
        void onGameSessionChanged(GameSession gameSession);
        void onGameOver();
    }

    public RealtimeGameStateApi(EnvironmentConfig environmentConfig) {
        this.gameStateService = new RealtimeGameStateService(environmentConfig);

        // Set up internal listener that forwards events to our Java callbacks
        this.gameStateService.addListener(new GameStateListener() {
            @Override
            public void onPlayerStateChanged(PlayerGameState playerState) {
                // skip if shutting down
                if (isShuttingDown) return;
                // Notify all registered callbacks
                // Use Gdx.app.postRunnable to ensure UI thread safety
                final PlayerGameState finalPlayerState = playerState;
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        for (GameStateCallback callback : listeners) {
                            callback.onPlayerStateChanged(finalPlayerState);
                        }
                    }
                });
            }

            @Override
            public void onGameSessionChanged(GameSession gameSession) {
                // Skip if shutting down
                if (isShuttingDown) return;
                // Notify all registered callbacks
                final GameSession finalGameSession = gameSession;
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        for (GameStateCallback callback : listeners) {
                            callback.onGameSessionChanged(finalGameSession);
                        }
                    }
                });
            }

            @Override
            public void onGameOver() {
                // Skip if shutting down
                if (isShuttingDown) return;
                // Notify all registered callbacks
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        for (GameStateCallback callback : listeners) {
                            callback.onGameOver();
                        }
                    }
                });
            }
        });
    }

    /**
     * Add a callback to receive game state updates
     * @param callback The callback to add
     */
    public void addCallback(GameStateCallback callback) {
        listeners.add(callback);
    }

    /**
     * Remove a previously registered callback
     * @param callback The callback to remove
     */
    public void removeCallback(GameStateCallback callback) {
        listeners.remove(callback);
    }

    /**
     * Connect to a game session to receive updates
     * @param sessionId ID of the game session to join
     * @param playerId ID of the local player
     * @return Status message
     */
    public String connect(String sessionId, String playerId) {
        try {
            statusMessage = gameStateService.connect(sessionId, playerId);
            return statusMessage;
        } catch (Exception e) {
            statusMessage = "Failed to connect: " + e.getMessage();
            e.printStackTrace();
            return statusMessage;
        }
    }

    /**
     * Disconnect from the game session
     * @return Status message
     */
    public String disconnect() {
        try {
            isShuttingDown = true;
            statusMessage = gameStateService.disconnect();
            return statusMessage;
        } catch (Exception e) {
            statusMessage = "Failed to disconnect: " + e.getMessage();
            e.printStackTrace();
            return statusMessage;
        }
    }

    /**
     * Get all current player states
     * @return Map of player IDs to player states
     */
    public Map<String, PlayerGameState> getPlayerStates() {
        // TODO, use
        return gameStateService.getPlayerStates();
    }
}
