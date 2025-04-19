package gr17.noodleio.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

import gr17.noodleio.game.API.LobbyApi;
import gr17.noodleio.game.API.LobbyPlayerApi;
import gr17.noodleio.game.API.PlayerGameStateApi;
import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.model.PlayerResult;
import gr17.noodleio.game.states.GameStateManager;
import gr17.noodleio.game.states.MenuState;
import gr17.noodleio.game.util.ResourceManager;


public class Core extends ApplicationAdapter {

    private final EnvironmentConfig environmentConfig;

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

    private GameStateManager gsm;
    private SpriteBatch batch;

    private OrthographicCamera camera;
    private Viewport viewport;

    private static final float MIN_WORLD_WIDTH = 800;
    private static final float MIN_WORLD_HEIGHT = 480;
    private static final float MAX_WORLD_WIDTH = 1920;
    private static final float MAX_WORLD_HEIGHT = 1080;

    // Test variables
    private BitmapFont font;
    private String testStatus = "Press SPACE to test database functions\nPress W to move player up";
    private String lobbyId = null;
    private String playerId = null;
    private String sessionId = null;
    private LobbyApi lobbyApi;
    private LobbyPlayerApi lobbyPlayerApi;
    private PlayerGameStateApi playerGameStateApi;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new DynamicViewport(MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT,
            MAX_WORLD_WIDTH, MAX_WORLD_HEIGHT, camera);
        viewport.apply(true); // Apply the viewport initially

        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.5f);

        // Initialize game state manager and set initial state
        gsm = GameStateManager.getInstance();
        gsm.push(new MenuState(gsm));

        // Initialize API classes for testing
        if (environmentConfig != null) {
            lobbyApi = new LobbyApi(environmentConfig);
            lobbyPlayerApi = new LobbyPlayerApi(environmentConfig);
            playerGameStateApi = new PlayerGameStateApi(environmentConfig);
        }
    }

    @Override
    public void render() {
        // Clear screen with background color
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        // Handle test input
        handleTestInput();

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        gsm.update(Gdx.graphics.getDeltaTime());
        gsm.render(batch);

        // Draw test status
        batch.begin();
        font.draw(batch, testStatus, 20, viewport.getWorldHeight() - 20);
        batch.end();
    }

    private void handleTestInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            runDatabaseTest();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            movePlayerUp();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            movePlayerDown();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            movePlayerLeft();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            movePlayerRight();
        }
    }

    private void movePlayerUp() {
        if (environmentConfig == null) {
            testStatus = "Cannot test: Environment config is null";
            return;
        }

        if (sessionId == null || playerId == null) {
            testStatus = "Cannot move player: Create a game session first (press SPACE)";
            return;
        }

        String result = playerGameStateApi.movePlayerUp(playerId, sessionId);
        testStatus = "Move up result: " + result;
    }

    private void movePlayerDown() {
        if (environmentConfig == null) {
            testStatus = "Cannot test: Environment config is null";
            return;
        }

        if (sessionId == null || playerId == null) {
            testStatus = "Cannot move player: Create a game session first (press SPACE)";
            return;
        }

        String result = playerGameStateApi.movePlayerDown(playerId, sessionId);
        testStatus = "Move up result: " + result;
    }

    private void movePlayerLeft() {
        if (environmentConfig == null) {
            testStatus = "Cannot test: Environment config is null";
            return;
        }

        if (sessionId == null || playerId == null) {
            testStatus = "Cannot move player: Create a game session first (press SPACE)";
            return;
        }

        String result = playerGameStateApi.movePlayerLeft(playerId, sessionId);
        testStatus = "Move up result: " + result;
    }

    private void movePlayerRight() {
        if (environmentConfig == null) {
            testStatus = "Cannot test: Environment config is null";
            return;
        }

        if (sessionId == null || playerId == null) {
            testStatus = "Cannot move player: Create a game session first (press SPACE)";
            return;
        }

        String result = playerGameStateApi.movePlayerRight(playerId, sessionId);
        testStatus = "Move up result: " + result;
    }

    private void runDatabaseTest() {
        if (environmentConfig == null) {
            testStatus = "Cannot test: Environment config is null";
            return;
        }

        // Test step 1: Create a lobby with owner
        String playerName = "TestPlayer_" + System.currentTimeMillis();
        String result = lobbyApi.createLobbyWithOwner(playerName, 4);
        testStatus = "Step 1: " + result;

        // Parse the response to get lobby ID and player ID
        try {
            // Extract lobby ID from message like "Lobby created with ID: abc123 | Player 'name' added as owner with ID: xyz789"
            if (result.contains("Lobby created with ID:")) {
                String[] parts = result.split("\\|");
                if (parts.length > 0) {
                    String lobbyPart = parts[0].trim();
                    lobbyId = lobbyPart.substring(lobbyPart.lastIndexOf(":") + 1).trim();
                }

                if (parts.length > 1) {
                    String playerPart = parts[1].trim();
                    String idSection = playerPart.substring(playerPart.lastIndexOf(":") + 1).trim();
                    playerId = idSection;
                }

                testStatus += "\nExtracted Lobby ID: " + lobbyId + ", Player ID: " + playerId;

                // Test step 2: Start a game session
                if (lobbyId != null && playerId != null) {
                    String gameResult = lobbyPlayerApi.startGameSession(playerId, lobbyId);
                    testStatus += "\nStep 2: " + gameResult;

                    // Extract session ID if available
                    if (gameResult.contains("ID:")) {
                        int idIndex = gameResult.indexOf("ID:") + 3;
                        int commaIndex = gameResult.indexOf(",", idIndex);
                        if (commaIndex > idIndex) {
                            sessionId = gameResult.substring(idIndex, commaIndex).trim();
                            testStatus += "\nExtracted Session ID: " + sessionId;
                        }
                    }
                }
            }
        } catch (Exception e) {
            testStatus += "\nError parsing response: " + e.getMessage();
        }
    }

    @Override
    public void resize(int width, int height) {
        // Update viewport when screen is resized
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        // Also dispose the current state
        if (gsm != null && !gsm.isEmpty()) {
            gsm.disposeAll();
        }
    }
}
