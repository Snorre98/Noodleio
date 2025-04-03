package gr17.noodleio.game.states;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import gr17.noodleio.game.models.Lobby;

public class PlayState extends State {

    public PlayState(GameStateManager gsm, Lobby lobby) {
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
