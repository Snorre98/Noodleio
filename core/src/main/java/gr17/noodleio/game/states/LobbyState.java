package gr17.noodleio.game.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
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

        // Log connection info
        Gdx.app.log("LobbyState", "Initializing with Supabase URL: " + environmentConfig.getSupabaseUrl());

        // Initialize lobby player API
        lobbyPlayerApi = new LobbyPlayerApi(environmentConfig);
    }

    private void createSkin() {
        skin = new Skin();

        // Add default font
        BitmapFont font = new BitmapFont();
        skin.add("default-font", font);

        // Create label style
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        // Create button style
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        skin.add("default", buttonStyle);
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

        // Add buttons
        TextButton startGameButton = new TextButton("Start Game", skin);
        table.add(startGameButton).padRight(20);

        TextButton backButton = new TextButton("Back", skin);
        table.add(backButton).padLeft(20);
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
                        // TODO: Create and transition to GameState
                        statusLabel.setText("Game starting with session: " + sessionId);
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
            skin.dispose();
        }
    }
}
