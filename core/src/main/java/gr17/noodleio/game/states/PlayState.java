package gr17.noodleio.game.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import gr17.noodleio.game.API.LobbyPlayerApi;
import gr17.noodleio.game.API.PlayerGameStateApi;
import gr17.noodleio.game.API.RealtimeGameStateApi;
import gr17.noodleio.game.config.Config;
import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.model.PlayerResult;
import gr17.noodleio.game.models.GameSession;
import gr17.noodleio.game.models.PlayerGameState;
import gr17.noodleio.game.util.ResourceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayState extends State implements RealtimeGameStateApi.GameStateCallback {
    // Session and player info
    private String sessionId;
    private String playerId;
    private String playerName;
    private boolean isGameOver = false;

    // APIs for game state management
    private PlayerGameStateApi playerGameStateApi;
    private RealtimeGameStateApi realtimeGameStateApi;
    private LobbyPlayerApi lobbyPlayerApi;

    // Rendering
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Viewport gameViewport;
    private Viewport uiViewport;
    private OrthographicCamera uiCamera;

    // Game state tracking
    private ConcurrentHashMap<String, PlayerGameState> playerStates = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> playerNames = new ConcurrentHashMap<>();
    private GameSession currentSession;
    private Map<String, Color> playerColors = new HashMap<>();
    private Color[] colorPalette = {
        Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW,
        Color.PURPLE, Color.CYAN, Color.ORANGE, Color.PINK
    };

    // Movement cooldown to prevent spamming
    private float movementCooldown = 0;
    private static final float MOVEMENT_DELAY = 0.1f; // 100ms between movements

    // Player visualization
    private static final float PLAYER_SIZE = 20;
    private ResourceManager resourceManager;

    /**
     * Constructor for PlayState
     * @param gsm Game State Manager
     * @param sessionId The game session ID
     * @param playerId The local player's ID
     * @param playerName The local player's name
     * @param rm Resource Manager for fonts and textures
     */
    public PlayState(GameStateManager gsm, String sessionId, String playerId, String playerName, ResourceManager rm) {
        super(gsm);
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.resourceManager = rm;

        // Initialize environment config
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

        // Initialize APIs
        playerGameStateApi = new PlayerGameStateApi(environmentConfig);
        realtimeGameStateApi = new RealtimeGameStateApi(environmentConfig);
        lobbyPlayerApi = new LobbyPlayerApi(environmentConfig);

        // Register as a callback for game state updates
        realtimeGameStateApi.addCallback(this);

        // Initialize rendering tools
        shapeRenderer = new ShapeRenderer();
        font = resourceManager != null ? resourceManager.getDefaultFont() : new BitmapFont();

        // Set up game viewport for world coordinates
        gameViewport = new FitViewport(1080, 1080, cam);

        // Set up UI viewport for screen coordinates
        uiCamera = new OrthographicCamera();
        uiViewport = new FitViewport(800, 480, uiCamera);

        // Connect to the game session
        connectToGameSession();

        // Map player names (since we have the local player's name)
        playerNames.put(playerId, playerName);

        // Assign a color to the local player
        playerColors.put(playerId, colorPalette[0]);

        Gdx.app.log("PlayState", "Initialized with sessionId: " + sessionId + ", playerId: " + playerId);
    }

    /**
     * Connect to the game session using the realtime API
     */
    private void connectToGameSession() {
        String result = realtimeGameStateApi.connect(sessionId, playerId);
        Gdx.app.log("PlayState", "Connection result: " + result);
    }

    @Override
    protected void handleInput() {
        // Only process input if movement cooldown has elapsed
        if (movementCooldown <= 0) {
            boolean moved = false;

            // Movement controls
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
                String result = playerGameStateApi.movePlayerUp(playerId, sessionId);
                Gdx.app.log("PlayState", "Move up result: " + result);
                moved = true;
            } else if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                String result = playerGameStateApi.movePlayerDown(playerId, sessionId);
                Gdx.app.log("PlayState", "Move down result: " + result);
                moved = true;
            } else if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                String result = playerGameStateApi.movePlayerLeft(playerId, sessionId);
                Gdx.app.log("PlayState", "Move left result: " + result);
                moved = true;
            } else if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                String result = playerGameStateApi.movePlayerRight(playerId, sessionId);
                Gdx.app.log("PlayState", "Move right result: " + result);
                moved = true;
            }

            if (moved) {
                movementCooldown = MOVEMENT_DELAY;
            }
        }

        // Escape key to exit the game
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            disconnectAndReturnToMenu();
        }
    }

