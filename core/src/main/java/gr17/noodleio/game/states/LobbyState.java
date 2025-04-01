package gr17.noodleio.game.states;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class LobbyState extends State {

    public LobbyState(GameStateManager gsm) {
        super(gsm);
    }

    @Override
    protected void handleInput() {
    }

    @Override
    public void update(float dt) {
        handleInput();
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.begin();
        sb.end();
    }

    @Override
    public void dispose() {
    }
}
