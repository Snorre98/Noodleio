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

import gr17.noodleio.game.API.LeaderboardApi;
import gr17.noodleio.game.config.Config;
import gr17.noodleio.game.config.EnvironmentConfig;

public class LeaderboardState extends State {
    private Stage stage;
    private Skin skin;
    private Table table;
    private Label statusLabel;
    private Label[] leaderboardLabels;

    // API for leaderboard operations
    private LeaderboardApi leaderboardApi;

    // Number of entries to display
    private static final int TOP_ENTRIES = 5;

    public LeaderboardState(GameStateManager gsm) {
        super(gsm);
        log("Initializing LeaderboardState");

        // Initialize environment config and API
        initializeApi();

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
        setupLeaderboardDisplay();
        setupButtons();

        // Load leaderboard data
        loadLeaderboard();
    }

    private void initializeApi() {
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

        // Initialize leaderboard API
        leaderboardApi = new LeaderboardApi(environmentConfig);
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
                // Create a black texture for buttons
                Pixmap blackPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
                blackPix.setColor(Color.BLACK);
                blackPix.fill();
                Texture blackTex = new Texture(blackPix);
                skin.add("default-round", blackTex);
                blackPix.dispose();
            }

            // Similarly for the down state texture
            if (Gdx.files.internal("default-round-down.png").exists()) {
                skin.add("default-round-down", new Texture(Gdx.files.internal("default-round-down.png")));
            } else {
                // Create a dark gray texture for the pressed button state
                Pixmap grayPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
                grayPix.setColor(new Color(0.2f, 0.2f, 0.2f, 1f)); // Dark gray
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
            Label titleLabel = new Label("LEADERBOARD", titleStyle);

            // Add the title to the table
            table.add(titleLabel).padBottom(40).colspan(2);
            table.row();
        } catch (Exception e) {
            logError("Error creating title", e);
            // Add a simple title as fallback
            Label titleLabel = new Label("LEADERBOARD", new Label.LabelStyle(skin.getFont("default-font"), null));
            table.add(titleLabel).padBottom(40).colspan(2);
            table.row();
        }
    }

    private void setupLeaderboardDisplay() {
        Label header = new Label("PLAYER : SCORE (TIME)", skin);
        table.add(header).padBottom(10);
        table.row();

        leaderboardLabels = new Label[TOP_ENTRIES];
        for (int i = 0; i < TOP_ENTRIES; i++) {
            Label entryLabel = new Label("---", skin);
            table.add(entryLabel).padBottom(10).left();
            table.row();
            leaderboardLabels[i] = entryLabel;
        }

        statusLabel = new Label("Loading leaderboard...", skin);
        table.add(statusLabel).padTop(20);
        table.row();
    }


    private void setupButtons() {
        // Add back button
        TextButton backButton = new TextButton("Back to Menu", skin);
        table.add(backButton).colspan(3).width(200).height(50).padTop(20);

        // Add button listener
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Return to menu state
                gsm.set(new MenuState(gsm));
            }
        });
    }

    private void loadLeaderboard() {
        try {
            // Fetch top entries from the leaderboard
            String result = leaderboardApi.fetchLeaderboard(TOP_ENTRIES);

            // Parse and display entries
            updateLeaderboardDisplay(result);
        } catch (Exception e) {
            logError("Error loading leaderboard", e);
            statusLabel.setText("Error loading leaderboard");
        }
    }

    private void updateLeaderboardDisplay(String leaderboardText) {
        String[] lines = leaderboardText.split("\\r?\\n");

        int labelIndex = 0;
        // Skip header lines
        int startLine = 2; // Skip "TOP x PLAYERS" and "------------------------"

        for (int i = startLine; i < lines.length; i++) {
            if (labelIndex >= TOP_ENTRIES) break;

            // Skip non-data lines
            if (!lines[i].contains(":")) continue;

            if (leaderboardLabels[labelIndex] != null) {
                leaderboardLabels[labelIndex].setText(lines[i].trim());
            }

            labelIndex++;
        }

        // Fill remaining labels with placeholders if any
        for (int i = labelIndex; i < TOP_ENTRIES; i++) {
            if (leaderboardLabels[i] != null) {
                leaderboardLabels[i].setText("---");
            }
        }

        statusLabel.setText("Leaderboard loaded successfully");
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
