package gr17.noodleio.game.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import gr17.noodleio.game.API.PlayerGameStateApi;
import gr17.noodleio.game.API.RealtimeGameStateApi;
import gr17.noodleio.game.config.Config;
import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.models.GameSession;
import gr17.noodleio.game.models.PlayerGameState;
import gr17.noodleio.game.util.ResourceManager;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified game state for multiplayer gameplay.
 * Uses screen coordinates for rendering and handles player movement.
 */
public class PlayState extends State implements RealtimeGameStateApi.GameStateCallback {
    // Constants
    private static final float MOVEMENT_DELAY = 0.1f;  // 100ms between movements
    private static final float PLAYER_SIZE = 15f;      // Size of player circle
    private static final Color BACKGROUND_COLOR = new Color(0.1f, 0.1f, 0.3f, 1f);

    // Game state
    private String sessionId;
    private String playerId;
    private GameSession currentSession;
    private ConcurrentHashMap<String, PlayerGameState> players = new ConcurrentHashMap<>();
    private float movementCooldown = 0;

    // APIs
    private RealtimeGameStateApi realtimeGameStateApi;
    private PlayerGameStateApi playerGameStateApi;

    // Rendering resources
    private ShapeRenderer shapes;
    private BitmapFont font;

    /**
     * Creates a new play state.
     *
     * @param gsm Game state manager
     * @param sessionId ID of the game session
     * @param playerId ID of the local player
     * @param playerName Name of the local player
     * @param rm Resource manager for shared resources
     */
    public PlayState(GameStateManager gsm, String sessionId, String playerId, String playerName, ResourceManager rm) {
        super(gsm);

        // Store player info
        this.sessionId = sessionId;
        this.playerId = playerId;

        // Initialize graphics resources
        this.shapes = new ShapeRenderer();
        this.font = new BitmapFont();
        this.font.getData().setScale(2);

        // Create environment configuration
        EnvironmentConfig config = new EnvironmentConfig() {
            @Override public String getSupabaseUrl() { return Config.getSupabaseUrl(); }
            @Override public String getSupabaseKey() { return Config.getSupabaseKey(); }
        };

        // Initialize APIs and connect to game session
        try {
            this.realtimeGameStateApi = new RealtimeGameStateApi(config);
            this.playerGameStateApi = new PlayerGameStateApi(config);

            // Register for callbacks and connect
            this.realtimeGameStateApi.addCallback(this);
            String result = this.realtimeGameStateApi.connect(sessionId, playerId);
            Gdx.app.log("PlayState", "Connection result: " + result);
        } catch (Exception e) {
            Gdx.app.error("PlayState", "Error initializing game state APIs", e);
        }
    }

