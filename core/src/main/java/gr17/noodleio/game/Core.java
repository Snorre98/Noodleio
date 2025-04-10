package gr17.noodleio.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

import gr17.noodleio.game.API.LeaderboardApi;
import gr17.noodleio.game.API.LobbyApi;
import gr17.noodleio.game.API.LobbyPlayerApi;
import gr17.noodleio.game.API.TestConnectionApi;
import gr17.noodleio.game.API.CursorRealtimeApi;
import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.models.CursorPosition;
import gr17.noodleio.game.ui.components.InputFieldRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import java.util.HashMap;
import java.util.Map;
import com.badlogic.gdx.math.Vector3;

/**
 * Main application class that handles the game's lifecycle, rendering, and input.
 */
public class Core extends ApplicationAdapter {
    // LibGDX rendering components
    private SpriteBatch batch;
    private Texture image;
    private BitmapFont testText;
    private OrthographicCamera camera;
    private Viewport viewport;

    // Input handling
    private InputFieldRenderer inputRenderer;
    private InputMultiplexer inputMultiplexer;
    private InputProcessor inputProcessor;
    private Vector3 tempVec = new Vector3();

    // Status messages
    private String statusMessage = "Initializing...";
    private String leaderboardMessage = "";
    private String addEntryMessage = "";
    private String lobbyMessage = "";
    private String playerMessage = "";
    private String cursorStatusMessage = "";

    // API services
    private final EnvironmentConfig environmentConfig;
    private TestConnectionApi testConnectionApi;
    private LeaderboardApi leaderboardApi;
    private LobbyApi lobbyApi;
    private LobbyPlayerApi lobbyPlayerApi;
    private CursorRealtimeApi cursorRealtimeApi;

    // Cursor tracking
    private Map<String, CursorPosition> otherCursors = new HashMap<>();
    private Texture cursorTexture;

    // Constants for viewport dimensions
    private static final float MIN_WORLD_WIDTH = 800;
    private static final float MIN_WORLD_HEIGHT = 480;
    private static final float MAX_WORLD_WIDTH = 1920;
    private static final float MAX_WORLD_HEIGHT = 1080;

    /**
     * Default constructor - uses null environment config.
     */
    public Core() {
        this(null);
    }

    /**
     * Constructor with environment configuration.
     * @param environmentConfig The application's environment configuration
     */
    public Core(EnvironmentConfig environmentConfig) {
        this.environmentConfig = environmentConfig;
    }

    @Override
    public void create() {
        initializeGraphics();
        initializeInput();
        initializeServices();
    }

    /**
     * Initializes graphics-related components.
     */
    private void initializeGraphics() {
        // Camera and viewport setup
        camera = new OrthographicCamera();
        viewport = new DynamicViewport(MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT,
            MAX_WORLD_WIDTH, MAX_WORLD_HEIGHT, camera);
        viewport.apply(true);

        // Drawing components
        testText = new BitmapFont();
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");

        // Create and initialize cursor texture
        initializeCursorTexture();
    }

    /**
     * Creates and initializes the cursor texture.
     */
    private void initializeCursorTexture() {
        // Create a default cursor texture (red pixel)
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 0, 0, 1);
        pixmap.fill();
        cursorTexture = new Texture(pixmap);
        pixmap.dispose();

