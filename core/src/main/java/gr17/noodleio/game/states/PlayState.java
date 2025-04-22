package gr17.noodleio.game.states;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

import gr17.noodleio.game.API.PlayerGameStateApi;
import gr17.noodleio.game.API.RealtimeGameStateApi;
import gr17.noodleio.game.Entities.BodyPart;
import gr17.noodleio.game.Entities.Food.Food;
import gr17.noodleio.game.Entities.Food.MagnetBoost;
import gr17.noodleio.game.Entities.Food.PowerUp;
import gr17.noodleio.game.Entities.Food.SpeedBoost;
import gr17.noodleio.game.Entities.Snake;
import gr17.noodleio.game.config.Config;
import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.model.PlayerResult;
import gr17.noodleio.game.models.GameSession;
import gr17.noodleio.game.models.PlayerGameState;
import gr17.noodleio.game.util.ResourceManager;

import java.util.Collections;
import java.util.List;

/**
 * Multiplayer game state that incorporates snake mechanics.
 * Uses cursor-based movement and realtime synchronization with other players.
 */
public class PlayState extends State implements RealtimeGameStateApi.GameStateCallback {

    // Constants
    private static final float SYNC_INTERVAL = 0.1f;
    private static final Color BACKGROUND_COLOR = new Color(0.1f, 0.1f, 0.3f, 1f);
    private static final Color CURSOR_TARGET_COLOR = new Color(1f, 1f, 1f, 0.3f);
    private static final float CURSOR_TARGET_SIZE = 10f;
    private static final int OTHER_PLAYER_SNAKE_SEGMENTS = 5;  // Number of body segments for other players
    private static final Color OTHER_PLAYER_HEAD_COLOR = new Color(0.2f, 0.4f, 0.8f, 1f);  // Blue head
    private static final Color OTHER_PLAYER_BODY_COLOR = new Color(0.1f, 0.3f, 0.7f, 1f);  // Darker blue body

    // Game state
    private String sessionId;
    private String playerId;
    private String playerName;
    private GameSession currentSession;
    private ConcurrentHashMap<String, PlayerGameState> players = new ConcurrentHashMap<>();
    private int lastReportedScore = 0;
    private float scoreUpdateTimer = 0;
    private static final float SCORE_UPDATE_INTERVAL = 1.0f; // Update score every second

    // Client-side prediction
    private Vector2 clientPredictedPosition = new Vector2();
    private Vector2 serverConfirmedPosition = new Vector2();
    private float syncTimer = 0;

    // Cursor tracking
    private Vector2 cursorPosition = new Vector2();
    private Vector2 targetPosition = new Vector2();
    private boolean isMovementActive = false;

    // Speed boost status
    private boolean hasSpeedBoost = false;

    // APIs
    private RealtimeGameStateApi realtimeGameStateApi;
    private PlayerGameStateApi playerGameStateApi;

    // Rendering resources
    private ShapeRenderer shapes;
    private BitmapFont font;
    private SpriteBatch gameBatch;
    private SpriteBatch foodBatch;
    private SpriteBatch uiBatch;
    private ResourceManager resources;
    private Texture backgroundTexture;
    private int mapWidth, mapHeight;
    private SpriteBatch backgroundBatch;

    // Snake-related fields
    private Snake localSnake;
    private ArrayList<Food> foods = new ArrayList<>();
    private ArrayList<PowerUp> powerUps = new ArrayList<>();
    private Map<String, OtherPlayerSnake> otherPlayerSnakes = new HashMap<>();

    /**
     * Simple class to represent other players' snakes with head and body
     */
    private class OtherPlayerSnake {
        public Vector2 headPosition = new Vector2();
        public ArrayList<Vector2> bodyPositions = new ArrayList<>();
        public Vector2 previousPosition = new Vector2();
        public float snakeSize = 15f;  // Same size as the local player's snake

        public OtherPlayerSnake(float x, float y) {
            headPosition.set(x, y);
            previousPosition.set(x, y);

            // Initialize body segments behind the head
            for (int i = 0; i < OTHER_PLAYER_SNAKE_SEGMENTS; i++) {
                bodyPositions.add(new Vector2(x - (i+1) * snakeSize, y));
            }
        }

