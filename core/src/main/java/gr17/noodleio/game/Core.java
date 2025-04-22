package gr17.noodleio.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.states.GameStateManager;
import gr17.noodleio.game.states.MenuState;

public class Core extends ApplicationAdapter {

    public Core() {
        this(null);
    }

    public Core(EnvironmentConfig environmentConfig) {
    }

    private GameStateManager gsm;
    private SpriteBatch batch;

    private OrthographicCamera camera;
    private Viewport viewport;

    // Your virtual screen size (cropped area, logical units)
    private static final float VIRTUAL_WIDTH = 375;
    private static final float VIRTUAL_HEIGHT = 667;

    private BitmapFont font;

    @Override
    public void create() {
        Gdx.graphics.setForegroundFPS(60);
        Gdx.graphics.setVSync(true);

        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        viewport.apply(true);

        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.5f);

        gsm = GameStateManager.getInstance();
        gsm.push(new MenuState(gsm));
    }

    @Override
    public void render() {
        // Clear the screen with a background color (also shows black bars when needed)
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float deltaTime = Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        gsm.update(deltaTime);
        gsm.render(batch);

        batch.begin();
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, VIRTUAL_HEIGHT - 10);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        if (gsm != null && !gsm.isEmpty()) {
            gsm.disposeAll();
        }
    }
}