    /**
     * Handles player input for movement and game control.
     */
    @Override
    protected void handleInput() {
        // Process movement input if the cooldown timer allows
        if (movementCooldown <= 0) {
            boolean moved = false;

            try {
                if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                    playerGameStateApi.movePlayerUp(playerId, sessionId);
                    moved = true;
                } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                    playerGameStateApi.movePlayerDown(playerId, sessionId);
                    moved = true;
                } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                    playerGameStateApi.movePlayerLeft(playerId, sessionId);
                    moved = true;
                } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                    playerGameStateApi.movePlayerRight(playerId, sessionId);
                    moved = true;
                }
            } catch (Exception e) {
                Gdx.app.error("PlayState", "Error processing movement", e);
            }

            if (moved) {
                movementCooldown = MOVEMENT_DELAY;
            }
        }

        // Handle exit
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            disconnectAndReturnToMenu();
        }
    }

    /**
     * Updates the game state.
     *
     * @param dt Delta time since the last update
     */
    @Override
    public void update(float dt) {
        // Update movement cooldown
        if (movementCooldown > 0) {
            movementCooldown -= dt;
            if (movementCooldown < 0) movementCooldown = 0;
        }

        // Process input
        handleInput();
    }

    /**
     * Renders the game state.
     *
     * @param sb SpriteBatch for rendering text and sprites
     */
    @Override
    public void render(SpriteBatch sb) {
        // Clear screen with background color
        Gdx.gl.glClearColor(
            BACKGROUND_COLOR.r,
            BACKGROUND_COLOR.g,
            BACKGROUND_COLOR.b,
            BACKGROUND_COLOR.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw game elements
        renderGameElements();

        // Draw UI text
        renderUI(sb);
    }

    /**
     * Renders game elements using ShapeRenderer.
     */
    private void renderGameElements() {
        // Begin shape rendering
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Draw center crosshair
        float centerX = Gdx.graphics.getWidth() / 2f;
        float centerY = Gdx.graphics.getHeight() / 2f;

        shapes.setColor(Color.RED);
        shapes.rectLine(centerX - 100, centerY, centerX + 100, centerY, 10);
        shapes.rectLine(centerX, centerY - 100, centerX, centerY + 100, 10);

        // Draw corner markers
        shapes.setColor(Color.GREEN);
        shapes.circle(0, 0, 30);
        shapes.circle(Gdx.graphics.getWidth(), 0, 30);
        shapes.circle(0, Gdx.graphics.getHeight(), 30);
        shapes.circle(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 30);

        // Draw all players
        for (PlayerGameState player : players.values()) {
            // Convert from game coordinates to screen coordinates
            float x = player.getX_pos() * Gdx.graphics.getWidth() / 1080;
            float y = player.getY_pos() * Gdx.graphics.getHeight() / 1080;

            // Check if this is the local player
            boolean isLocalPlayer = player.getPlayer_id().replace("\"", "").equals(playerId);

            // Draw player with white outline
            shapes.setColor(Color.WHITE);
            shapes.circle(x, y, PLAYER_SIZE + 2);

            // Draw player with appropriate color
            shapes.setColor(isLocalPlayer ? Color.YELLOW : Color.BLUE);
            shapes.circle(x, y, PLAYER_SIZE);
        }

        shapes.end();
    }

    /**
     * Renders UI elements using SpriteBatch.
     */
    private void renderUI(SpriteBatch sb) {
        sb.begin();
        font.setColor(Color.WHITE);

        // Draw player list and positions
        float y = Gdx.graphics.getHeight() - 50;
        font.draw(sb, "Players: " + players.size(), 20, y);

        for (PlayerGameState player : players.values()) {
            y -= 40;
            String isLocal = player.getPlayer_id().replace("\"", "").equals(playerId) ? " (YOU)" : "";
            font.draw(sb, String.format("Player %s: (%.1f, %.1f)%s",
                player.getPlayer_id().substring(0, Math.min(4, player.getPlayer_id().length())),
                player.getX_pos(), player.getY_pos(), isLocal), 20, y);
        }

        // Draw instructions
        font.draw(sb, "Arrow keys to move, ESC to exit", 20, 40);

        sb.end();
    }

    /**
     * Disconnects from the game session and returns to the menu.
     */
    private void disconnectAndReturnToMenu() {
        try {
            // Disconnect from the game session
            realtimeGameStateApi.disconnect();
        } catch (Exception e) {
            Gdx.app.error("PlayState", "Error disconnecting", e);
        }

        // Return to menu state
        gsm.set(new MenuState(gsm));
    }

    /**
     * Disposes resources when the state is no longer needed.
     */
    @Override
    public void dispose() {
        try {
            // Dispose rendering resources
            if (shapes != null) shapes.dispose();
            if (font != null) font.dispose();

            // Clean up API connections
            if (realtimeGameStateApi != null) {
                realtimeGameStateApi.removeCallback(this);
                realtimeGameStateApi.disconnect();
            }
        } catch (Exception e) {
            Gdx.app.error("PlayState", "Error disposing resources", e);
        }
    }

    // RealtimeGameStateApi.GameStateCallback implementation

    /**
     * Called when a player's state changes.
     */
    @Override
    public void onPlayerStateChanged(PlayerGameState playerState) {
        try {
            // Clean player ID and store the updated state
            String pid = playerState.getPlayer_id().replace("\"", "");
            players.put(pid, playerState);
        } catch (Exception e) {
            Gdx.app.error("PlayState", "Error updating player state", e);
        }
    }

    /**
     * Called when the game session changes.
     */
    @Override
    public void onGameSessionChanged(GameSession gameSession) {
        this.currentSession = gameSession;
    }

    /**
     * Called when the game is over.
     */
    @Override
    public void onGameOver() {
        Gdx.app.log("PlayState", "Game over received");
        // Could transition to an end game state here
    }
}
