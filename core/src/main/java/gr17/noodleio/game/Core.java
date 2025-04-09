package gr17.noodleio.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

import gr17.noodleio.game.API.LeaderboardApi;
import gr17.noodleio.game.API.TestConnectionApi;
import gr17.noodleio.game.config.EnvironmentConfig;

public class Core extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;

    private OrthographicCamera camera;
    private Viewport viewport;

    private BitmapFont testText;
    private String statusMessage = "Initializing...";
    private String leaderboardMessage = "";
    private String addEntryMessage = "";

    // Environment config
    private final EnvironmentConfig environmentConfig;

    // API classes
    private TestConnectionApi testConnectionApi;
    private LeaderboardApi leaderboardApi;

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

    @Override
    public void create() {
        // Initialize services if config is provided
        if (environmentConfig != null) {
            try {
                // Create API classes
                testConnectionApi = new TestConnectionApi(environmentConfig);
                leaderboardApi = new LeaderboardApi(environmentConfig);

                // Test Supabase connection
                statusMessage = testConnectionApi.testSupabaseConnection();

                // Add a test entry to the leaderboard
                addEntryMessage = leaderboardApi.addTestLeaderboardEntry();

                // Fetch and display the leaderboard
                leaderboardMessage = leaderboardApi.fetchLeaderboard(5);
            } catch (Exception e) {
                statusMessage = "Failed to connect: " + e.getMessage();
                e.printStackTrace();
            }
        } else {
            statusMessage = "No Supabase config provided";
        }

        // Initialize LibGDX components
        camera = new OrthographicCamera();
        viewport = new DynamicViewport(MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT,
            MAX_WORLD_WIDTH, MAX_WORLD_HEIGHT, camera);
        viewport.apply(true);
        testText = new BitmapFont();
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

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

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
        testText.dispose();
    }

    // Accessor for TestConnectionApi
    public TestConnectionApi getTestConnectionApi() {
        return testConnectionApi;
    }

    // Accessor for LeaderboardApi
    public LeaderboardApi getLeaderboardApi() {
        return leaderboardApi;
    }
}
