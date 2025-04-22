package gr17.noodleio.game.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;

import gr17.noodleio.game.API.LobbyPlayerApi;
import gr17.noodleio.game.config.Config;
import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.states.ui.BaseUIState;
import gr17.noodleio.game.util.ResourceManager;

public class LobbyState extends BaseUIState {

    private String lobbyId;
    private String playerId;
    private String playerName;
    private Label lobbyCodeLabel;
    private Label playerNameLabel;
    private Label playersLabel;
    private TextButton startGameButton;

    private LobbyPlayerApi lobbyPlayerApi;
    private boolean isLobbyOwner = false;
    private float gameSessionCheckTimer = 0;
    private float playerListRefreshTimer = 0;
    private static final float GAME_SESSION_CHECK_INTERVAL = 2.0f;
    private static final float PLAYER_LIST_REFRESH_INTERVAL = 3.0f;

    public LobbyState(GameStateManager gsm) {
        super(gsm);
        initializeApis();
    }

    @Override
    protected void setupUI() {
        uiFactory.addTitle(table, "LOBBY");

        uiFactory.addLabel(table, "Lobby Code:");
        lobbyCodeLabel = uiFactory.addLabel(table, "-----");

        playerNameLabel = uiFactory.addLabel(table, "You: -----");
        uiFactory.addLabel(table, "Players:");
        playersLabel = uiFactory.addLabel(table, "Loading players...");

        startGameButton = uiFactory.addButton(table, "Start Game", this::startGame);
        startGameButton.setVisible(false); // Initially hidden

        uiFactory.createBackButton(table, () -> gsm.set(new MenuState(gsm)));
        statusLabel = uiFactory.createStatusLabel(table);
    }

    private void initializeApis() {
        EnvironmentConfig config = new EnvironmentConfig() {
            public String getSupabaseUrl() { return Config.getSupabaseUrl(); }
            public String getSupabaseKey() { return Config.getSupabaseKey(); }
        };
        lobbyPlayerApi = new LobbyPlayerApi(config);
    }

    public void setLobbyData(String lobbyId, String playerId, String playerName) {
        this.lobbyId = lobbyId;
        this.playerId = playerId;
        this.playerName = playerName;

        // Delay UI update until after setupUI() has run
        Gdx.app.postRunnable(() -> updateUI());
    }


    private void updateUI() {
        if (lobbyCodeLabel != null && lobbyId != null) {
            lobbyCodeLabel.setText(lobbyId);
        }

        if (playerNameLabel != null && playerName != null) {
            playerNameLabel.setText("You: " + playerName);
        }

        refreshPlayersList();

        if (lobbyId != null && playerId != null && !playerId.equals("Not needed")) {
            try {
                boolean isOwner = lobbyPlayerApi.isLobbyOwner(playerId, lobbyId);
                if (isOwner != isLobbyOwner) {
                    isLobbyOwner = isOwner;
                    startGameButton.setVisible(isLobbyOwner);
                }
            } catch (Exception e) {
                logError("Error checking lobby owner", e);
            }
        }
    }

    private void refreshPlayersList() {
        try {
            String players = lobbyPlayerApi.getPlayersInLobby(lobbyId);
            playersLabel.setText(players);
        } catch (Exception e) {
            logError("Error refreshing players list", e);
            playersLabel.setText("Error loading players: " + e.getMessage());
        }
    }

    private void startGame() {
        if (lobbyId != null && playerId != null) {
            setStatus("Starting game...");
            try {
                String result = lobbyPlayerApi.startGameSession(playerId, lobbyId);
                if (result.contains("ID:")) {
                    String sessionId = result.split("ID:")[1].split(",")[0].trim();
                    ResourceManager rm = new ResourceManager();
                    rm.load();
                    PlayState playState = new PlayState(gsm, sessionId, playerId, playerName, rm);
                    gsm.set(playState);
                } else {
                    setStatus("Game started but couldn't get session ID");
                }
            } catch (Exception e) {
                logError("Error starting game", e);
                setStatus("Error starting game");
            }
        } else {
            setStatus("Error: Missing lobby or player data");
        }
    }

    private void checkForActiveGameSession() {
        try {
            String result = lobbyPlayerApi.checkActiveGameSession(lobbyId);
            if (result.contains("session_id:")) {
                String sessionId = result.split("session_id:")[1].split(",")[0].trim();
                if (playerId == null || playerId.equals("Not needed")) {
                    playerId = lobbyPlayerApi.getPlayerIdFromName(playerName);
                }
                ResourceManager rm = new ResourceManager();
                rm.load();
                PlayState playState = new PlayState(gsm, sessionId, playerId, playerName, rm);
                gsm.set(playState);
            }
        } catch (Exception e) {
            logError("Error checking for active game session", e);
        }
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        playerListRefreshTimer += dt;
        if (playerListRefreshTimer >= PLAYER_LIST_REFRESH_INTERVAL) {
            playerListRefreshTimer = 0;
            refreshPlayersList();
        }

        gameSessionCheckTimer += dt;
        if (gameSessionCheckTimer >= GAME_SESSION_CHECK_INTERVAL) {
            gameSessionCheckTimer = 0;
            checkForActiveGameSession();
        }
    }
}
