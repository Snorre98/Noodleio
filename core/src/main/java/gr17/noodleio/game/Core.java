package gr17.noodleio.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.Viewport;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.states.GameStateManager;
import gr17.noodleio.game.states.MenuState;

public class Core extends ApplicationAdapter {

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
    }

    private GameStateManager gsm;
    private SpriteBatch batch;

    private OrthographicCamera camera;
    private Viewport viewport;

    private static final float MIN_WORLD_WIDTH = 800;
    private static final float MIN_WORLD_HEIGHT = 480;
    private static final float MAX_WORLD_WIDTH = 1920;
    private static final float MAX_WORLD_HEIGHT = 1080;
    private BitmapFont font;

    @Override
    public void create() {
        // Set target framerate for better performance
        Gdx.graphics.setForegroundFPS(60);

        // Enable VSync if supported by the hardware
        Gdx.graphics.setVSync(true);

        // Existing code continues...
        camera = new OrthographicCamera();
        viewport = new DynamicViewport(MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT,
            MAX_WORLD_WIDTH, MAX_WORLD_HEIGHT, camera);
        viewport.apply(true);

        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.5f);

        // Initialize game state manager and set initial state
        gsm = GameStateManager.getInstance();
        gsm.push(new MenuState(gsm));
    }

    // In the render() method, add some FPS monitoring, but using glClear instead of ScreenUtils:
    @Override
    public void render() {
        // Clear screen with background color
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Cap delta time to prevent physics issues on slow frames
        float deltaTime = Math.min(Gdx.graphics.getDeltaTime(), 1/30f);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        // Update game with capped delta time
        gsm.update(deltaTime);
        gsm.render(batch);

        // Show FPS counter for debugging
        batch.begin();
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, Gdx.graphics.getHeight() - 10);
        batch.end();
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
