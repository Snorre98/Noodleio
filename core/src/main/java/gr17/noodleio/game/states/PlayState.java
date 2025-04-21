package gr17.noodleio.game.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import gr17.noodleio.game.API.PlayerGameStateApi;
import gr17.noodleio.game.API.RealtimeGameStateApi;
import gr17.noodleio.game.config.Config;
import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.models.GameSession;
import gr17.noodleio.game.models.PlayerGameState;
import gr17.noodleio.game.util.ResourceManager;

// Import snake-related classes
import gr17.noodleio.game.Entities.Snake;
import gr17.noodleio.game.Entities.Food.Food;
import gr17.noodleio.game.Entities.Food.PowerUp;
import gr17.noodleio.game.Entities.Food.SpeedBoost;
import gr17.noodleio.game.Entities.Food.MagnetBoost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multiplayer game state that incorporates snake mechanics.
 * Uses cursor-based movement and realtime synchronization with other players.
 */
public class PlayState extends State implements RealtimeGameStateApi.GameStateCallback {
    // Constants
    private static final float MOVEMENT_DELAY = 0.05f;  // 50ms between movement updates
    private static final Color BACKGROUND_COLOR = new Color(0.1f, 0.1f, 0.3f, 1f);
    private static final Color CURSOR_TARGET_COLOR = new Color(1f, 1f, 1f, 0.3f);
    private static final float CURSOR_TARGET_SIZE = 10f;

    // Game state
    private String sessionId;
    private String playerId;
    private String playerName;
    private GameSession currentSession;
    private ConcurrentHashMap<String, PlayerGameState> players = new ConcurrentHashMap<>();
    private float movementCooldown = 0;

    // Cursor tracking
    private Vector2 cursorPosition = new Vector2();
    private Vector2 targetPosition = new Vector2();
    private Vector2 currentPlayerPosition = new Vector2();
    private boolean isMovementActive = false;

    // Speed boost status
    private boolean hasSpeedBoost = false;
    private float speedMultiplier = 1.0f;

    // APIs
    private RealtimeGameStateApi realtimeGameStateApi;
    private PlayerGameStateApi playerGameStateApi;

    // Rendering resources
    private ShapeRenderer shapes;
    private BitmapFont font;
    private SpriteBatch gameBatch;

    // Snake-related fields
    private Snake localSnake;
    private ArrayList<Food> foods = new ArrayList<>();
    private ArrayList<PowerUp> powerUps = new ArrayList<>();
    private Map<String, Snake> otherPlayerSnakes = new HashMap<>();

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
        this.playerName = playerName;

        // Set up camera
        this.cam = new OrthographicCamera();
        this.cam.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // Initialize graphics resources
        this.shapes = new ShapeRenderer();
        this.shapes.setAutoShapeType(true);
        this.font = new BitmapFont();
        this.font.getData().setScale(2);
        this.gameBatch = new SpriteBatch();

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

