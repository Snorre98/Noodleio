package gr17.noodleio.game.states;

import com.badlogic.gdx.scenes.scene2d.ui.Label;

import gr17.noodleio.game.API.LeaderboardApi;
import gr17.noodleio.game.config.Config;
import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.states.ui.BaseUIState;

public class LeaderboardState extends BaseUIState {

    private static final int TOP_ENTRIES = 5;

    private Label[] leaderboardLabels;
    private LeaderboardApi leaderboardApi;

    public LeaderboardState(GameStateManager gsm) {
        super(gsm);
        initializeApi();
    }

    @Override
    protected void setupUI() {
        uiFactory.addTitle(table, "LEADERBOARD");
        uiFactory.addLabel(table, "PLAYER : SCORE (TIME)", 10);

        leaderboardLabels = new Label[TOP_ENTRIES];
        for (int i = 0; i < TOP_ENTRIES; i++) {
            leaderboardLabels[i] = uiFactory.addLabel(table, "---", 10);
        }

        statusLabel = uiFactory.createStatusLabel(table);
        uiFactory.createBackButton(table, this::returnToMenu);

        loadLeaderboard();
    }

    private void initializeApi() {
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
    }

    private void loadLeaderboard() {
        try {
            String result = leaderboardApi.fetchLeaderboard(TOP_ENTRIES);
            updateLeaderboardDisplay(result);
        } catch (Exception e) {
            logError("Error loading leaderboard", e);
            setStatus("Error loading leaderboard");
        }
    }

    private void updateLeaderboardDisplay(String leaderboardText) {
        String[] lines = leaderboardText.split("\\r?\\n");
        int labelIndex = 0;
        int startLine = 2; // Skip headers

        for (int i = startLine; i < lines.length && labelIndex < TOP_ENTRIES; i++) {
            if (!lines[i].contains(":")) continue;
            leaderboardLabels[labelIndex++].setText(lines[i].trim());
        }

        for (int i = labelIndex; i < TOP_ENTRIES; i++) {
            leaderboardLabels[i].setText("---");
        }

        setStatus("Leaderboard loaded successfully");
    }
}