//    @Override
//    public void update(float dt) {
//        // Update movement cooldown
//        if (movementCooldown > 0) {
//            movementCooldown -= dt;
//        }
//
//        // Only handle input if the game is not over
//        if (!isGameOver) {
//            handleInput();
//        }
//
//        // Check for game over condition
//        if (currentSession != null && currentSession.getEnded_at() != null) {
//            if (!isGameOver) {
//                handleGameOver();
//            }
//        }
//    }


    @Override
    public void update(float dt) {
        // Update movement cooldown
        if (movementCooldown > 0) {
            movementCooldown -= dt;
        }

        // Only handle input if the game is not over
        if (!isGameOver) {
            handleInput();
        }

        // Update camera to follow local player if exists
        updateCameraPosition();

        // Check for game over condition
        if (currentSession != null && currentSession.getEnded_at() != null) {
            if (!isGameOver) {
                handleGameOver();
            }
        }
    }

    private void updateCameraPosition() {
        // Follow the local player if it exists in player states
        PlayerGameState localPlayer = playerStates.get(playerId);
        if (localPlayer != null) {
            // Smoothly move camera to player position
            float lerp = 0.1f; // Adjust for smoother/faster movement
            cam.position.x += (localPlayer.getX_pos() - cam.position.x) * lerp;
            cam.position.y += (localPlayer.getY_pos() - cam.position.y) * lerp;

            // Keep camera within map bounds
            if (currentSession != null) {
                float halfViewportWidth = cam.viewportWidth * cam.zoom / 2;
                float halfViewportHeight = cam.viewportHeight * cam.zoom / 2;

                cam.position.x = Math.max(halfViewportWidth, Math.min(currentSession.getMap_length() - halfViewportWidth, cam.position.x));
                cam.position.y = Math.max(halfViewportHeight, Math.min(currentSession.getMap_height() - halfViewportHeight, cam.position.y));
            }

            cam.update();
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        // Clear the screen
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Set the game viewport for rendering the game world
        gameViewport.apply();

        // Render game elements using ShapeRenderer
        renderGameElements();

        // Render player names in the game world
        renderPlayerNames(sb);

        // Switch to UI viewport for text rendering
        uiViewport.apply();
        sb.setProjectionMatrix(uiCamera.combined);

        // Begin sprite batch for UI text rendering
        sb.begin();
        renderUI(sb);
        sb.end();
    }

    /**
     * Render player names above their character
     */
    private void renderPlayerNames(SpriteBatch sb) {
        if (currentSession == null) return;

        // Set projection matrix for the game world
        sb.setProjectionMatrix(cam.combined);
        sb.begin();

        // Draw player names above each player
        for (PlayerGameState playerState : playerStates.values()) {
            String pid = playerState.getPlayer_id();
            String name = playerNames.getOrDefault(pid, "Player " + pid.substring(0, 4));

            // Get the color for this player
            Color playerColor = playerColors.getOrDefault(pid, Color.WHITE);
            font.setColor(playerColor);

            // Calculate text position above the player
            // We need a different approach since BitmapFont doesn't have getBounds
            // Instead, measure width approximately based on character count
            float textWidth = name.length() * 8; // Approximate width
            float textX = playerState.getX_pos() - (textWidth / 2);
            float textY = playerState.getY_pos() + PLAYER_SIZE + 10; // Position above player

            // Draw the player name
            font.draw(sb, name, textX, textY);
        }

        // Reset font color
        font.setColor(Color.WHITE);
        sb.end();
    }

    /**
     * Render game elements using ShapeRenderer
     */
    private void renderGameElements() {
        if (currentSession == null) return;

        shapeRenderer.setProjectionMatrix(cam.combined);

        // Draw game boundary
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(0, 0, currentSession.getMap_length(), currentSession.getMap_height());
        shapeRenderer.end();

        // Draw players
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (PlayerGameState playerState : playerStates.values()) {
            // Get or assign a color for this player
            String pid = playerState.getPlayer_id();
            if (!playerColors.containsKey(pid)) {
                int colorIndex = playerColors.size() % colorPalette.length;
                playerColors.put(pid, colorPalette[colorIndex]);
            }

            // Draw the player
            shapeRenderer.setColor(playerColors.get(pid));
            shapeRenderer.rect(
                playerState.getX_pos() - PLAYER_SIZE / 2,
                playerState.getY_pos() - PLAYER_SIZE / 2,
                PLAYER_SIZE, PLAYER_SIZE
            );
        }

        shapeRenderer.end();
    }

    /**
     * Render UI elements using SpriteBatch
     */
    private void renderUI(SpriteBatch sb) {
        // Render scores
        float yPos = uiViewport.getWorldHeight() - 20;
        float xPos = 10;

        // Title
        font.draw(sb, "SCORES:", xPos, yPos);
        yPos -= 30;

        // Render each player's score
        for (PlayerGameState playerState : playerStates.values()) {
            String pid = playerState.getPlayer_id();
            String name = playerNames.getOrDefault(pid, "Player " + pid.substring(0, 4));

            // Get the assigned color for this player
            if (playerColors.containsKey(pid)) {
                font.setColor(playerColors.get(pid));
            } else {
                font.setColor(Color.WHITE);
            }

            font.draw(sb, name + ": " + playerState.getScore(), xPos, yPos);
            yPos -= 25;
        }

        // Reset font color
        font.setColor(Color.WHITE);

        // Display connection status
        String status = realtimeGameStateApi.getConnectionStatus();
        font.draw(sb, status, 10, 20);

        // Display game over message if applicable
        if (isGameOver) {
            Gdx.app.log("PlayerState", "Game Over!");
            //String gameOverText = "GAME OVER!";

//            font.draw(sb, gameOverText,
//                uiViewport.getWorldWidth() / 2 - font.getBounds(gameOverText).width / 2,
//                uiViewport.getWorldHeight() / 2
//            );
        }
    }

    /**
     * Handle game over - prepare to transition to the EndGameState
     * This is a placeholder for future implementation
     */
    private void handleGameOver() {
        isGameOver = true;
        Gdx.app.log("PlayState", "Game over detected - TODO: Implement full game over handling");

        // TODO: Implement proper game over handling and EndGameState transition
        // For now, just display a message and return to menu after a delay

        // Add a delay before returning to menu
        new Thread(() -> {
            try {
                Thread.sleep(3000); // 3 second delay
                Gdx.app.postRunnable(this::disconnectAndReturnToMenu);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Disconnect from the game session and return to the menu
     */
    private void disconnectAndReturnToMenu() {
        // Disconnect from game session
        String result = realtimeGameStateApi.disconnect();
        Gdx.app.log("PlayState", "Disconnection result: " + result);

        // Return to menu
        gsm.set(new MenuState(gsm));
    }

    @Override
    public void dispose() {
        // Clean up resources
        shapeRenderer.dispose();

        if (font != null && resourceManager == null) {
            // Only dispose the font if we created it (not if it came from ResourceManager)
            font.dispose();
        }

        // Disconnect from the game session
        realtimeGameStateApi.disconnect();

        // Remove callbacks
        realtimeGameStateApi.removeCallback(this);
    }

//    public void resize(int width, int height) {
//        gameViewport.update(width, height, true);
//        uiViewport.update(width, height, true);
//    }

    //
    // RealtimeGameStateApi.GameStateCallback implementation
    //

    @Override
    public void onPlayerStateChanged(PlayerGameState playerState) {
        // Update local player state cache
        playerStates.put(playerState.getPlayer_id(), playerState);

        // If this is a new player, try to get their name
        if (!playerNames.containsKey(playerState.getPlayer_id())) {
            // Clean the player ID by removing any extra quotes
            String cleanPlayerId = playerState.getPlayer_id().replace("\"", "");

            // This would happen asynchronously in a real implementation
            String result = lobbyPlayerApi.getPlayerById(cleanPlayerId);
            if (result != null && result.contains("Player found:")) {
                // Extract the player name from the result string
                int nameStartIndex = result.indexOf("Player found: ") + 14;
                int nameEndIndex = result.indexOf(" (ID:", nameStartIndex);
                if (nameStartIndex >= 0 && nameEndIndex > nameStartIndex) {
                    String name = result.substring(nameStartIndex, nameEndIndex);
                    playerNames.put(playerState.getPlayer_id(), name);
                }
            }
        }
    }

//    @Override
//    public void onGameSessionChanged(GameSession gameSession) {
//        this.currentSession = gameSession;
//
//        // If this is the first time we're getting the session, initialize the camera
//        if (currentSession != null) {
//            // Center the camera on the map
//            cam.position.set(
//                currentSession.getMap_length() / 2f,
//                currentSession.getMap_height() / 2f,
//                0
//            );
//
//            // Set zoom to show the entire map
//            // Calculate the zoom factor based on map dimensions and screen size
//            float mapRatio = (float)currentSession.getMap_length() / currentSession.getMap_height();
//            float screenRatio = (float)Gdx.graphics.getWidth() / Gdx.graphics.getHeight();
//
//            if (mapRatio > screenRatio) {
//                // Width limited
//                cam.zoom = currentSession.getMap_length() / Gdx.graphics.getWidth();
//            } else {
//                // Height limited
//                cam.zoom = currentSession.getMap_height() / Gdx.graphics.getHeight();
//            }
//
//            cam.update();
//        }
//    }

    @Override
    public void onGameSessionChanged(GameSession gameSession) {
        this.currentSession = gameSession;

        // If this is the first time we're getting the session, initialize the camera
        if (currentSession != null) {
            // Center the camera on the game map
            cam.position.set(
                currentSession.getMap_length() / 2f,
                currentSession.getMap_height() / 2f,
                0
            );
            cam.update();
        }
    }

    @Override
    public void onGameOver() {
        if (!isGameOver) {
            handleGameOver();
        }
    }
}
