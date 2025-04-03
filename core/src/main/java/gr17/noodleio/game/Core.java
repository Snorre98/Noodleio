package gr17.noodleio.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

import gr17.noodleio.game.model.PlayerResult;
import gr17.noodleio.game.states.GameStateManager;
import gr17.noodleio.game.states.MenuState;
import gr17.noodleio.game.util.ResourceManager;

public class Core extends ApplicationAdapter {
    private GameStateManager gsm;
    private SpriteBatch batch;
    private Texture image;

    private OrthographicCamera camera;
    private Viewport viewport;

    private BitmapFont testText;

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
        testText = new BitmapFont(); // This creates the default font
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");
        gsm = GameStateManager.getInstance();
        gsm.push(new MenuState(gsm));

    }

    @Override
    public void render() {
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
        image.dispose();
        testText.dispose();
    }
}
