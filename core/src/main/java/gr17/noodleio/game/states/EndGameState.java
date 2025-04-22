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
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;

import gr17.noodleio.game.API.LeaderboardApi;
import gr17.noodleio.game.config.Config;
import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.model.PlayerResult;
import gr17.noodleio.game.models.GameSession;
import gr17.noodleio.game.util.ResourceManager;

public class EndGameState extends State {
    private Stage stage;
    private Skin skin;
    private Table table;
    private Array<PlayerResult> results;
    private String playerName;
    private int placement;
    private ResourceManager rm;
    private LeaderboardApi leaderboardApi;

    public EndGameState(GameStateManager gsm, Array<PlayerResult> results, String playerName, int placement, ResourceManager rm, GameSession gameSession) {
        super(gsm);
        log("Initializing EndGameState");

        this.results = results;
        this.playerName = playerName;
        this.placement = placement;
        this.rm = rm;

        // Initialize API first
        initializeApi();

        // Get the player's score
        int playerScore = 0;
        for (PlayerResult result : results) {
            if (result.name.equals(playerName)) {
                playerScore = result.score;
                break;
            }
        }

        // Save score to leaderboard if game has ended
        if (gameSession != null && gameSession.getEnded_at() != null) {
            saveScoreToLeaderboard(playerName, playerScore, gameSession);
        }

        // Create stage with viewport that matches our camera
        stage = new Stage(new FitViewport(800, 480, cam));
        Gdx.input.setInputProcessor(stage);

        // Create skin with required resources
        setupSkin();

        // Create table for UI layout
        table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Setup UI components
        setupTitle();
        setupResultsDisplay();
        setupButtons();
    }

    private void initializeApi() {
        try {
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

            // Initialize the leaderboard API
            leaderboardApi = new LeaderboardApi(environmentConfig);
            log("LeaderboardApi initialized successfully");
        } catch (Exception e) {
            logError("Error in init API", e);
        }
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

            // Create a white pixel texture for UI elements
            Pixmap whitePix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            whitePix.setColor(Color.WHITE);
            whitePix.fill();
            skin.add("white", new Texture(whitePix));
            whitePix.dispose();

            // Load button textures if they exist
            if (Gdx.files.internal("default-round.png").exists()) {
                skin.add("default-round", new Texture(Gdx.files.internal("default-round.png")));
            } else {
                // Create a black texture for buttons instead of using white
                Pixmap blackPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
                blackPix.setColor(Color.BLACK);
                blackPix.fill();
                Texture blackTex = new Texture(blackPix);
                skin.add("default-round", blackTex);
                blackPix.dispose();
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

            // Create button styles
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
            logError("Error loading skin resources", e);

            // Create a minimal fallback skin
            skin = new Skin();
            skin.add("default-font", new BitmapFont());

            TextButton.TextButtonStyle fallbackStyle = new TextButton.TextButtonStyle();
            fallbackStyle.font = skin.getFont("default-font");
            skin.add("default", fallbackStyle);

            Label.LabelStyle labelStyle = new Label.LabelStyle();
            labelStyle.font = skin.getFont("default-font");
            labelStyle.fontColor = Color.WHITE;
            skin.add("default", labelStyle);
        }
    }

    private void setupTitle() {
        try {
            // Add title to the top of the screen
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
            titleStyle.fontColor = Color.WHITE;

            // Create title based on placement
            String titleText = (placement == 1) ? "YOU WON!" : "You reached place #" + placement;
            Label titleLabel = new Label(titleText, titleStyle);

            // Add the title to the table
            table.add(titleLabel).padBottom(40);
            table.row();
        } catch (Exception e) {
            logError("Error creating title", e);
            // Add a simple title as fallback
            Label titleLabel = new Label("GAME OVER", new Label.LabelStyle(skin.getFont("default-font"), Color.WHITE));
            table.add(titleLabel).padBottom(40);
            table.row();
        }
    }

    private void setupResultsDisplay() {
        // Add results header
        Label header = new Label("RESULTS", skin);
        table.add(header).padBottom(20);
        table.row();

        // Display each player's result
        for (int i = 0; i < results.size; i++) {
            PlayerResult result = results.get(i);

            // Create label style for this entry
            Label.LabelStyle resultStyle = new Label.LabelStyle(skin.getFont("default-font"), Color.WHITE);

            // Highlight the current player's row
            if (result.name.equals(playerName)) {
                try {
                    BitmapFont playerFont = new BitmapFont();
                    if (skin.has("default-font", BitmapFont.class)) {
                        playerFont = new BitmapFont(skin.getFont("default-font").getData().getFontFile());
                    }
                    playerFont.getData().setScale(1.2f);
                    playerFont.setColor(Color.YELLOW);
                    resultStyle.font = playerFont;
                } catch (Exception e) {
                    logError("Error creating custom font", e);
                    // Just use the default font with a different color if there's an error
                    resultStyle.fontColor = Color.YELLOW;
                }
            }

            // Create and add the result label
            Label resultLabel = new Label((i + 1) + ". " + result.name + " - " + result.score + " points", resultStyle);
            table.add(resultLabel).padBottom(10).left();
            table.row();
        }
    }

    private void setupButtons() {
        // Add back button
        TextButton backButton = new TextButton("Back to Menu", skin);
        table.add(backButton).width(200).height(50).padTop(30);

        // Add button listener
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Return to menu state
                gsm.set(new MenuState(gsm));
            }
        });
    }

    private void saveScoreToLeaderboard(String playerName, int score, GameSession gameSession) {
        try {
            if (leaderboardApi == null) {
                logError("Cannot save score: LeaderboardApi is null");
                return;
            }

            // Calculate game duration if possible
            Double durationSeconds = null;
            if (gameSession != null && gameSession.getStarted_at() != null && gameSession.getEnded_at() != null) {
                // Calculate the duration in seconds between start and end times
                long startMillis = gameSession.getStarted_at().toEpochMilliseconds();
                long endMillis = gameSession.getEnded_at().toEpochMilliseconds();
                durationSeconds = (endMillis - startMillis) / 1000.0;
            }

            // Check if the session-specific method exists
            try {
                // Try to call the specific method if it exists
                leaderboardApi.addLeaderboardEntryFromSession(playerName, score, gameSession);
                log("Score saved to leaderboard");
            } catch (NoSuchMethodError e) {
                // Fallback to standard method if the specialized one doesn't exist
                log("addLeaderboardEntryFromSession not found, using standard method");

                // Call the three-argument version with duration
                leaderboardApi.addLeaderboardEntry(playerName, score, durationSeconds);
                log("Score saved to leaderboard for " + playerName);
            }
        } catch (Exception e) {
            logError("Error saving score to leaderboard", e);
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
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
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
            // Dispose textures that were created or loaded
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