        public void update(float x, float y) {
            // Calculate movement direction
            Vector2 direction = new Vector2(x, y).sub(headPosition);

            // Skip update if no movement
            if (direction.len() < 0.01f) return;

            // Save previous position before updating
            previousPosition.set(headPosition);

            // Update head position
            headPosition.set(x, y);

            // Update body positions (follow-the-leader)
            Vector2 prevPos = new Vector2(headPosition);
            Vector2 tempPos = new Vector2();

            for (int i = 0; i < bodyPositions.size(); i++) {
                Vector2 currPos = bodyPositions.get(i);
                tempPos.set(currPos);

                // Calculate body segment spacing based on direction
                Vector2 bodyDirection = new Vector2(prevPos).sub(currPos);
                if (bodyDirection.len() > 0.01f) {
                    bodyDirection.nor().scl(snakeSize + 8); // Same spacing as local snake
                    currPos.set(prevPos).sub(bodyDirection);
                }

                prevPos.set(tempPos);
            }
        }

        public void render(ShapeRenderer shapeRenderer) {
            if (bodyPositions.size() < 1) return;

            ArrayList<Vector2> noodlePoints = new ArrayList<>();

            // Add head
            noodlePoints.add(new Vector2(headPosition.x, headPosition.y));

            // Add body parts
            for (Vector2 bodyPos : bodyPositions) {
                noodlePoints.add(new Vector2(bodyPos.x, bodyPos.y));
            }

            // Set noodle thickness
            float noodleThickness = snakeSize * 1.8f;

            // Draw the noodle
            shapeRenderer.setColor(OTHER_PLAYER_BODY_COLOR);

            // Draw continuous line with thickness
            for (int i = 0; i < noodlePoints.size() - 1; i++) {
                Vector2 current = noodlePoints.get(i);
                Vector2 next = noodlePoints.get(i + 1);

                shapeRenderer.rectLine(current.x, current.y, next.x, next.y, noodleThickness);
            }

            // Draw rounded caps at each joint
            for (int i = 0; i < noodlePoints.size(); i++) {
                Vector2 point = noodlePoints.get(i);
                shapeRenderer.circle(point.x, point.y, noodleThickness / 2, 15);
            }

            // Draw head end cap with different color
            shapeRenderer.setColor(OTHER_PLAYER_HEAD_COLOR);
            shapeRenderer.circle(headPosition.x, headPosition.y, noodleThickness / 2, 15);
        }
    }

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
    this.resources = rm;  // Store the resource manager

    // Set up camera
    this.cam = new OrthographicCamera();
    this.cam.setToOrtho(false, Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2);

    // Initialize graphics resources
    this.shapes = new ShapeRenderer();
    this.shapes.setAutoShapeType(true);
    this.font = new BitmapFont();
    this.font.getData().setScale(2);
    this.gameBatch = new SpriteBatch();
    this.foodBatch = new SpriteBatch();
    this.uiBatch = new SpriteBatch();

    this.font.getRegion().getTexture().setFilter(
        Texture.TextureFilter.Linear,
        Texture.TextureFilter.Linear
    );

    // Create a separate batch for background drawing
    this.backgroundBatch = new SpriteBatch();

    // Get the background texture from resources
    this.backgroundTexture = resources.getBackgroundTexture();

    // Set initial map dimensions
    this.mapWidth = 1080;
    this.mapHeight = 1080;

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
        log("Connection result: " + result);
    } catch (Exception e) {
        logError("Error initializing game state APIs", e);
    }

    // Initialize snake-related components
    initializeSnakeComponents();
}

/**
 * Spawn foods at random positions within the map boundary
 */
private void spawnFoods() {
    foods.clear();

    // Get actual map dimensions
    int actualMapWidth = 1080;
    int actualMapHeight = 1080;
    if (currentSession != null) {
        actualMapWidth = currentSession.getMap_length();
        actualMapHeight = currentSession.getMap_height();
    }

    for (int i = 0; i < 50; i++) {
        // Place food within map bounds (in game coordinates)
        // Add a small margin (20 units) to keep food away from edges
        int margin = 20;
        int x = margin + (int)(Math.random() * (actualMapWidth - 2*margin));
        int y = margin + (int)(Math.random() * (actualMapHeight - 2*margin));

        // Convert to screen coordinates for rendering
        Vector2 screenPos = gameToScreenCoordinates(new Vector2(x, y));

        // Get a random food texture (50/50 chance between wheat and egg)
        Texture foodTexture = resources.getRandomFoodTexture();

        // Create a food object with the selected texture
        Food food = new Food(screenPos);
        food.texture = foodTexture;
        foods.add(food);
    }
}

