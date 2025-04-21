package gr17.noodleio.game.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

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
 * Uses cursor-based movement for player control.
 */
public class PlayState extends State implements RealtimeGameStateApi.GameStateCallback {
    // Constants
    private static final float MOVEMENT_DELAY = 0.05f;  // 50ms between movement updates
    private static final float PLAYER_SIZE = 15f;      // Size of player circle
    private static final Color BACKGROUND_COLOR = new Color(0.1f, 0.1f, 0.3f, 1f);
    private static final Color CURSOR_TARGET_COLOR = new Color(1f, 1f, 1f, 0.3f);
    private static final float CURSOR_TARGET_SIZE = 10f;

    // Game state
    private String sessionId;
    private String playerId;
    private GameSession currentSession;
    private ConcurrentHashMap<String, PlayerGameState> players = new ConcurrentHashMap<>();
    private float movementCooldown = 0;

    // Cursor tracking
    private Vector2 cursorPosition = new Vector2();
    private Vector2 targetPosition = new Vector2();
    private Vector2 currentPlayerPosition = new Vector2();
    private boolean isMovementActive = false;

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
        this.shapes.setAutoShapeType(true);
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
        // Update cursor position (screen space)
        cursorPosition.set(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());

        // Convert to game coordinates
        targetPosition = screenToGameCoordinates(cursorPosition);

