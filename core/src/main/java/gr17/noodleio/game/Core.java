package gr17.noodleio.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.models.LeaderboardEntry;
import gr17.noodleio.game.services.LeaderboardService;
import gr17.noodleio.game.services.ServiceManager;

import java.util.List;
import java.util.Random;

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

    // Service manager
    private ServiceManager serviceManager;

    // API service
    private LeaderboardService api;

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
                // Create the service manager
                serviceManager = new ServiceManager(environmentConfig);

                // Create the API service
                api = new LeaderboardService(environmentConfig);

                // Test Supabase connection
                statusMessage = api.testSupabaseConnection();

                // Add a test entry to the leaderboard
                addTestLeaderboardEntry();

                // Fetch and display the leaderboard
                fetchLeaderboard();
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

    private void addTestLeaderboardEntry() {
        if (api != null) {
            try {
                // Generate a random score between 100 and 10000
                Random random = new Random();
                int randomScore = random.nextInt(9901) + 100; // Random score between 100 and 10000

                // Generate a player name with a timestamp to make it unique
                String playerName = "TestPlayer_" + System.currentTimeMillis();

                // Add the entry
                LeaderboardEntry newEntry = api.addLeaderboardEntry(playerName, randomScore, null);

                if (newEntry != null) {
                    addEntryMessage = "Added new entry: " + playerName + " with score " + randomScore;
                } else {
                    addEntryMessage = "Failed to add new leaderboard entry";
                }
            } catch (Exception e) {
                addEntryMessage = "Error adding leaderboard entry: " + e.getMessage();
                e.printStackTrace();
            }
        }
    }

    private void fetchLeaderboard() {
        if (api != null) {
            try {
                // Get top 5 leaderboard entries
                List<LeaderboardEntry> topEntries = api.getTopLeaderboard(5);

                // Format a message to display
                StringBuilder sb = new StringBuilder("Top 5 Players:\n");

                if (topEntries.isEmpty()) {
                    sb.append("No entries found");
                } else {
                    for (LeaderboardEntry entry : topEntries) {
                        sb.append(entry.getPlayer_name())
                            .append(": ")
                            .append(entry.getScore())
                            .append("\n");
                    }
                }

                leaderboardMessage = sb.toString();
            } catch (Exception e) {
                leaderboardMessage = "Failed to load leaderboard: " + e.getMessage();
                e.printStackTrace();
            }
        }
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

    // Accessor for ServiceManager
    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    // Accessor for Api
    public LeaderboardService getApi() {
        return api;
    }
}
