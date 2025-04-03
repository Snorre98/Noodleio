package gr17.noodleio.game.views;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import java.util.List;

import gr17.noodleio.game.models.Player;

public class LobbyView {
    // Functional interfaces for callbacks
    public interface ButtonCallback {
        void execute();
    }

    private Stage stage;
    private BitmapFont font;
    private Label.LabelStyle labelStyle;
    private TextButton.TextButtonStyle buttonStyle;
    private Table playerListTable;
    private Button startButton;

    // Controller callbacks
    private ButtonCallback onBackClicked;
    private ButtonCallback onStartClicked;

    public LobbyView(ButtonCallback onBackClicked, ButtonCallback onStartClicked, boolean isHost) {
        this.onBackClicked = onBackClicked;
        this.onStartClicked = onStartClicked;

        font = new BitmapFont();
        font.getData().setScale(1.5f);

        labelStyle = new Label.LabelStyle(font, Color.WHITE);

        buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        createUI(isHost);
    }

    private void createUI(boolean isHost) {
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.pad(20);

        Label titleLabel = new Label("Noodle.io Lobby", labelStyle);
        titleLabel.setAlignment(Align.center);
        mainTable.add(titleLabel).expandX().colspan(2).padBottom(20);
        mainTable.row();

        playerListTable = new Table();

        ScrollPane scrollPane = new ScrollPane(playerListTable);
        scrollPane.setScrollingDisabled(true, false);

        mainTable.add(scrollPane).expand().fill().colspan(2).padBottom(20);
        mainTable.row();

        Button backButton = new TextButton("Back", buttonStyle);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBackClicked.execute();
            }
        });

        startButton = new TextButton("Start Game", buttonStyle);
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onStartClicked.execute();
            }
        });

        startButton.setDisabled(!isHost);

        mainTable.add(backButton).expandX().align(Align.left);
        mainTable.add(startButton).expandX().align(Align.right);

        stage.addActor(mainTable);
    }

    public void updatePlayerList(List<Player> players, boolean isHost) {
        playerListTable.clear();

        Label nameHeader = new Label("Player Name", labelStyle);
        Label statusHeader = new Label("Status", labelStyle);

        playerListTable.add(nameHeader).expandX().align(Align.left).padRight(50);
        playerListTable.add(statusHeader).expandX().align(Align.left);
        playerListTable.row();

        for (Player player : players) {
            Label nameLabel = new Label(player.getName(), labelStyle);
            Label statusLabel = new Label(player.isHost() ? "Host" : "Ready", labelStyle);

            playerListTable.add(nameLabel).expandX().align(Align.left).padRight(50);
            playerListTable.add(statusLabel).expandX().align(Align.left);
            playerListTable.row();
        }

        startButton.setDisabled(!isHost);
    }

    public void update(float dt) {
        stage.act(dt);
    }

    public void render() {
        stage.draw();
    }

    public void dispose() {
        stage.dispose();
        font.dispose();
    }
}
