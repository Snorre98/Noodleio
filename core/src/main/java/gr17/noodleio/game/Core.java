package gr17.noodleio.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.services.ServiceManager;

// Import any other service classes you create

public class Core extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;

    private OrthographicCamera camera;
    private Viewport viewport;

    private BitmapFont testText;
    private String statusMessage = "Initializing...";

    // Environment config
    private final EnvironmentConfig environmentConfig;

    // Service manager
    private ServiceManager serviceManager;

    // Game-specific services that use Supabase
    //private UserService userService;

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

                // Test Supabase connection
                testSupabaseConnection();

                statusMessage = "Connected to Supabase!";
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

    private void testSupabaseConnection() {
        // Example: Access Supabase services
        if (serviceManager != null) {
            // This is just to verify the client initialization works
            // The first access to any service will trigger the lazy initialization
            try {
                // Just accessing the service property will initialize the client
                Object auth = serviceManager.getAuth();
                System.out.println("Supabase auth service initialized successfully");
            } catch (Exception e) {
                System.err.println("Error initializing Supabase: " + e.getMessage());
                throw e;
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

}
