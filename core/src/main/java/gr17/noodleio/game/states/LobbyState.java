package gr17.noodleio.game.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;

import gr17.noodleio.game.API.LobbyPlayerApi;
import gr17.noodleio.game.config.Config;
import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.util.ResourceManager;

public class LobbyState extends State {
    private Stage stage;
    private Skin skin;
    private Table table;

    // Lobby data
    private String lobbyId;
    private String playerId;
    private String playerName;

    // UI elements
    private Label lobbyCodeLabel;
    private Label playersLabel;
    private Label statusLabel;

    // API for lobby player operations
    private LobbyPlayerApi lobbyPlayerApi;

    private float gameSessionCheckTimer = 0;
    private static final float GAME_SESSION_CHECK_INTERVAL = 2.0f; // Check every 2 seconds

    private float playerListRefreshTimer = 0;
    private static final float PLAYER_LIST_REFRESH_INTERVAL = 3.0f; // Refresh every 3 seconds

    public LobbyState(GameStateManager gsm) {
        super(gsm);

        // Initialize environment config and APIs
        initializeApis();

        // Create stage with viewport that matches our camera
        stage = new Stage(new FitViewport(800, 480, cam));
        Gdx.input.setInputProcessor(stage);

        // Create skin and UI
        createSkin();
        createUI();
    }

    public void setLobbyData(String lobbyId, String playerId, String playerName) {
        this.lobbyId = lobbyId;
        this.playerId = playerId;
        this.playerName = playerName;

        Gdx.app.log("LobbyState", "Setting lobby data - Lobby ID: " + lobbyId +
            ", Player ID: " + playerId + ", Player Name: " + playerName);

        // Update UI with the new lobby data
        updateUI();
    }

    private void initializeApis() {
        // Create environment config directly using Config class
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

        // Initialize lobby player API
        lobbyPlayerApi = new LobbyPlayerApi(environmentConfig);
    }

    private void createSkin() {
        skin = new Skin();

        try {
            // Add default font
            if (Gdx.files.internal("default.fnt").exists()) {
                skin.add("default-font", new BitmapFont(Gdx.files.internal("default.fnt")));
            } else {
                skin.add("default-font", new BitmapFont());
            }

            // Create a white pixel texture for buttons and fields
            Pixmap whitePix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            whitePix.setColor(Color.WHITE);
            whitePix.fill();
            skin.add("white", new Texture(whitePix));
            whitePix.dispose();

            // Load button textures if they exist
            if (Gdx.files.internal("default-round.png").exists()) {
                skin.add("default-round", new Texture(Gdx.files.internal("default-round.png")));
            } else {
                // Use white pixel texture as fallback
                skin.add("default-round", skin.get("white", Texture.class));
            }

            if (Gdx.files.internal("default-round-down.png").exists()) {
                skin.add("default-round-down", new Texture(Gdx.files.internal("default-round-down.png")));
            } else {
                // Create a gray texture as fallback for the "down" state
                Pixmap grayPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
                grayPix.setColor(new Color(0.5f, 0.5f, 0.5f, 1f));
                grayPix.fill();
                Texture grayTex = new Texture(grayPix);
                skin.add("default-round-down", grayTex);
                grayPix.dispose();
            }

            // Create button styles matching MenuState
            TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
            textButtonStyle.font = skin.getFont("default-font");
            textButtonStyle.up = skin.newDrawable("default-round");
            textButtonStyle.down = skin.newDrawable("default-round-down");
            textButtonStyle.checked = skin.newDrawable("default-round-down");
            skin.add("default", textButtonStyle);

            // Create label style
            Label.LabelStyle labelStyle = new Label.LabelStyle();
            labelStyle.font = skin.getFont("default-font");
            labelStyle.fontColor = Color.WHITE;
            skin.add("default", labelStyle);

        } catch (Exception e) {
            Gdx.app.error("LobbyState", "Error loading skin resources", e);

            // Create a minimal fallback skin if the above fails
            if (skin.getFont("default-font") == null) {
                skin.add("default-font", new BitmapFont());
            }

            if (skin.has("default", TextButton.TextButtonStyle.class) == false) {
                TextButton.TextButtonStyle fallbackStyle = new TextButton.TextButtonStyle();
                fallbackStyle.font = skin.getFont("default-font");
                skin.add("default", fallbackStyle);
            }

            if (skin.has("default", Label.LabelStyle.class) == false) {
                Label.LabelStyle labelStyle = new Label.LabelStyle();
                labelStyle.font = skin.getFont("default-font");
                labelStyle.fontColor = Color.WHITE;
                skin.add("default", labelStyle);
            }
        }
    }