        // Start movement when mouse button is pressed
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            isMovementActive = true;
        }

        // Stop movement when mouse button is released
        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) == false) {
            isMovementActive = false;
        }

        // Handle exit with escape key
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

        // Update player position if movement is active
        if (isMovementActive && movementCooldown <= 0) {
            movePlayerTowardsCursor();
            movementCooldown = MOVEMENT_DELAY;
        }

        // Update current player position reference
        PlayerGameState localPlayer = getLocalPlayerState();
        if (localPlayer != null) {
            currentPlayerPosition.set(localPlayer.getX_pos(), localPlayer.getY_pos());
        }
    }

    /**
     * Moves the player towards the cursor position by making appropriate API calls.
     */
    private void movePlayerTowardsCursor() {
        // Get current player state
        PlayerGameState localPlayer = getLocalPlayerState();
        if (localPlayer == null) return;

        float currentX = localPlayer.getX_pos();
        float currentY = localPlayer.getY_pos();
        float targetX = targetPosition.x;
        float targetY = targetPosition.y;

        // Check which direction to move based on the cursor position
        try {
            // Move horizontally
            if (Math.abs(targetX - currentX) >= 1.0f) {
                if (targetX > currentX) {
                    playerGameStateApi.movePlayerRight(playerId, sessionId);
                } else if (targetX < currentX) {
                    playerGameStateApi.movePlayerLeft(playerId, sessionId);
                }
            }

            // Move vertically
            if (Math.abs(targetY - currentY) >= 1.0f) {
                if (targetY > currentY) {
                    playerGameStateApi.movePlayerDown(playerId, sessionId);
                } else if (targetY < currentY) {
                    playerGameStateApi.movePlayerUp(playerId, sessionId);
                }
            }
        } catch (Exception e) {
            Gdx.app.error("PlayState", "Error processing movement", e);
        }
    }

    /**
     * Gets the current state of the local player.
     *
     * @return The player's game state or null if not found
     */
    private PlayerGameState getLocalPlayerState() {
        for (PlayerGameState player : players.values()) {
            String pid = player.getPlayer_id().replace("\"", "");
            if (pid.equals(playerId)) {
                return player;
            }
        }
        return null;
    }

    /**
     * Converts screen coordinates to game coordinates.
     *
     * @param screenPos The position in screen space
     * @return The position in game coordinate space
     */
    private Vector2 screenToGameCoordinates(Vector2 screenPos) {
        // Get map dimensions if available
        int mapWidth = 1080, mapHeight = 1080;
        if (currentSession != null) {
            mapWidth = currentSession.getMap_length();
            mapHeight = currentSession.getMap_height();
        }

        float gameX = screenPos.x * mapWidth / Gdx.graphics.getWidth();
        float gameY = screenPos.y * mapHeight / Gdx.graphics.getHeight();

        return new Vector2(gameX, gameY);
    }

    /**
     * Converts game coordinates to screen coordinates.
     *
     * @param gamePos The position in game space
     * @return The position in screen coordinate space
     */
    private Vector2 gameToScreenCoordinates(Vector2 gamePos) {
        // Get map dimensions if available
        int mapWidth = 1080, mapHeight = 1080;
        if (currentSession != null) {
            mapWidth = currentSession.getMap_length();
            mapHeight = currentSession.getMap_height();
        }

        float screenX = gamePos.x * Gdx.graphics.getWidth() / mapWidth;
        float screenY = gamePos.y * Gdx.graphics.getHeight() / mapHeight;

        return new Vector2(screenX, screenY);
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
        renderGameElements(sb);

        // Draw UI text
        renderUI(sb);
    }

    /**
     * Renders game elements using ShapeRenderer.
     */
    private void renderGameElements(SpriteBatch sb) {
        // Begin shape rendering
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Draw center crosshair
        float centerX = Gdx.graphics.getWidth() / 2f;
        float centerY = Gdx.graphics.getHeight() / 2f;

        shapes.setColor(Color.RED);
        shapes.rectLine(centerX - 100, centerY, centerX + 100, centerY, 2);
        shapes.rectLine(centerX, centerY - 100, centerX, centerY + 100, 2);

        // Draw map boundary markers
        shapes.setColor(Color.GREEN);
        shapes.circle(0, 0, 10);
        shapes.circle(Gdx.graphics.getWidth(), 0, 10);
        shapes.circle(0, Gdx.graphics.getHeight(), 10);
        shapes.circle(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 10);

        // Draw cursor target if movement is active
        if (isMovementActive) {
            shapes.setColor(CURSOR_TARGET_COLOR);
            Vector2 screenTargetPos = gameToScreenCoordinates(targetPosition);
            shapes.circle(screenTargetPos.x, screenTargetPos.y, CURSOR_TARGET_SIZE);

            // Draw a line from player to target
            PlayerGameState localPlayer = getLocalPlayerState();
            if (localPlayer != null) {
                Vector2 playerScreenPos = gameToScreenCoordinates(
                    new Vector2(localPlayer.getX_pos(), localPlayer.getY_pos()));
                shapes.setColor(Color.WHITE);
                shapes.rectLine(playerScreenPos.x, playerScreenPos.y,
                    screenTargetPos.x, screenTargetPos.y, 1);
            }
        }

        // Draw all players
        for (PlayerGameState player : players.values()) {
            // Get clean player ID
            String pid = player.getPlayer_id().replace("\"", "");
            boolean isLocalPlayer = pid.equals(playerId);

            // Convert from game coordinates to screen coordinates
            Vector2 screenPos = gameToScreenCoordinates(
                new Vector2(player.getX_pos(), player.getY_pos()));

            // Draw player with white outline
            shapes.setColor(Color.WHITE);
            shapes.circle(screenPos.x, screenPos.y, PLAYER_SIZE + 2);

            // Draw player with appropriate color
            shapes.setColor(isLocalPlayer ? Color.YELLOW : Color.BLUE);
            shapes.circle(screenPos.x, screenPos.y, PLAYER_SIZE);

            // Draw player ID above player
            shapes.end();
            sb.begin();
            font.setColor(Color.WHITE);
            font.draw((Batch) sb, pid.substring(0, Math.min(4, pid.length())),
                screenPos.x - 15, screenPos.y + PLAYER_SIZE + 15);
            sb.end();
            shapes.begin();
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
            String pid = player.getPlayer_id().replace("\"", "");
            String isLocal = pid.equals(playerId) ? " (YOU)" : "";
            font.draw(sb, String.format("Player %s: (%.1f, %.1f)%s",
                pid.substring(0, Math.min(4, pid.length())),
                player.getX_pos(), player.getY_pos(), isLocal), 20, y);
        }

        // Draw cursor position
        y -= 40;
        font.draw(sb, String.format("Cursor: (%.1f, %.1f)",
            targetPosition.x, targetPosition.y), 20, y);

        // Draw instructions
        font.draw(sb, "Click and hold to move, ESC to exit", 20, 40);

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

            // Update current player position if this is local player
            if (pid.equals(playerId)) {
                currentPlayerPosition.set(playerState.getX_pos(), playerState.getY_pos());
            }
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
