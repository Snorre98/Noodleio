package gr17.noodleio.game.states;

import java.util.Stack;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

// Singleton game state manager
public class GameStateManager {

    private static GameStateManager instance;
    private final Stack<State> states;

    private GameStateManager() {
        states = new Stack<>();
    }
    
    public boolean isEmpty() {
        return states.isEmpty();
    }

    public static GameStateManager getInstance() {
        if (instance == null) {
            instance = new GameStateManager();
        }
        return instance;
    }

    public void push(State state) {
        states.push(state);
    }

    public void pop() {
        State state = states.pop();
        state.dispose(); // Make sure to dispose the state being removed
    }

    public void set(State state) {
        if (!states.isEmpty()) {
            State oldState = states.pop();
            oldState.dispose(); // Make sure to dispose the old state
        }
        states.push(state);
    }

    public void update(float dt) {
        if (!states.isEmpty()) {
            states.peek().update(dt);
        }
    }

    public void render(SpriteBatch sb) {
        if (!states.isEmpty()) {
            states.peek().render(sb);
        }
    }
    
    public void disposeAll() {
        while (!states.isEmpty()) {
            State state = states.pop();
            state.dispose();
        }
    }

}
