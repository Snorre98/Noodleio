package gr17.noodleio.game.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

public abstract class State {
    // Global debug logging flags for all states
    protected static final boolean DEBUG_LOGGING_INFO = true;
    protected static final boolean DEBUG_LOGGING_ERROR = true;
    protected static final boolean DEBUG_LOGGING_ERROR_WITH_EXCEPTION = true;

    protected OrthographicCamera cam;
    protected Vector3 mouse;
    protected GameStateManager gsm;

    protected State(GameStateManager gsm) {
        this.gsm = gsm;
        cam = new OrthographicCamera();
        mouse = new Vector3();
    }

    /**
     * Logs a message using LibGDX logging if info logging is enabled
     * Each state will have its own tag
     */
    protected void log(String message) {
        if (DEBUG_LOGGING_INFO) {
            Gdx.app.log(getLoggingTag(), message);
        }
    }

    /**
     * Logs a message with local logging flag
     * If global flag is true, always logs
     * If global flag is false, only logs if localFlag is true
     */
    protected void log(String message, boolean localFlag) {
        if (DEBUG_LOGGING_INFO || localFlag) {
            Gdx.app.log(getLoggingTag(), message);
        }
    }

    /**
     * Logs an error message with exception if error logging with exceptions is enabled
     */
    protected void logError(String message, Throwable e) {
        if (DEBUG_LOGGING_ERROR_WITH_EXCEPTION) {
            Gdx.app.error(getLoggingTag(), message, e);
        }
    }

    /**
     * Logs an error message with exception with local logging flag
     * If global flag is true, always logs
     * If global flag is false, only logs if localFlag is true
     */
    protected void logError(String message, Throwable e, boolean localFlag) {
        if (DEBUG_LOGGING_ERROR_WITH_EXCEPTION || localFlag) {
            Gdx.app.error(getLoggingTag(), message, e);
        }
    }

    /**
     * Logs an error message without exception if error logging is enabled
     */
    protected void logError(String message) {
        if (DEBUG_LOGGING_ERROR) {
            Gdx.app.error(getLoggingTag(), message);
        }
    }

    /**
     * Logs an error message without exception with local logging flag
     * If global flag is true, always logs
     * If global flag is false, only logs if localFlag is true
     */
    protected void logError(String message, boolean localFlag) {
        if (DEBUG_LOGGING_ERROR || localFlag) {
            Gdx.app.error(getLoggingTag(), message);
        }
    }

    /**
     * Gets the logging tag for this state
     * Default implementation uses the class simple name
     * Override in subclasses if you want a different tag
     */
    protected String getLoggingTag() {
        return this.getClass().getSimpleName();
    }

    protected abstract void handleInput();

    public abstract void update(float dt);

    public abstract void render(SpriteBatch sb);

    public abstract void dispose();
}
