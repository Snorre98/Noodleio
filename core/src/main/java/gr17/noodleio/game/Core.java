package gr17.noodleio.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

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


//        ResourceManager rm = new ResourceManager();
//        rm.load(); //Laster inn font fra rm
//        //Mock data
//        Array<PlayerResult> results = new Array<>();
//        results.add(new gr17.noodleio.game.model.PlayerResult("Per", 120));
//        results.add(new gr17.noodleio.game.model.PlayerResult("Paal", 90));
//        results.add(new gr17.noodleio.game.model.PlayerResult("Espen", 70));
//        results.add(new gr17.noodleio.game.model.PlayerResult("You", 65));
//        results.add(new gr17.noodleio.game.model.PlayerResult("Askeladd", 40));
//
//        String playerName = "Magnus";
//        int placement = 4;
//
//        gsm.push(new gr17.noodleio.game.states.EndGameState(gsm, results, playerName, placement, rm));


    }

    @Override
    public void render() {
        // Clear screen with background color
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        gsm.update(Gdx.graphics.getDeltaTime());
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
