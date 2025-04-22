package gr17.noodleio.game.states;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Array;

import gr17.noodleio.game.API.LeaderboardApi;
import gr17.noodleio.game.config.Config;
import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.model.PlayerResult;
import gr17.noodleio.game.models.GameSession;
import gr17.noodleio.game.states.ui.BaseUIState;
import gr17.noodleio.game.states.ui.UIComponents;
import gr17.noodleio.game.util.ResourceManager;

public class EndGameState extends BaseUIState {

    private Array<PlayerResult> results;
    private String playerName;
    private int placement;
    private ResourceManager rm;
    private LeaderboardApi leaderboardApi;

    public EndGameState(GameStateManager gsm, Array<PlayerResult> results, String playerName,
                        int placement, ResourceManager rm, GameSession gameSession) {
        super(gsm);

        log("Starting EndGameState constructor");

        this.results = results;
        this.playerName = playerName;
        this.placement = placement;
        this.rm = rm;

        // Initialize API
        initializeApi();

        // Save score to leaderboard
        saveScore(gameSession);
    }

    @Override
    protected void setupUI() {
        log("Setting up EndGameState UI");

        if (results == null) {
            logError("Results array is null in setupUI");
            return;
        }

        // Add title based on placement
        String titleText = (placement == 1) ? "YOU WON!" : "You reached place #" + placement;
        log("Adding title: " + titleText);
        uiFactory.addTitle(table, titleText);

        // Add results header
        uiFactory.addLabel(table, "RESULTS", 20);

        // Display each player's result
        setupResultsDisplay();

        // Add back button
        uiFactory.createBackButton(table, this::returnToMenu);

        log("EndGameState UI setup completed");
    }

    private void setupResultsDisplay() {
        if (results == null) {
            logError("Results array is null in setupResultsDisplay");
            return;
        }

        log("Setting up results display for " + results.size + " players");

        // Display each player's result
        for (int i = 0; i < results.size; i++) {
            PlayerResult result = results.get(i);

            if (result == null) {
                logError("Null result at index " + i);
                continue;
            }

            // Create label style for this entry
            Label.LabelStyle resultStyle = new Label.LabelStyle(
                UIComponents.getInstance().getSkin().getFont("default-font"),
                Color.WHITE
            );

            // Highlight the current player's row
            if (result.name != null && result.name.equals(playerName)) {
                log("Highlighting player's result: " + result.name);
                resultStyle.fontColor = Color.YELLOW;
            }

            // Create and add the result label
            String resultText = (i + 1) + ". " + result.name + " - " + result.score + " points";
            Label resultLabel = new Label(resultText, resultStyle);
            table.add(resultLabel).padBottom(10).left();
            table.row();
        }
    }

    private void initializeApi() {
        log("Initializing LeaderboardApi");

        try {
            EnvironmentConfig environmentConfig = new EnvironmentConfig() {
                @Override
                public String getSupabaseUrl() {
                    return Config.getSupabaseUrl();
                }

                @Override
                public String getSupabaseKey() {
                    return Config.getSupabaseKey();
                }
            };

            leaderboardApi = new LeaderboardApi(environmentConfig);
            log("LeaderboardApi initialized successfully");
        } catch (Exception e) {
            logError("Error initializing LeaderboardApi", e);
        }
    }

    private void saveScore(GameSession gameSession) {
        log("Attempting to save score to leaderboard");

        try {
            if (leaderboardApi == null) {
                logError("Cannot save score: LeaderboardApi is null");
                return;
            }

            // Get the player's score
            int playerScore = 0;
            if (results != null) {
                for (PlayerResult result : results) {
                    if (result != null && result.name != null && result.name.equals(playerName)) {
                        playerScore = result.score;
                        log("Found player score: " + playerScore);
                        break;
                    }
                }
            }

            // Save score to leaderboard if game has ended
            if (gameSession != null && gameSession.getEnded_at() != null) {
                log("Game session is valid, calculating duration");

                // Calculate game duration if possible
                Double durationSeconds = null;
                if (gameSession.getStarted_at() != null) {
                    long startMillis = gameSession.getStarted_at().toEpochMilliseconds();
                    long endMillis = gameSession.getEnded_at().toEpochMilliseconds();
                    durationSeconds = (endMillis - startMillis) / 1000.0;
                    log("Game duration: " + durationSeconds + " seconds");
                }

                // Try to call the specific method if it exists
                try {
                    leaderboardApi.addLeaderboardEntryFromSession(playerName, playerScore, gameSession);
                    log("Score saved to leaderboard using session data");
                } catch (NoSuchMethodError e) {
                    log("Fallback to standard method");
                    leaderboardApi.addLeaderboardEntry(playerName, playerScore, durationSeconds);
                    log("Score saved to leaderboard using standard method");
                }
            }
        } catch (Exception e) {
            logError("Error saving score to leaderboard", e);
        }
    }

    @Override
    public void dispose() {
        log("Disposing EndGameState");
        super.dispose();
    }
}