        // Try to load custom cursor texture if available
        try {
            if (Gdx.files.internal("cursor.png").exists()) {
                cursorTexture.dispose();
                cursorTexture = new Texture("cursor.png");
            }
        } catch (Exception e) {
            Gdx.app.log("Core", "Using default cursor texture: " + e.getMessage());
        }
    }

    /**
     * Initializes input handling components.
     */
    private void initializeInput() {
        // Create input field renderer
        inputRenderer = new InputFieldRenderer("Enter text here...", "Type something...");

        // Setup initial input processor
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(inputRenderer.getStage());
        Gdx.input.setInputProcessor(inputRenderer.getStage());
    }

    /**
     * Sets up the input processor for cursor tracking.
     */
    private void setupInputProcessor() {
        inputProcessor = new InputAdapter() {
            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                // Convert screen coordinates to world coordinates
                camera.unproject(tempVec.set(screenX, screenY, 0));
                float worldX = tempVec.x;
                float worldY = tempVec.y;

                // Send cursor position over realtime API
                if (cursorRealtimeApi != null && cursorRealtimeApi.isConnected()) {
                    cursorRealtimeApi.sendCursorPosition(worldX, worldY);
                }

                return false; // Allow stage to also process the event
            }
        };

        // Configure input multiplexer with all processors
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(inputRenderer.getStage());
        inputMultiplexer.addProcessor(inputProcessor);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    /**
     * Initializes backend services and APIs.
     */
    private void initializeServices() {
        if (environmentConfig == null) {
            statusMessage = "No Supabase config provided";
            return;
        }

        try {
            // Create and initialize all API classes
            initializeApiClasses();

            // Run test operations
            runTestOperations();

            // Set up input processor for cursor tracking
            setupInputProcessor();
        } catch (Exception e) {
            statusMessage = "Failed to connect: " + e.getMessage();
            e.printStackTrace();
        }
    }

    /**
     * Initializes all API service classes.
     */
    private void initializeApiClasses() {
        testConnectionApi = new TestConnectionApi(environmentConfig);
        leaderboardApi = new LeaderboardApi(environmentConfig);
        cursorRealtimeApi = new CursorRealtimeApi(environmentConfig);
        lobbyApi = new LobbyApi(environmentConfig);
        lobbyPlayerApi = new LobbyPlayerApi(environmentConfig);
    }

    /**
     * Runs test operations for each service.
     */
    private void runTestOperations() {
        // Test Supabase connection
        statusMessage = testConnectionApi.testSupabaseConnection();

        // Test leaderboard functionality
        testLeaderboardOperations();

        // Test lobby operations
        testLobbyOperations();

        // Connect to cursor tracking channel
        testCursorOperations();
    }

    /**
     * Tests leaderboard operations.
     */
    private void testLeaderboardOperations() {
        // Add a test entry to the leaderboard
        addEntryMessage = leaderboardApi.addTestLeaderboardEntry();

        // Fetch and display the leaderboard
        leaderboardMessage = leaderboardApi.fetchLeaderboard(5);
    }

    /**
     * Tests lobby operations.
     */
    private void testLobbyOperations() {
        // Create a test lobby with owner
        String combinedMessage = lobbyApi.createTestLobbyWithOwner();

        // Parse and display the lobby message
        parseLobbyCreationMessage(combinedMessage);
    }

    /**
     * Parses the lobby creation message and updates status variables.
     */
    private void parseLobbyCreationMessage(String combinedMessage) {
        // Split the combined message if it contains the separator
        if (combinedMessage.contains(" | ")) {
            String[] parts = combinedMessage.split(" \\| ");
            lobbyMessage = parts[0];
            playerMessage = parts[1];
        } else {
            lobbyMessage = combinedMessage;
        }
    }

    /**
     * Tests cursor realtime operations.
     */
    private void testCursorOperations() {
        // Generate a unique channel name with timestamp
        String channelName = "cursor-room-" + System.currentTimeMillis() % 1000;

        // Connect to the cursor channel
        cursorStatusMessage = cursorRealtimeApi.connect(channelName);
    }

    @Override
    public void render() {
        // Clear the screen
        ScreenUtils.clear(0.2f, 0.2f, 0.2f, 1);

        // Update camera and render scene
        updateCamera();
        renderScene();

        // Render UI components
        inputRenderer.render(Gdx.graphics.getDeltaTime());
    }

    /**
     * Updates the camera for rendering.
     */
    private void updateCamera() {
        camera.update();
        batch.setProjectionMatrix(camera.combined);
    }

    /**
     * Renders the main scene including status information.
     */
    private void renderScene() {
        batch.begin();

        // Calculate center position
        float x = (viewport.getWorldWidth() - image.getWidth()) / 2;
        float y = (viewport.getWorldHeight() - image.getHeight()) / 2;

        // Draw main image
        batch.draw(image, x, y);

        // Draw status messages
        renderStatusMessages(x, y);

        // Draw other users' cursors
        drawOtherCursors();

        batch.end();
    }

    /**
     * Renders status messages on screen.
     */
    private void renderStatusMessages(float x, float y) {
        testText.draw(batch, statusMessage, x, y - 30);
        testText.draw(batch, addEntryMessage, x, y - 60);
        testText.draw(batch, leaderboardMessage, x, y - 90);
        testText.draw(batch, lobbyMessage, x, y - 150);
        testText.draw(batch, playerMessage, x, y - 180);
        testText.draw(batch, cursorStatusMessage, x, y - 210);
    }

    /**
     * Draws cursors of other connected users.
     */
    private void drawOtherCursors() {
        // Skip drawing if we're not connected
        if (cursorRealtimeApi == null || !cursorRealtimeApi.isConnected()) {
            return;
        }

        // Get the current user ID so we don't draw our own cursor
        String currentUserId = cursorRealtimeApi.getUserId();

        // Get latest cursor positions from realtime service
        Map<String, CursorPosition> cursors = getLatestCursorPositions();

        // Draw each remote cursor
        for (Map.Entry<String, CursorPosition> entry : cursors.entrySet()) {
            // Skip our own cursor
            if (entry.getKey().equals(currentUserId)) {
                continue;
            }

            CursorPosition position = entry.getValue();

            // Draw cursor representation
            batch.draw(cursorTexture, position.getX() - 5, position.getY() - 5, 10, 10);

            // Draw user ID label
            testText.draw(batch, entry.getKey(), position.getX() + 10, position.getY());
        }
    }

    /**
     * Gets the latest cursor positions from the realtime service.
     */
    private Map<String, CursorPosition> getLatestCursorPositions() {
        if (cursorRealtimeApi != null) {
            return cursorRealtimeApi.getCursorPositions();
        }
        return otherCursors; // Fallback to empty map
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        inputRenderer.resize(width, height);
    }

    @Override
    public void dispose() {
        // Dispose graphics resources
        batch.dispose();
        image.dispose();
        testText.dispose();
        cursorTexture.dispose();
        inputRenderer.dispose();

        // Disconnect from services
        if (cursorRealtimeApi != null && cursorRealtimeApi.isConnected()) {
            cursorRealtimeApi.disconnect();
        }
    }

    // Accessor methods for API services

    /**
     * Gets the test connection API.
     */
    public TestConnectionApi getTestConnectionApi() {
        return testConnectionApi;
    }

    /**
     * Gets the leaderboard API.
     */
    public LeaderboardApi getLeaderboardApi() {
        return leaderboardApi;
    }

    /**
     * Gets the lobby API.
     */
    public LobbyApi getLobbyApi() {
        return lobbyApi;
    }

    /**
     * Gets the lobby player API.
     */
    public LobbyPlayerApi getLobbyPlayerApi() {
        return lobbyPlayerApi;
    }
}
