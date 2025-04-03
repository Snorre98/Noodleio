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
        GameStateManager result = instance;
        if (result == null) {
            synchronized (GameStateManager.class) {
                result = instance;
                if (result == null) {
                    instance = result = new GameStateManager();
                }
            }
        }
        return result;
    }

    public void push(State state) {
        states.push(state);
    }

    public void pop() {
        if (!states.isEmpty()) {
            states.peek().dispose();
            states.pop();
        }
    }

    public void set(State state) {
        if (!states.isEmpty()) {
            states.peek().dispose();
            states.pop();
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
