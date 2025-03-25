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
        states.pop();
    }

    public void set(State state) {
        states.pop();
        states.push(state);
    }

    public void update(float dt) {
        states.peek().update(dt);
    }

    public void render(SpriteBatch sb) {
        states.peek().render(sb);
    }

}
