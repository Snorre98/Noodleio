package gr17.noodleio.game.states;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import gr17.noodleio.game.models.Lobby;
import gr17.noodleio.game.models.Player;
import gr17.noodleio.game.views.LobbyView;

public class LobbyState extends State {

    private Lobby lobby;
    private LobbyView view;

    private Player currentPlayer;

    public LobbyState(GameStateManager gsm, Lobby lobby, String playerName) {
        super(gsm);

        this.lobby = lobby;

        currentPlayer = new Player(playerName, true);
        lobby.addPlayer(currentPlayer);

        lobby.addPlayer(new Player("Player2", false));
        lobby.addPlayer(new Player("Player3", false));

        view = new LobbyView(
            this::onBackButtonClicked,
            this::onStartGameClicked,
            isCurrentPlayerHost()
        );

        updateView();
    }

    public void onBackButtonClicked() {
        gsm.set(new MenuState(gsm));
    }

    public void onStartGameClicked() {
        if (isCurrentPlayerHost()) {
            gsm.set(new PlayState(gsm, lobby));
        }
    }

    public boolean isCurrentPlayerHost() {
        return currentPlayer.isHost();
    }

    private void updateView() {
        view.updatePlayerList(lobby.getPlayers(), isCurrentPlayerHost());
    }

    @Override
    protected void handleInput() {
    }

    @Override
    public void update(float dt) {
        view.update(dt);
    }

    @Override
    public void render(SpriteBatch sb) {
        view.render();
    }

    @Override
    public void dispose() {
        view.dispose();
    }
}
