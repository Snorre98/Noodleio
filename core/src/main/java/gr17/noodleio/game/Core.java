package gr17.noodleio.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

import gr17.noodleio.game.states.GameStateManager;
import gr17.noodleio.game.states.MenuState;

public class Core extends ApplicationAdapter {
    private GameStateManager gsm;
    private SpriteBatch batch;

    private OrthographicCamera camera;
    private Viewport viewport;

    private static final float MIN_WORLD_WIDTH = 800;
    private static final float MIN_WORLD_HEIGHT = 480;
    private static final float MAX_WORLD_WIDTH = 1920;
    private static final float MAX_WORLD_HEIGHT = 1080;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new DynamicViewport(MIN_WORLD_WIDTH, MIN_WORLD_HEIGHT,
            MAX_WORLD_WIDTH, MAX_WORLD_HEIGHT, camera);
        viewport.apply(true); // Apply the viewport initially
        
        batch = new SpriteBatch();
        
        // Initialize game state manager and set initial state
        gsm = GameStateManager.getInstance();
        gsm.push(new MenuState(gsm));
        
        // Enable asset loading from internal assets directory
        Gdx.files.internal(".");
    }

    @Override
    public void render() {
        // Clear screen with background color
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        
        // Calculate delta time
        float dt = Gdx.graphics.getDeltaTime();
        
        // Update current game state
        gsm.update(dt);
        
        // Update camera and set batch projection matrix
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        
        // Render current game state
        gsm.render(batch);
    }

    @Override
    public void resize(int width, int height) {
        // Update viewport when screen is resized
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        // Also dispose the current state
        if (gsm != null && !gsm.isEmpty()) {
            gsm.disposeAll();
        }
    }
}
