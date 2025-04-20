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
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;

import gr17.noodleio.game.API.LobbyApi;
import gr17.noodleio.game.API.LobbyPlayerApi;
import gr17.noodleio.game.config.Config;
import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.models.LobbyPlayer;

public class MenuState extends State {
    private Stage stage;
    private Skin skin;
    private Table table;
    private TextField playerNameField;
    private TextField lobbyCodeField;
    private Label statusLabel;

    // API for lobby operations
    private LobbyApi lobbyApi;

    private LobbyPlayerApi lobbyPlayerApi;
    private String lobbyId;
    private String playerId;

    public MenuState(GameStateManager gsm) {
        super(gsm);

        // Initialize environment config and APIs
        initializeApis();

        // Create stage with viewport that matches our camera
        stage = new Stage(new FitViewport(800, 480, cam));
        Gdx.input.setInputProcessor(stage);

        // Create skin with required resources
        setupSkin();

        // Create table for UI layout
        table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        setupTitle();
        setupButtons(gsm);
    }

    private void initializeApis() {
        // Create environment config directly using Config
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

//        // Log the config values
//        Gdx.app.log("MenuState", "Initializing APIs with Supabase URL: " + environmentConfig.getSupabaseUrl());
//        Gdx.app.log("MenuState", "API Key starts with: " +
//            (environmentConfig.getSupabaseKey().length() > 5 ?
//                environmentConfig.getSupabaseKey().substring(0, 10) + "..." : "invalid"));

        // Initialize lobby API
        lobbyApi = new LobbyApi(environmentConfig);
        lobbyPlayerApi = new LobbyPlayerApi(environmentConfig);
    }

