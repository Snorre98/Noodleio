package gr17.noodleio.game.states.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import gr17.noodleio.game.states.GameStateManager;
import gr17.noodleio.game.states.MenuState;
import gr17.noodleio.game.states.State;

public abstract class BaseUIState extends State {

    protected Stage stage;
    protected Table table;
    protected UIFactory uiFactory;
    protected Label statusLabel;
    private boolean uiInitialized = false;

    public BaseUIState(GameStateManager gsm) {
        super(gsm);
        log("BaseUIState constructor");
        // Don't initialize UI here, wait until we're called to initialize
    }

    protected void initializeUIDeferred() {
        if (uiInitialized) {
            return;
        }

        log("Initializing UI State");
        // Initialize UI components in a try-catch block to handle errors gracefully
        try {
            initializeUI();
            uiInitialized = true;
        } catch (Exception e) {
            logError("Failed to initialize UI, creating minimal fallback", e);
            createFallbackUI();
        }
    }

    private void initializeUI() {
        try {
            log("Creating UI factory");
            // Create UI factory - ensure UIComponents is initialized first
            uiFactory = new UIFactory();

            log("Creating stage");
            // Create stage
            stage = uiFactory.createStage();

            log("Creating main table");
            // Create main table
            table = uiFactory.createMainTable();
            stage.addActor(table);

            log("Setting up stage as input processor");
            // Setup stage as input processor
            uiFactory.setupDefaultStage(stage);

            log("Setting up state-specific UI components");
            // Create UI components specific to this state - call setupUI only after basic setup is complete
            setupUI();

            log("UI initialization completed");
        } catch (Exception e) {
            logError("Error initializing UI", e);
            throw e; // Re-throw to be caught by outer try-catch
        }
    }

    private void createFallbackUI() {
        // Create minimal fallback UI in case of initialization failure
        stage = new Stage();
        table = new Table();
        table.setFillParent(true);
        stage.addActor(table);
        Gdx.input.setInputProcessor(stage);
        uiInitialized = true;
    }

    /**
     * Override this method to setup state-specific UI components
     */
    protected abstract void setupUI();

    @Override
    protected void handleInput() {
        // Input is handled by Scene2D stage
    }

    @Override
    public void update(float dt) {
        if (!uiInitialized) {
            initializeUIDeferred();
        }
        if (stage != null) {
            stage.act(dt);
        }
    }

    @Override
    public void render(SpriteBatch sb) {
        if (!uiInitialized) {
            initializeUIDeferred();
        }
        clearScreen();
        if (stage != null) {
            stage.draw();
        }
    }

    protected void clearScreen() {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT);
    }

    @Override
    public void dispose() {
        if (stage != null) {
            log("Disposing stage for");
            stage.dispose();
        }
    }

    protected void setStatus(String message) {
        if (statusLabel != null) {
            log("Setting status: " + message);
            statusLabel.setText(message);
        } else {
            logError("Status label is null, cannot set status: " + message);
        }
    }

    protected void returnToMenu() {
        log("Returning to menu from");
        gsm.set(new MenuState(gsm));
    }
}