        // Initialize snake-related components
        initializeSnakeComponents();
    }

    /**
     * Initialize snake components (local player snake, foods, power-ups)
     */
    private void initializeSnakeComponents() {
        // Initialize the local player's snake
        localSnake = new Snake();

        // Position snake in the center initially
        localSnake.pos.set(Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);
        localSnake.snakeHead.pos.set(localSnake.pos);

        // Initialize foods with visibility distance from camera
        spawnFoods();

        // Initialize power-ups
        spawnPowerUps();
    }

    /**
     * Spawn foods at random positions
     */
    private void spawnFoods() {
        foods.clear();
        int viewWidth = Gdx.graphics.getWidth();
        int viewHeight = Gdx.graphics.getHeight();

        for (int i = 0; i < 50; i++) {
            // Place food within view bounds plus some margin
            int x = (int)(Math.random() * viewWidth * 2) - viewWidth/2;
            int y = (int)(Math.random() * viewHeight * 2) - viewHeight/2;
            foods.add(new Food(new Vector2(x, y)));
        }
    }

    /**
     * Spawn power-ups at specific positions
     */
    private void spawnPowerUps() {
        powerUps.clear();
        int viewWidth = Gdx.graphics.getWidth();
        int viewHeight = Gdx.graphics.getHeight();

        // Place power-ups at visible positions
        powerUps.add(new SpeedBoost(new Vector2(viewWidth/4, viewHeight/4)));
        powerUps.add(new MagnetBoost(new Vector2(viewWidth*3/4, viewHeight/4)));
    }

    /**
     * Handles player input for movement and game control.
     */
    @Override
    protected void handleInput() {
        // Update cursor position (screen space)
        cursorPosition.set(Gdx.input.getX(), Gdx.input.getY());

        // Convert to game coordinates
        targetPosition = screenToGameCoordinates(cursorPosition);

        // Start movement when mouse button is pressed
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            isMovementActive = true;
        }

        // Stop movement when mouse button is released
        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
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

            // Convert to screen coordinates for the snake
            Vector2 screenPos = gameToScreenCoordinates(currentPlayerPosition);

            // Update snake head position to match player position
            if (localSnake != null) {
                // Update snake position to match game position
                localSnake.pos.set(screenPos);
                localSnake.snakeHead.pos.set(screenPos);

                // Create a Vector3 for the snake's update method
                Vector3 snakeTargetPos = new Vector3();

                if (isMovementActive) {
                    // Use mouse position as target for visual effect
                    snakeTargetPos.set(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), 0);
                    cam.unproject(snakeTargetPos);
                    localSnake.update(snakeTargetPos);
                }
            }
        }

        // Update food interactions
        updateFoodInteractions();

        // Update power-up interactions
        updatePowerUpInteractions();

        // Update camera position to follow player
        updateCameraPosition();
    }

    /**
     * Updates the camera position to follow the player
     */
    private void updateCameraPosition() {
        PlayerGameState localPlayer = getLocalPlayerState();
        if (localPlayer != null) {
            // Convert game position to screen position
            Vector2 screenPos = gameToScreenCoordinates(
                new Vector2(localPlayer.getX_pos(), localPlayer.getY_pos()));

            // Smoothly move camera toward player
            cam.position.x = screenPos.x;
            cam.position.y = screenPos.y;
            cam.update();
        }
    }

    /**
     * Update interactions with food items
     */
    private void updateFoodInteractions() {
        PlayerGameState localPlayer = getLocalPlayerState();
        if (localPlayer == null || localSnake == null) return;

        for (int i = 0; i < foods.size(); i++) {
            Food f = foods.get(i);

            if (localSnake.checkFoodCollision(f)) {
                // Food was eaten, could update score in database here
            }

            if (localSnake.attractFood && localSnake.snakeHead.attractFoodDetection(f.collisionShape)) {
                Vector2 snakePos = new Vector2(localSnake.pos.x, localSnake.pos.y);
                f.getAttracted(snakePos);
            }

            f.update();
        }
    }

    /**
     * Update interactions with power-ups
     */
    private void updatePowerUpInteractions() {
        if (localSnake == null) return;

        for (PowerUp p : powerUps) {
            if (localSnake.checkFoodCollision(p)) {
                if (p.getType().equals("speed")) {
                    localSnake.enableSpeedBoost();
                    hasSpeedBoost = true;
                    speedMultiplier = 2.0f; // Increase movement speed

                    // Schedule to reset speed after duration
                    new Thread(() -> {
                        try {
                            Thread.sleep(4500); // Same duration as in Snake class
                            hasSpeedBoost = false;
                            speedMultiplier = 1.0f;
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }

                if (p.getType().equals("magnet")) {
                    localSnake.enableMagnetBoost();
                    // Magnet effect is handled in the Snake class
                }
            }

            p.update();
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
            // Apply speed multiplier if speed boost is active
            int movesToMake = hasSpeedBoost ? 2 : 1;

            for (int i = 0; i < movesToMake; i++) {
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

        // Invert Y because screen coordinates have origin at top-left
        float gameX = screenPos.x * mapWidth / Gdx.graphics.getWidth();
        float gameY = (Gdx.graphics.getHeight() - screenPos.y) * mapHeight / Gdx.graphics.getHeight();

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

        // Invert Y to convert back to screen coordinates
        float screenX = gamePos.x * Gdx.graphics.getWidth() / mapWidth;
        float screenY = Gdx.graphics.getHeight() - (gamePos.y * Gdx.graphics.getHeight() / mapHeight);

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

        // Update camera
        cam.update();

        // Set up shape renderer with camera
        shapes.setProjectionMatrix(cam.combined);

        // Draw game elements
        renderGameElements(sb);

        // Draw UI text (UI uses the passed batch which has identity projection)
        renderUI(sb);
    }

    /**
     * Renders game elements using ShapeRenderer and SpriteBatch.
     */
    private void renderGameElements(SpriteBatch sb) {
        // Begin game batch with camera projection
        gameBatch.setProjectionMatrix(cam.combined);
        gameBatch.begin();
        gameBatch.end();

        // Render foods
        for (Food f : foods) {
            f.render(cam);
        }

        // Render power-ups
        for (PowerUp p : powerUps) {
            p.render(cam);
        }

        // Render local snake if it exists
        if (localSnake != null) {
            localSnake.render(cam);
        }

        // Begin shape rendering
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Draw cursor target if movement is active
        if (isMovementActive) {
            shapes.setColor(CURSOR_TARGET_COLOR);
            shapes.circle(cursorPosition.x, cursorPosition.y, CURSOR_TARGET_SIZE);

            // Draw a line from player to target
            PlayerGameState localPlayer = getLocalPlayerState();
            if (localPlayer != null) {
                Vector2 playerScreenPos = gameToScreenCoordinates(
                    new Vector2(localPlayer.getX_pos(), localPlayer.getY_pos()));
                shapes.setColor(Color.WHITE);
                shapes.rectLine(playerScreenPos.x, playerScreenPos.y,
                    cursorPosition.x, cursorPosition.y, 1);
            }
        }

        // Render other players as simplified snakes
        renderOtherPlayers();

        shapes.end();
    }

    /**
     * Renders other players as simplified snakes
     */
    private void renderOtherPlayers() {
        for (PlayerGameState player : players.values()) {
            // Get clean player ID
            String pid = player.getPlayer_id().replace("\"", "");
            boolean isLocalPlayer = pid.equals(playerId);

            // Skip the local player as it's rendered as a snake
            if (isLocalPlayer) continue;

            // Convert from game coordinates to screen coordinates
            Vector2 screenPos = gameToScreenCoordinates(
                new Vector2(player.getX_pos(), player.getY_pos()));

            // Draw player (simplified for now)
            shapes.setColor(Color.BLUE); // Other players are blue
            shapes.circle(screenPos.x, screenPos.y, 15); // Simple circle for now

            // Draw player ID/name above player
            // (Drawing text is done in a separate pass with SpriteBatch)
        }

        // Draw player names above other players
        gameBatch.begin();
        for (PlayerGameState player : players.values()) {
            String pid = player.getPlayer_id().replace("\"", "");
            boolean isLocalPlayer = pid.equals(playerId);

            // Skip the local player
            if (isLocalPlayer) continue;

            Vector2 screenPos = gameToScreenCoordinates(
                new Vector2(player.getX_pos(), player.getY_pos()));

            font.setColor(Color.WHITE);
            String displayName = pid.substring(0, Math.min(4, pid.length()));
            font.draw(gameBatch, displayName, screenPos.x - 15, screenPos.y - 25);
        }
        gameBatch.end();
    }

    /**
     * Renders UI elements using SpriteBatch.
     */
    private void renderUI(SpriteBatch sb) {
        // UI is drawn with the passed SpriteBatch (identity projection)
        sb.begin();
        font.setColor(Color.WHITE);

        // Draw player list and positions
        float y = Gdx.graphics.getHeight() - 50;
        font.draw(sb, "Players: " + players.size(), 20, y);

        // Limit to 3 players in the UI to avoid cluttering
        int playerCount = 0;
        for (PlayerGameState player : players.values()) {
            if (playerCount >= 3) break;

            y -= 40;
            String pid = player.getPlayer_id().replace("\"", "");
            String isLocal = pid.equals(playerId) ? " (YOU)" : "";
            font.draw(sb, String.format("Player %s: (%.1f, %.1f)%s",
                pid.substring(0, Math.min(4, pid.length())),
                player.getX_pos(), player.getY_pos(), isLocal), 20, y);

            playerCount++;
        }

        // Draw cursor position
        y -= 40;
        font.draw(sb, String.format("Cursor: (%.1f, %.1f)",
            targetPosition.x, targetPosition.y), 20, y);

        // Draw score if local snake exists
        if (localSnake != null) {
            font.draw(sb, "Score: " + localSnake.score,
                Gdx.graphics.getWidth() - 150, Gdx.graphics.getHeight() - 50);
        }

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
            if (gameBatch != null) gameBatch.dispose();

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