/**
 * Spawn power-ups at specific positions within the map boundary
 */
private void spawnPowerUps() {
    powerUps.clear();

    // Get actual map dimensions
    int actualMapWidth = 1080;
    int actualMapHeight = 1080;
    if (currentSession != null) {
        actualMapWidth = currentSession.getMap_length();
        actualMapHeight = currentSession.getMap_height();
    }

    // Place power-ups at evenly distributed positions
    // Convert game coordinates to screen coordinates
    Vector2 speedPos = gameToScreenCoordinates(new Vector2(actualMapWidth/4, actualMapHeight/4));
    SpeedBoost speedBoost = new SpeedBoost(speedPos, resources.getSpeedBoostTexture());
    powerUps.add(speedBoost);

    Vector2 magnetPos = gameToScreenCoordinates(new Vector2(actualMapWidth*3/4, actualMapHeight*3/4));
    MagnetBoost magnetBoost = new MagnetBoost(magnetPos, resources.getMagnetBoostTexture());
    powerUps.add(magnetBoost);
}

/**
 * Renders game elements using SpriteBatch and ShapeRenderer.
 */
private void renderGameElements() {
    // Draw map boundary
    renderMapBoundary();

    // Begin the food sprite batch with the camera's projection matrix
    foodBatch.begin();
    foodBatch.setProjectionMatrix(cam.combined);

    // Draw all foods
    for (Food f : foods) {
        if (!f.isEat && f.texture != null) {
            // Draw the food texture
            float width = f.size * 2; // Diameter
            float height = f.size * 2;
            foodBatch.draw(f.texture,
                f.pos.x - width/2, // center the texture on the position
                f.pos.y - height/2,
                width, height);
        }
    }

    // Draw all power-ups
    for (PowerUp p : powerUps) {
        if (!p.isEat && p.texture != null) {
            float width = p.size * 2;
            float height = p.size * 2;
            foodBatch.draw(p.texture,
                p.pos.x - width/2,
                p.pos.y - height/2,
                width, height);
        }
    }

    foodBatch.end();

    shapes.begin(ShapeRenderer.ShapeType.Filled);

    // Draw cursor target if movement is active
    if (isMovementActive) {
        shapes.setColor(CURSOR_TARGET_COLOR);
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        cam.unproject(mousePos);
        shapes.circle(mousePos.x, mousePos.y, CURSOR_TARGET_SIZE);

        // Draw a line from player to target
        if (localSnake != null) {
            shapes.setColor(Color.WHITE);
            shapes.rectLine(localSnake.pos.x, localSnake.pos.y,
                mousePos.x, mousePos.y, 1);
        }
    }

    shapes.end();

    // Render other player snakes
    renderOtherPlayerSnakes();

    // Render local snake if it exists
    if (localSnake != null) {
        renderSnake(localSnake, shapes);
    }
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

        // Initialize client predicted position to match snake's starting position
        // This ensures the camera and server know where the snake is from the start
        clientPredictedPosition = screenToGameCoordinates(new Vector2(localSnake.pos.x, localSnake.pos.y));
        serverConfirmedPosition = new Vector2(clientPredictedPosition);

        // Ensure camera is positioned on snake from the beginning
        cam.position.x = localSnake.pos.x;
        cam.position.y = localSnake.pos.y;
        cam.update();

        // Initialize foods with visibility distance from camera
        spawnFoods();

        // Initialize power-ups
        spawnPowerUps();

        // Initialize body segments properly
        for (int i = 1; i < localSnake.body.size(); i++) {
            BodyPart segment = localSnake.body.get(i);
            BodyPart previous = localSnake.body.get(i-1);
            // Position segments properly behind each other
            float distance = segment.size + 8;
            segment.pos.set(previous.pos.x - distance, previous.pos.y);
        }

        // Ensure the snake gets updated at least once before rendering
        if (localSnake != null) {
            // Set a default direction (right) if no movement yet
            Vector3 defaultDirection = new Vector3(
                localSnake.pos.x + 100, localSnake.pos.y, 0);
            localSnake.update(defaultDirection);
        }
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
        // Process input
        handleInput();

        float cappedDt = Math.min(dt, 1/30f); // Cap at 30 FPS minimum

        // If no movement yet but we're just starting, initialize positions
        if (!isMovementActive && serverConfirmedPosition.isZero() && clientPredictedPosition.isZero()) {
            // Initialize positions to snake's starting point
            if (localSnake != null) {
                Vector2 initialGamePos = screenToGameCoordinates(new Vector2(localSnake.pos.x, localSnake.pos.y));
                clientPredictedPosition.set(initialGamePos);
                serverConfirmedPosition.set(initialGamePos);
            }
        }

        // Now handle movement if active
        handleLocalMovement(cappedDt);

        // Sync with server periodically
        syncWithServer(dt);

        // Update the snake position using predicted position
        if (localSnake != null) {
            Vector2 screenPos = gameToScreenCoordinates(clientPredictedPosition);
            localSnake.pos.set(screenPos);
            localSnake.snakeHead.pos.set(screenPos);

            // Update snake direction - even if not moving, provide a default direction
            if (isMovementActive) {
                Vector3 snakeTargetPos = new Vector3();
                snakeTargetPos.set(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY(), 0);
                cam.unproject(snakeTargetPos);
                localSnake.update(snakeTargetPos);
            } else {
                // Default direction if not moving (look right)
                Vector3 defaultDir = new Vector3(localSnake.pos.x + 100, localSnake.pos.y, 0);
                localSnake.update(defaultDir);
            }
        }

        // Update score in the database if it has changed
        scoreUpdateTimer += dt;
        if (scoreUpdateTimer >= SCORE_UPDATE_INTERVAL) {
            scoreUpdateTimer = 0;
            updateScoreIfNeeded();
        }

        // Update other player snakes
        updateOtherPlayerSnakes();

        // Update food interactions
        updateFoodInteractions();

        // Update power-up interactions
        updatePowerUpInteractions();

        // Update camera position to follow player
        updateCameraPosition();
    }

    private void updateScoreIfNeeded() {
        if (localSnake != null && localSnake.score != lastReportedScore) {
            // Score has changed, update it in the database
            int newScore = localSnake.score;
            playerGameStateApi.updatePlayerScore(playerId, sessionId, newScore);
            lastReportedScore = newScore;

            // Log the score update
            log("Updated score to " + newScore);
        }
    }

    /**
     * Updates the snake representations for other players
     */
    private void updateOtherPlayerSnakes() {
        for (PlayerGameState player : players.values()) {
            // Get clean player ID
            String pid = player.getPlayer_id().replace("\"", "");

            // Skip local player
            if (pid.equals(playerId)) continue;

            // Convert from game coordinates to screen coordinates
            Vector2 screenPos = gameToScreenCoordinates(
                new Vector2(player.getX_pos(), player.getY_pos()));

            // Create or update other player snake
            OtherPlayerSnake otherSnake = otherPlayerSnakes.get(pid);
            if (otherSnake == null) {
                // Create new snake for this player
                otherSnake = new OtherPlayerSnake(screenPos.x, screenPos.y);
                otherPlayerSnakes.put(pid, otherSnake);
            } else {
                // Update existing snake
                otherSnake.update(screenPos.x, screenPos.y);
            }
        }

        // Remove snakes for players who are no longer in the game
        ArrayList<String> playersToRemove = new ArrayList<>();
        for (String pid : otherPlayerSnakes.keySet()) {
            boolean playerExists = false;
            for (PlayerGameState player : players.values()) {
                String currentPid = player.getPlayer_id().replace("\"", "");
                if (currentPid.equals(pid)) {
                    playerExists = true;
                    break;
                }
            }

            if (!playerExists) {
                playersToRemove.add(pid);
            }
        }

        // Remove snakes for players who left
        for (String pid : playersToRemove) {
            otherPlayerSnakes.remove(pid);
        }
    }

    /**
     * Updates the camera position to follow the player
     */
    private void updateCameraPosition() {
        // Use the client predicted position for smoother camera following
        if (!clientPredictedPosition.isZero()) {
            // Convert predicted position to screen coordinates
            Vector2 screenPos = gameToScreenCoordinates(clientPredictedPosition);

            // Update camera position
            cam.position.x = screenPos.x;
            cam.position.y = screenPos.y;
            cam.update();
        }
        // Fallback to server position if client position not initialized
        else if (getLocalPlayerState() != null) {
            PlayerGameState localPlayer = getLocalPlayerState();
            Vector2 screenPos = gameToScreenCoordinates(
                new Vector2(localPlayer.getX_pos(), localPlayer.getY_pos()));
            cam.position.x = screenPos.x;
            cam.position.y = screenPos.y;
            cam.update();
        }
        // Final fallback - use the local snake's position directly
        else if (localSnake != null) {
            // Use the snake's position directly as fallback
            cam.position.x = localSnake.pos.x;
            cam.position.y = localSnake.pos.y;
            cam.update();
        }
    }

    /**
     * Update interactions with food items
     */
    private void updateFoodInteractions() {
        if (localSnake == null) return;

        for (int i = 0; i < foods.size(); i++) {
            Food f = foods.get(i);

            // Skip already eaten food
            if (f.isEat) continue;

            // Check collision with snake
            if (localSnake.checkFoodCollision(f)) {
                // Food was eaten, updated in checkFoodCollision
            }

            // Handle magnet attraction - only if needed
            if (localSnake.attractFood && !f.isEat &&
                localSnake.snakeHead.attractFoodDetection(f.collisionShape)) {
                f.getAttracted(localSnake.pos);
            }

            // Update food position
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

                    // Schedule to reset speed after duration
                    new Thread(() -> {
                        try {
                            Thread.sleep(4500); // Same duration as in Snake class
                            hasSpeedBoost = false;
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

    private void handleLocalMovement(float dt) {
        // Skip if no mouse input
        if (!isMovementActive) return;

        // Get current server position
        PlayerGameState localPlayer = getLocalPlayerState();
        if (localPlayer == null) return;

        // Initialize positions if needed
        if (serverConfirmedPosition.isZero()) {
            serverConfirmedPosition.set(localPlayer.getX_pos(), localPlayer.getY_pos());
            clientPredictedPosition.set(serverConfirmedPosition);
        }

        // Get target position from mouse
        Vector3 mousePos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        cam.unproject(mousePos);
        Vector2 targetPos = screenToGameCoordinates(new Vector2(mousePos.x, mousePos.y));

        // Calculate direction and distance
        Vector2 direction = new Vector2(targetPos).sub(clientPredictedPosition);
        float distance = direction.len();

        // Only move if far enough from target
        if (distance > 1.0f) {
            // Normalize and scale by speed and delta time
            direction.nor();
            // Increased speeds for more responsive movement
            float speed = hasSpeedBoost ? 150.0f : 100.0f; // Much higher units per second
            clientPredictedPosition.add(
                direction.x * speed * dt,
                direction.y * speed * dt
            );

            // Keep within map bounds (assuming 1080x1080 map)
            clientPredictedPosition.x = Math.max(0, Math.min(clientPredictedPosition.x, 1080));
            clientPredictedPosition.y = Math.max(0, Math.min(clientPredictedPosition.y, 1080));
        }
    }

    private void syncWithServer(float dt) {
        syncTimer += dt;

        // Time to sync with server
        if (syncTimer >= SYNC_INTERVAL) {
            syncTimer = 0;

            // Calculate direction from last confirmed position
            Vector2 direction = new Vector2(clientPredictedPosition).sub(serverConfirmedPosition);
            float distance = direction.len();

            // Only send if moved significantly
            if (distance > 8.0f) {
                // Decide direction based on largest component
                if (Math.abs(direction.x) > Math.abs(direction.y)) {
                    // Move horizontally
                    if (direction.x > 0) {
                        playerGameStateApi.movePlayerRight(playerId, sessionId);
                    } else {
                        playerGameStateApi.movePlayerLeft(playerId, sessionId);
                    }
                } else {
                    // Move vertically
                    if (direction.y > 0) {
                        playerGameStateApi.movePlayerDown(playerId, sessionId);
                    } else {
                        playerGameStateApi.movePlayerUp(playerId, sessionId);
                    }
                }
            }
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

        // Direct mapping - don't invert Y as the DB functions already handle it correctly
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

        // Direct mapping back to screen coordinates
        float screenX = gamePos.x * Gdx.graphics.getWidth() / mapWidth;
        float screenY = gamePos.y * Gdx.graphics.getHeight() / mapHeight;

        return new Vector2(screenX, screenY);
    }
    /**
     * Renders the background to fill exactly the map boundary
     */
    private void renderBackground() {
        // Get map dimensions
        int mapWidth = 1080, mapHeight = 1080;
        if (currentSession != null) {
            mapWidth = currentSession.getMap_length();
            mapHeight = currentSession.getMap_height();
        }

        // Convert map corners to screen coordinates (same as in renderMapBoundary)
        Vector2 topLeft = gameToScreenCoordinates(new Vector2(0, 0));
        Vector2 bottomRight = gameToScreenCoordinates(new Vector2(mapWidth, mapHeight));

        // Calculate width and height in screen coordinates
        float screenWidth = bottomRight.x - topLeft.x;
        float screenHeight = bottomRight.y - topLeft.y;

        // Draw the background to fill exactly the map boundary
        backgroundBatch.begin();
        backgroundBatch.setProjectionMatrix(cam.combined);
        backgroundBatch.draw(
            backgroundTexture,
            topLeft.x, topLeft.y,  // Position at top-left corner of map
            screenWidth, screenHeight  // Size to fill the entire map in screen coordinates
        );
        backgroundBatch.end();
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

        // Update map dimensions if session is available
        if (currentSession != null) {
            mapWidth = currentSession.getMap_length();
            mapHeight = currentSession.getMap_height();
        }

        // Draw background first
        renderBackground();

        // Set up shape renderer with camera
        shapes.setProjectionMatrix(cam.combined);

        // Draw game elements (including map boundary)
        renderGameElements();

        // Draw UI text
        renderUI();
    }
    /**
     * Renders a snake to look like a noodle instead of separate circles
     */
    private void renderSnake(Snake snake, ShapeRenderer shapeRenderer) {
        if (snake.body.size() < 2) return; // Need at least head and one body part

        // First collect all snake points (head and body)
        ArrayList<Vector2> noodlePoints = new ArrayList<>();

        // Add head
        noodlePoints.add(new Vector2(snake.snakeHead.pos.x, snake.snakeHead.pos.y));

        // Add body parts
        for (BodyPart bp : snake.body) {
            noodlePoints.add(new Vector2(bp.pos.x, bp.pos.y));
        }

        // Set noodle thickness based on head size
        float noodleThickness = snake.snakeHead.size * 1.8f;

        // Create slight color variations for noodle segments
        Color baseColor = snake.snakeHead.color;
        Color[] segmentColors = new Color[noodlePoints.size() - 1];
        for (int i = 0; i < segmentColors.length; i++) {
            float variation = 0.1f * ((float)Math.sin(i * 0.5f + System.currentTimeMillis() / 1000.0) + 1.0f);
            segmentColors[i] = new Color(
                baseColor.r - variation * 0.1f,
                baseColor.g - variation * 0.1f,
                baseColor.b + variation * 0.1f,
                baseColor.a
            );
        }

        // Draw the noodle shape
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw continuous line with thickness and varying colors
        for (int i = 0; i < noodlePoints.size() - 1; i++) {
            Vector2 current = noodlePoints.get(i);
            Vector2 next = noodlePoints.get(i + 1);

            // Use varying colors for each segment
            shapeRenderer.setColor(segmentColors[i]);

            // Draw a thick line between points
            shapeRenderer.rectLine(current.x, current.y, next.x, next.y, noodleThickness);
        }

        // Draw rounded caps at each joint to make it smoother
        for (int i = 0; i < noodlePoints.size(); i++) {
            Vector2 point = noodlePoints.get(i);

            // Use head color for first point, segment colors for others
            if (i == 0) {
                shapeRenderer.setColor(baseColor);
            } else {
                shapeRenderer.setColor(segmentColors[i-1]);
            }

            // Draw a circle at each joint
            shapeRenderer.circle(point.x, point.y, noodleThickness / 2, 15);
        }

        shapeRenderer.end();

        // Draw eyes on the head to give it character
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.WHITE);

        // Calculate eye positions based on head position and direction
        Vector2 headPos = snake.snakeHead.pos;
        Vector2 direction = new Vector2();

        // If we have at least one body segment, determine direction
        if (snake.body.size() > 1) {
            Vector2 bodyPos = snake.body.get(1).pos;
            direction.set(headPos).sub(bodyPos).nor();
        } else {
            direction.set(1, 0); // Default right direction
        }

        // Calculate perpendicular vector for eye positioning
        Vector2 perpendicular = new Vector2(-direction.y, direction.x);

        // Position eyes on the head
        float eyeOffset = snake.snakeHead.size * 0.5f;
        float eyeForwardOffset = snake.snakeHead.size * 0.5f;
        float eyeSize = snake.snakeHead.size * 0.3f;

        Vector2 leftEye = new Vector2(headPos).add(
            new Vector2(direction).scl(eyeForwardOffset).add(
                new Vector2(perpendicular).scl(eyeOffset)
            )
        );

        Vector2 rightEye = new Vector2(headPos).add(
            new Vector2(direction).scl(eyeForwardOffset).add(
                new Vector2(perpendicular).scl(-eyeOffset)
            )
        );

        // Draw the white parts of eyes
        shapeRenderer.circle(leftEye.x, leftEye.y, eyeSize, 10);
        shapeRenderer.circle(rightEye.x, rightEye.y, eyeSize, 10);

        // Draw the pupils
        shapeRenderer.setColor(Color.BLACK);
        shapeRenderer.circle(leftEye.x, leftEye.y, eyeSize * 0.6f, 8);
        shapeRenderer.circle(rightEye.x, rightEye.y, eyeSize * 0.6f, 8);

        shapeRenderer.end();

        // Draw magnet circle if active
        if (snake.attractFood) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.WHITE);
            shapeRenderer.circle(headPos.x, headPos.y, 120, 30);
            shapeRenderer.end();
        }
    }

    /**
     * Renders the map boundary to show the player where the limits are
     */
    private void renderMapBoundary() {
        // Get map dimensions
        int mapWidth = 1080, mapHeight = 1080;
        if (currentSession != null) {
            mapWidth = currentSession.getMap_length();
            mapHeight = currentSession.getMap_height();
        }

        // Convert map coordinates to screen coordinates
        Vector2 topLeft = gameToScreenCoordinates(new Vector2(0, 0));
        Vector2 topRight = gameToScreenCoordinates(new Vector2(mapWidth, 0));
        Vector2 bottomLeft = gameToScreenCoordinates(new Vector2(0, mapHeight));
        Vector2 bottomRight = gameToScreenCoordinates(new Vector2(mapWidth, mapHeight));

        // Set up shape renderer for lines
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(Color.RED);

        // Draw map border (thicker line for visibility)
        float borderThickness = 3.0f;

        // Draw borders
        shapes.rectLine(topLeft.x, topLeft.y, topRight.x, topRight.y, borderThickness);
        shapes.rectLine(topRight.x, topRight.y, bottomRight.x, bottomRight.y, borderThickness);
        shapes.rectLine(bottomRight.x, bottomRight.y, bottomLeft.x, bottomLeft.y, borderThickness);
        shapes.rectLine(bottomLeft.x, bottomLeft.y, topLeft.x, topLeft.y, borderThickness);

        // Add corner markers for better visibility
        float markerSize = 20.0f;
        shapes.setColor(Color.YELLOW);

        // Draw corner markers
        shapes.rectLine(topLeft.x, topLeft.y, topLeft.x + markerSize, topLeft.y, borderThickness);
        shapes.rectLine(topLeft.x, topLeft.y, topLeft.x, topLeft.y + markerSize, borderThickness);

        shapes.rectLine(topRight.x, topRight.y, topRight.x - markerSize, topRight.y, borderThickness);
        shapes.rectLine(topRight.x, topRight.y, topRight.x, topRight.y + markerSize, borderThickness);

        shapes.rectLine(bottomRight.x, bottomRight.y, bottomRight.x - markerSize, bottomRight.y, borderThickness);
        shapes.rectLine(bottomRight.x, bottomRight.y, bottomRight.x, bottomRight.y - markerSize, borderThickness);

        shapes.rectLine(bottomLeft.x, bottomLeft.y, bottomLeft.x + markerSize, bottomLeft.y, borderThickness);
        shapes.rectLine(bottomLeft.x, bottomLeft.y, bottomLeft.x, bottomLeft.y - markerSize, borderThickness);

        shapes.end();
    }

    /**
     * Renders other players as simplified snakes
     */
    private void renderOtherPlayerSnakes() {
        // Begin shape rendering for other player snakes
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Render all other player snakes
        for (OtherPlayerSnake snake : otherPlayerSnakes.values()) {
            snake.render(shapes);
        }

        shapes.end();

        // Draw player names above other players
        gameBatch.begin();
        gameBatch.setProjectionMatrix(cam.combined);

        for (Map.Entry<String, OtherPlayerSnake> entry : otherPlayerSnakes.entrySet()) {
            String pid = entry.getKey();
            OtherPlayerSnake snake = entry.getValue();

            font.setColor(Color.WHITE);
            String displayName = pid.substring(0, Math.min(4, pid.length()));
            font.draw(gameBatch, displayName, snake.headPosition.x - 15, snake.headPosition.y + 30);
        }

        gameBatch.end();
    }

    /**
     * Renders UI elements
     */
    private void renderUI() {
        uiBatch.begin();

        font.setColor(Color.WHITE);

        Map<String, PlayerGameState> playerStates = players;

        float y = Gdx.graphics.getHeight() - 50;
        font.draw(uiBatch, "Players: " + playerStates.size(), 20, y);

        List<PlayerGameState> sortedPlayers = new ArrayList<>(playerStates.values());
        Collections.sort(sortedPlayers, (p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));

        int playerCount = 0;

        for (PlayerGameState player : sortedPlayers) {
            y -= 40;
            String pid = player.getPlayer_id().replace("\"", "");
            String isLocal = pid.equals(playerId) ? " (YOU)" : "";

            font.draw(uiBatch, String.format("Player%s: %d%s",
                pid.substring(0, Math.min(4, pid.length())),
                player.getScore(), isLocal), 20, y);

            playerCount++;
        }

        font.draw(uiBatch, "FPS: " + Gdx.graphics.getFramesPerSecond(),
            Gdx.graphics.getWidth() - 150, Gdx.graphics.getHeight() - 50);

        font.draw(uiBatch, "Eat food! Press and hold to move", 20, 40);

        uiBatch.end();
    }

    /**
     * Disconnects from the game session and returns to the menu.
     */
    private void disconnectAndReturnToMenu() {
        try {
            // Disconnect from the game session
            realtimeGameStateApi.disconnect();
        } catch (Exception e) {
            logError("Error disconnecting", e);
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
            if (backgroundBatch != null) backgroundBatch.dispose();
            if (shapes != null) shapes.dispose();
            if (font != null) font.dispose();
            if (gameBatch != null) gameBatch.dispose();
            if (foodBatch != null) foodBatch.dispose();
            if (uiBatch != null) uiBatch.dispose();

            // Clean up API connections
            if (realtimeGameStateApi != null) {
                realtimeGameStateApi.removeCallback(this);
                realtimeGameStateApi.disconnect();
            }
        } catch (Exception e) {
            logError("Error disposing resources", e);
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

            if (pid.equals(playerId)) {
                serverConfirmedPosition.set(playerState.getX_pos(), playerState.getY_pos());

                // Optional correction if client is too far from server
                Vector2 diff = new Vector2(clientPredictedPosition).sub(serverConfirmedPosition);
                if (diff.len() > 32) { // If more than 2 steps off
                    clientPredictedPosition.lerp(serverConfirmedPosition, 0.5f);
                }
            }
        } catch (Exception e) {
            logError("Error updating player state", e);
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
        log("Game over received");

        // Create a copy of the players to avoid concurrent modification
        final Map<String, PlayerGameState> playersCopy = new HashMap<>(players);

        // Post the state change to happen on the next frame
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    // Disconnect first
                    if (realtimeGameStateApi != null) {
                        realtimeGameStateApi.removeCallback(PlayState.this);
                        realtimeGameStateApi.disconnect();
                    }

                    // Create the results array
                    Array<PlayerResult> results = new Array<>();

                    // Sort players by score
                    List<PlayerGameState> sortedPlayers = new ArrayList<>(playersCopy.values());
                    Collections.sort(sortedPlayers, (p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));

                    // Add players to results
                    for (PlayerGameState player : sortedPlayers) {
                        String pid = player.getPlayer_id().replace("\"", "");
                        String name = pid.equals(playerId) ? playerName : "Player " + pid.substring(0, 4);
                        results.add(new PlayerResult(name, player.getScore()));
                    }

                    // Determine local player's placement
                    int placement = 1;
                    for (int i = 0; i < results.size; i++) {
                        if (results.get(i).name.equals(playerName)) {
                            placement = i + 1;
                            break;
                        }
                    }

                    // Create resource manager
                    ResourceManager rm = new ResourceManager();
                    rm.load();

                    // Transition to EndGameState
                    gsm.set(new EndGameState(gsm, results, playerName, placement, rm, currentSession));
                } catch (Exception e) {
                    logError("Error transitioning to end game state", e);
                    // Fallback to menu if there's an error
                    gsm.set(new MenuState(gsm));
                }
            }
        });
    }
}