    private void createUI() {
        // Create table for UI layout
        table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Add lobby title
        Label titleLabel = new Label("LOBBY", skin);
        titleLabel.setFontScale(2.0f);
        table.add(titleLabel).colspan(2).padBottom(40);
        table.row();

        // Add lobby code display
        table.add(new Label("Lobby Code:", skin)).padRight(20);
        lobbyCodeLabel = new Label("-----", skin);
        table.row();
        table.add(lobbyCodeLabel).padBottom(20);
        table.row();

        // Add player name display
        table.add(new Label("You:", skin)).padRight(20);
        Label playerNameLabel = new Label("-----", skin);
        table.add(playerNameLabel).padBottom(30);
        table.row();

        // Add players list title
        table.add(new Label("Players:", skin)).colspan(2).padBottom(10);
        table.row();

        // Add players list
        playersLabel = new Label("Loading players...", skin);
        table.add(playersLabel).colspan(2).padBottom(30);
        table.row();

        // Add buttons with consistent sizing and styling to match MenuState
        TextButton startGameButton = new TextButton("Start Game", skin);
        table.add(startGameButton).width(200).height(50).padRight(20);

        TextButton backButton = new TextButton("Back", skin);
        table.add(backButton).width(200).height(50).padLeft(20);
        table.row();

        // Add status label
        statusLabel = new Label("", skin);
        table.add(statusLabel).colspan(2).padTop(30);

        // Configure button listeners
        startGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                startGame();
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                gsm.set(new MenuState(gsm));
            }
        });
    }

    private void updateUI() {
        if (lobbyId != null) {
            lobbyCodeLabel.setText(lobbyId);
        }

        if (playerName != null) {
            // Update the player name label (index 3 in the table)
            Table table = (Table) stage.getActors().first();
            if (table.getChildren().size > 3) {
                Actor playerNameActor = table.getChildren().get(3);
                if (playerNameActor instanceof Label) {
                    ((Label) playerNameActor).setText(playerName);
                }
            }
        }

        // Load players in the lobby
        refreshPlayersList();
    }

    private void refreshPlayersList() {
        if (lobbyId != null) {
            try {
                // Get players from the API
                Gdx.app.log("LobbyState", "Refreshing players list for lobby: " + lobbyId);
                String players = lobbyPlayerApi.getPlayersInLobby(lobbyId);
                playersLabel.setText(players);
            } catch (Exception e) {
                Gdx.app.error("LobbyState", "Error refreshing players list", e);
                playersLabel.setText("Error loading players: " + e.getMessage());
            }
        }
    }

    private void startGame() {
        if (lobbyId != null && playerId != null) {
            statusLabel.setText("Starting game...");
            Gdx.app.log("LobbyState", "Starting game with playerId: " + playerId + " and lobbyId: " + lobbyId);

            try {
                // Call the API to start a game session
                String result = lobbyPlayerApi.startGameSession(playerId, lobbyId);
                Gdx.app.log("LobbyState", "Game session start result: " + result);

                if (result.contains("Game session started successfully")) {
                    // Extract session ID
                    String sessionId = null;
                    if (result.contains("ID:")) {
                        int idIndex = result.indexOf("ID:") + 3;
                        int commaIndex = result.indexOf(",", idIndex);
                        if (commaIndex > idIndex) {
                            sessionId = result.substring(idIndex, commaIndex).trim();
                            Gdx.app.log("LobbyState", "Extracted session ID: " + sessionId);
                        }
                    }

                    // Transition to the game state
                    if (sessionId != null) {
                        // Create resource manager if needed
                        ResourceManager rm = new ResourceManager();
                        rm.load();

                        // Create and transition to PlayState
                        PlayState playState = new PlayState(gsm, sessionId, playerId, playerName, rm);
                        gsm.set(playState);
                    } else {
                        statusLabel.setText("Game started but couldn't get session ID");
                    }
                } else {
                    statusLabel.setText("Failed to start game: " + result);
                }
            } catch (Exception e) {
                Gdx.app.error("LobbyState", "Error starting game", e);
                statusLabel.setText("Error starting game: " + e.getMessage());
            }
        } else {
            statusLabel.setText("Error: Missing lobby or player data");
        }
    }

    @Override
    protected void handleInput() {
        // Input is handled by Stage
    }

    @Override
    public void update(float dt) {
        stage.act(dt);

        // Player list refresh logic
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

    private void checkForActiveGameSession() {
        if (lobbyId != null) {
            try {
                // Check if there's an active game session for this lobby
                String result = lobbyPlayerApi.checkActiveGameSession(lobbyId);

                if (result != null && result.contains("session_id:")) {
                    // Extract session ID
                    String sessionId = extractSessionId(result);
                    if (sessionId != null && !sessionId.isEmpty()) {
                        Gdx.app.log("LobbyState", "Active game session found: " + sessionId);

                        // Create resource manager
                        ResourceManager rm = new ResourceManager();
                        rm.load();

                        // Get player ID for this client if not already known
                        if (playerId == null || playerId.equals("Not needed")) {
                            // Get player ID from the player name
                            playerId = lobbyPlayerApi.getPlayerIdFromName(playerName);
                        }

                        // Transition to PlayState
                        PlayState playState = new PlayState(gsm, sessionId, playerId, playerName, rm);
                        gsm.set(playState);
                    }
                }
            } catch (Exception e) {
                Gdx.app.error("LobbyState", "Error checking for active game session", e);
            }
        }
    }

    private String extractSessionId(String result) {
        try {
            // Extract session ID from result string
            int startIndex = result.indexOf("session_id:") + "session_id:".length();
            int endIndex = result.indexOf(",", startIndex);
            if (endIndex == -1) endIndex = result.length();
            return result.substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            Gdx.app.error("LobbyState", "Error extracting session ID", e);
            return null;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        // Clear screen
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw stage
        stage.draw();
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
        }

        if (skin != null) {
            // Dispose textures that were created or loaded in createSkin
            if (skin.has("white", Texture.class)) {
                Texture tex = skin.get("white", Texture.class);
                if (tex != null) tex.dispose();
            }

            if (skin.has("default-round", Texture.class)) {
                Texture tex = skin.get("default-round", Texture.class);
                if (tex != null && tex != skin.get("white", Texture.class)) {
                    tex.dispose();
                }
            }

            if (skin.has("default-round-down", Texture.class)) {
                Texture tex = skin.get("default-round-down", Texture.class);
                if (tex != null) tex.dispose();
            }

            skin.dispose();
        }
    }
}
