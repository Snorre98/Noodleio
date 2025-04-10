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
import gr17.noodleio.game.API.TestConnectionApi;
import gr17.noodleio.game.config.EnvironmentConfig;

import gr17.noodleio.game.API.CursorRealtimeApi;

import gr17.noodleio.game.ui.components.InputFieldRenderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import java.util.HashMap;
import java.util.Map;
import gr17.noodleio.game.models.CursorPosition;
import com.badlogic.gdx.math.Vector3;

public class Core extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;

    private InputFieldRenderer inputRenderer;

    private InputMultiplexer inputMultiplexer;

    private OrthographicCamera camera;
    private Viewport viewport;

    private BitmapFont testText;
    private String statusMessage = "Initializing...";
    private String leaderboardMessage = "";
    private String addEntryMessage = "";
    private String lobbyMessage = "";
    private String playerMessage = "";

    // Environment config
    private final EnvironmentConfig environmentConfig;

    // API classes
    private TestConnectionApi testConnectionApi;
    private LeaderboardApi leaderboardApi;
    private LobbyApi lobbyApi;

    // Add these as fields in your Core class
    private CursorRealtimeApi cursorRealtimeApi;
    private String cursorStatusMessage = "";
    private Map<String, CursorPosition> otherCursors = new HashMap<>();
    private Texture cursorTexture;
    private InputProcessor inputProcessor;

    private Vector3 tempVec = new Vector3();

    private static final float MIN_WORLD_WIDTH = 800;
    private static final float MIN_WORLD_HEIGHT = 480;
    private static final float MAX_WORLD_WIDTH = 1920;
    private static final float MAX_WORLD_HEIGHT = 1080;

    // Default constructor
    public Core() {
        this(null);
    }

    // Constructor with environment config
    public Core(EnvironmentConfig environmentConfig) {
        this.environmentConfig = environmentConfig;
    }

    private void setupInputProcessor() {
        inputProcessor = new InputAdapter() {
            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                // Convert screen coordinates to world coordinates
                camera.unproject(tempVec.set(screenX, screenY, 0));
                float worldX = tempVec.x;
                float worldY = tempVec.y;

                // Send cursor position over realtime API
                cursorRealtimeApi.sendCursorPosition(worldX, worldY);

                return false; // Return false to allow stage to also process the event
            }
        };

        // Create and configure the input multiplexer
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(inputRenderer.getStage()); // Stage first
        inputMultiplexer.addProcessor(inputProcessor); // Then cursor processor

        // Set the multiplexer as the input processor
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    @Override
    public void create() {
        // Initialize LibGDX components
        camera = new OrthographicCamera();
        viewport = new DynamicViewport(MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT,
            MAX_WORLD_WIDTH, MAX_WORLD_HEIGHT, camera);
        viewport.apply(true);
        testText = new BitmapFont();
        batch = new SpriteBatch();

        // Load main image
        image = new Texture("libgdx.png");

        // Create a default cursor texture (1x1 white pixel)
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 0, 0, 1); // Red color for visibility
        pixmap.fill();
        cursorTexture = new Texture(pixmap);
        pixmap.dispose();

        inputRenderer = new InputFieldRenderer("Enter text here...", "Type something...");
        inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(inputRenderer.getStage()); // Use the stage from the renderer
        Gdx.input.setInputProcessor(inputRenderer.getStage()); // Set stage as input processor by default

        // Try to load the cursor texture, but only if it exists
        try {
            if (Gdx.files.internal("cursor.png").exists()) {
                // Dispose the default texture first
                cursorTexture.dispose();
                // Then load the real one
                cursorTexture = new Texture("cursor.png");
            }
        } catch (Exception e) {
            Gdx.app.log("Core", "Using default cursor texture: " + e.getMessage());
            // We already have a fallback texture, so no need to handle this further
        }

        // Initialize services if config is provided
        if (environmentConfig != null) {
            try {
                // Create API classes
                testConnectionApi = new TestConnectionApi(environmentConfig);
                leaderboardApi = new LeaderboardApi(environmentConfig);
                cursorRealtimeApi = new CursorRealtimeApi(environmentConfig);
                lobbyApi = new LobbyApi(environmentConfig);

                // Test Supabase connection
                statusMessage = testConnectionApi.testSupabaseConnection();

                // Add a test entry to the leaderboard
                addEntryMessage = leaderboardApi.addTestLeaderboardEntry();

                // Fetch and display the leaderboard
                leaderboardMessage = leaderboardApi.fetchLeaderboard(5);

                // Create a new lobby with a test player as owner
                String combinedMessage = lobbyApi.createTestLobbyWithOwner();

                // Split the combined message into lobby and player parts if it contains a separator
                if (combinedMessage.contains(" | ")) {
                    String[] parts = combinedMessage.split(" \\| ");
                    lobbyMessage = parts[0];
                    playerMessage = parts[1];
                } else {
                    // Otherwise just use the whole message for lobby
                    lobbyMessage = combinedMessage;
                }

                // Connect to the cursor channel
                cursorStatusMessage = cursorRealtimeApi.connect("cursor-room-" + System.currentTimeMillis() % 1000);

                // Set up input processor to track mouse movements
                setupInputProcessor();
            } catch (Exception e) {
                statusMessage = "Failed to connect: " + e.getMessage();
                e.printStackTrace();
            }
        } else {
            statusMessage = "No Supabase config provided";
        }
    }

    @Override
    public void render() {
        // Clear the screen
        ScreenUtils.clear(0.2f, 0.2f, 0.2f, 1);

        // Update camera
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        float x = (viewport.getWorldWidth() - image.getWidth()) / 2;
        float y = (viewport.getWorldHeight() - image.getHeight()) / 2;
        batch.draw(image, x, y);

        // Display Supabase connection status
        testText.draw(batch, statusMessage, x, y - 30);

        // Display add entry status
        testText.draw(batch, addEntryMessage, x, y - 60);

        // Display leaderboard data
        testText.draw(batch, leaderboardMessage, x, y - 90);

        // Display lobby creation status
        testText.draw(batch, lobbyMessage, x, y - 150);

        // Display player status
        testText.draw(batch, playerMessage, x, y - 180);

        // Display cursor status
        testText.draw(batch, cursorStatusMessage, x, y - 210);

        // Draw other users' cursors
        drawOtherCursors();

        batch.end();

        inputRenderer.render(Gdx.graphics.getDeltaTime());
    }

    private void drawOtherCursors() {
        // Skip drawing if we're not connected
        if (cursorRealtimeApi == null || !cursorRealtimeApi.isConnected()) {
            return;
        }

        // Get the current user ID so we don't draw our own cursor
        String currentUserId = cursorRealtimeApi.getUserId();

        // In a real implementation, you'd get the cursor positions from the realtime service
        // This is a placeholder until we modify the CursorRealtimeApi to expose the positions
        Map<String, CursorPosition> cursors = getLatestCursorPositions();

        for (Map.Entry<String, CursorPosition> entry : cursors.entrySet()) {
            // Skip our own cursor
            if (entry.getKey().equals(currentUserId)) {
                continue;
            }

            CursorPosition position = entry.getValue();

            // Draw a simple cursor representation (10x10 square)
            batch.draw(cursorTexture, position.getX() - 5, position.getY() - 5, 10, 10);

            // Draw the user ID near the cursor
            testText.draw(batch, entry.getKey(), position.getX() + 10, position.getY());
        }
    }

    // This method fetches the latest cursor positions
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
        batch.dispose();
        image.dispose();
        testText.dispose();
        cursorTexture.dispose();
        inputRenderer.dispose();

        // Disconnect from the cursor channel
        if (cursorRealtimeApi != null && cursorRealtimeApi.isConnected()) {
            cursorRealtimeApi.disconnect();
        }
    }

    // Accessor for TestConnectionApi
    public TestConnectionApi getTestConnectionApi() {
        return testConnectionApi;
    }

    // Accessor for LeaderboardApi
    public LeaderboardApi getLeaderboardApi() {
        return leaderboardApi;
    }

    // Accessor for LobbyApi
    public LobbyApi getLobbyApi() {
        return lobbyApi;
    }
}