    private void setupSkin() {
        try {
            skin = new Skin();

            // Load font and textures safely - check if they exist first
            if (Gdx.files.internal("default.fnt").exists()) {
                skin.add("default-font", new BitmapFont(Gdx.files.internal("default.fnt")));
            } else {
                // If not found, create a default font
                skin.add("default-font", new BitmapFont());
            }

            // Create a white pixel texture for TextField
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
                Texture whiteTex = new Texture(1, 1, Pixmap.Format.RGB888);
                skin.add("default-round", whiteTex);
            }

            if (Gdx.files.internal("default-round-down.png").exists()) {
                skin.add("default-round-down", new Texture(Gdx.files.internal("default-round-down.png")));
            } else {
                // Use gray pixel texture as fallback
                Texture grayTex = new Texture(1, 1, Pixmap.Format.RGB888);
                skin.add("default-round-down", grayTex);
            }

            // Create button styles
            TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
            textButtonStyle.font = skin.getFont("default-font");
            textButtonStyle.up = skin.newDrawable("default-round");
            textButtonStyle.down = skin.newDrawable("default-round-down");
            textButtonStyle.checked = skin.newDrawable("default-round-down");
            skin.add("default", textButtonStyle);

            // Create Label style
            Label.LabelStyle labelStyle = new Label.LabelStyle();
            labelStyle.font = skin.getFont("default-font");
            labelStyle.fontColor = Color.WHITE;
            skin.add("default", labelStyle);

        } catch (Exception e) {
            Gdx.app.error("MenuState", "Error loading skin resources", e);

            // Create a minimal fallback skin
            skin = new Skin();
            skin.add("default-font", new BitmapFont());

            TextButton.TextButtonStyle fallbackStyle = new TextButton.TextButtonStyle();
            fallbackStyle.font = skin.getFont("default-font");
            skin.add("default", fallbackStyle);

            Label.LabelStyle labelStyle = new Label.LabelStyle();
            labelStyle.font = skin.getFont("default-font");
            skin.add("default", labelStyle);
        }
    }

    private TextFieldStyle createTextFieldStyle() {
        TextFieldStyle textFieldStyle = new TextFieldStyle();
        textFieldStyle.font = skin.getFont("default-font");
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.cursor = skin.newDrawable("white", Color.WHITE);
        textFieldStyle.selection = skin.newDrawable("white", new Color(0.5f, 0.5f, 0.5f, 0.5f));
        textFieldStyle.background = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.8f));
        return textFieldStyle;
    }

    private void setupTitle() {
        try {
            // Add title to the top of the menu
            BitmapFont titleFont;
            if (Gdx.files.internal("default.fnt").exists()) {
                titleFont = new BitmapFont(Gdx.files.internal("default.fnt"));
            } else {
                titleFont = new BitmapFont();
            }
            titleFont.getData().setScale(2.0f);  // Make the title larger

            // Create a label style for the title
            Label.LabelStyle titleStyle = new Label.LabelStyle();
            titleStyle.font = titleFont;
            Label titleLabel = new Label("NOODLEIO", titleStyle);

            // Add the title to the table
            table.add(titleLabel).padBottom(50).colspan(1);
            table.row();
        } catch (Exception e) {
            Gdx.app.error("MenuState", "Error creating title", e);
            // Add a simple title as fallback
            Label titleLabel = new Label("NOODLEIO", new Label.LabelStyle(skin.getFont("default-font"), null));
            table.add(titleLabel).padBottom(50).colspan(1);
            table.row();
        }
    }

    private void setupButtons(GameStateManager gsm) {
        // Create buttons
        TextButton createGameButton = new TextButton("Create lobby", skin);
        TextButton joinGameButton = new TextButton("Join lobby", skin);
        TextButton leaderboardButton = new TextButton("Leaderboard", skin);

        // Create and add player name input field
        playerNameField = new TextField("", createTextFieldStyle());
        playerNameField.setMessageText("Enter player alias...");
        table.add(playerNameField).width(200).height(40).padBottom(20);
        table.row();

        // Add buttons to table with spacing
        table.add(createGameButton).padBottom(20).width(200).height(50);
        table.row();

        // Create and add lobby code input field
        lobbyCodeField = new TextField("", createTextFieldStyle());
        lobbyCodeField.setMessageText("Enter lobby code...");
        table.add(lobbyCodeField).width(200).height(40).padBottom(20);
        table.row();

        table.add(joinGameButton).padBottom(20).width(200).height(50);
        table.row();

        table.add(leaderboardButton).width(200).height(50);
        table.row();

        // Add status label for feedback
        statusLabel = new Label("", skin);
        table.add(statusLabel).padTop(30).width(300);

        // Add button listeners
        createGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String playerName = playerNameField.getText();
                if (playerName != null && !playerName.trim().isEmpty()) {
                    // Create a new lobby with the player as owner
                    createLobbyWithOwner(playerName, gsm);
                } else {
                    // Show error or prompt user to enter a name
                    statusLabel.setText("Please enter a player name");
                }
            }
        });

        joinGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String lobbyCode = lobbyCodeField.getText();
                String playerName = playerNameField.getText();

                if (lobbyCode == null || lobbyCode.trim().isEmpty()) {
                    statusLabel.setText("Please enter a lobby code");
                    return;
                }

                if (playerName == null || playerName.trim().isEmpty()) {
                    statusLabel.setText("Please enter a player name");
                    return;
                }

                statusLabel.setText("Joining lobby: " + lobbyCode);
                joinLobby(playerName, lobbyCode, gsm);
            }
        });

        leaderboardButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // TODO: Implement leaderboard state
                statusLabel.setText("Leaderboard not implemented yet");
                // gsm.set(new LeaderboardState(gsm));
                // dispose(); // Uncomment when implemented
            }
        });
    }

    private void createLobbyWithOwner(String playerName, GameStateManager gsm) {
        try {
            statusLabel.setText("Creating lobby...");
            Gdx.app.log("MenuState", "Attempting to create lobby with player: " + playerName);

            // Create lobby with owner
            String result = lobbyApi.createLobbyWithOwner(playerName, 4);
            Gdx.app.log("MenuState", "Lobby creation result: " + result);

            // Parse response to extract IDs
            if (result.contains("Lobby created with ID:")) {
                String[] parts = result.split("\\|");
                if (parts.length > 0) {
                    String lobbyPart = parts[0].trim();
                    lobbyId = lobbyPart.substring(lobbyPart.lastIndexOf(":") + 1).trim();
                    Gdx.app.log("MenuState", "Extracted lobby ID: " + lobbyId);
                }

                if (parts.length > 1) {
                    String playerPart = parts[1].trim();
                    String idSection = playerPart.substring(playerPart.lastIndexOf(":") + 1).trim();
                    playerId = idSection;
                    Gdx.app.log("MenuState", "Extracted player ID: " + playerId);
                }

                // Transition to lobby state with the created lobby data
                LobbyState lobbyState = new LobbyState(gsm);
                lobbyState.setLobbyData(lobbyId, playerId, playerName);
                gsm.set(lobbyState);

                // No need to dispose here as gsm.set() will handle it
            } else {
                // Show error message
                statusLabel.setText("Failed to create lobby: " + result);
                Gdx.app.error("MenuState", "Failed to create lobby: " + result);
            }
        } catch (Exception e) {
            Gdx.app.error("MenuState", "Error creating lobby with owner", e);
            if (e.getCause() != null) {
                Gdx.app.error("MenuState", "Caused by: " + e.getCause().getMessage());
            }
            statusLabel.setText("Error: " + e.getMessage());
        }
    }

    private void joinLobby(String playerName, String lobbyId, GameStateManager gsm){
        try {
            statusLabel.setText("Joining lobby... ");
            Gdx.app.log("MenuState", "Attempting to join lobby with player: " + playerName);
            String result = lobbyPlayerApi.joinLobby(playerName, lobbyId);
            Gdx.app.log("MenuState", "Lobby player API result: " + result);
            LobbyState lobbyState = new LobbyState(gsm);
            lobbyState.setLobbyData(lobbyId, "Not needed", playerName);
            gsm.set(lobbyState);
        } catch (Exception e){
            Gdx.app.error("MenuState", "Error joining lobby", e);
        }
    }

    @Override
    protected void handleInput() {
        // Input is handled by Scene2D stage
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

        // We don't use the SpriteBatch from the parameter because Stage has its own batch
        stage.draw();
    }

    @Override
    public void dispose() {
        try {
            // Dispose all resources to prevent memory leaks
            if (stage != null) {
                stage.dispose();
            }

            if (skin != null) {
                // Make sure to dispose all textures added to the skin
                if (skin.has("default-round", Texture.class)) {
                    Texture tex = skin.get("default-round", Texture.class);
                    if (tex != null) tex.dispose();
                }

                if (skin.has("default-round-down", Texture.class)) {
                    Texture tex = skin.get("default-round-down", Texture.class);
                    if (tex != null) tex.dispose();
                }

                if (skin.has("white", Texture.class)) {
                    Texture tex = skin.get("white", Texture.class);
                    if (tex != null) tex.dispose();
                }

                // Also dispose of any fonts we created directly (not through skin)
                if (stage != null) {
                    for (Actor actor : stage.getActors()) {
                        if (actor instanceof Table) {
                            Table table = (Table) actor;
                            for (Actor child : table.getChildren()) {
                                if (child instanceof Label) {
                                    Label label = (Label) child;
                                    if (label.getStyle() != null &&
                                        label.getStyle().font != null &&
                                        skin.has("default-font", BitmapFont.class) &&
                                        !label.getStyle().font.equals(skin.getFont("default-font"))) {
                                        label.getStyle().font.dispose();
                                    }
                                } else if (child instanceof TextButton) {
                                    TextButton button = (TextButton) child;
                                    if (button.getLabel() != null &&
                                        button.getLabel().getStyle() != null &&
                                        button.getLabel().getStyle().font != null &&
                                        skin.has("default-font", BitmapFont.class) &&
                                        !button.getStyle().font.equals(skin.getFont("default-font"))) {
                                        button.getLabel().getStyle().font.dispose();
                                    }
                                }
                            }
                        }
                    }
                }

                // Finally dispose the skin itself
                skin.dispose();
            }
        } catch (Exception e) {
            // Log any errors during disposal but don't crash
            Gdx.app.error("MenuState", "Error during dispose", e);
        }
    }
}
