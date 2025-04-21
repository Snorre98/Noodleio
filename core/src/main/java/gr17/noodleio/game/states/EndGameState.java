package gr17.noodleio.game.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;

import gr17.noodleio.game.model.PlayerResult;
import gr17.noodleio.game.util.ResourceManager;

public class EndGameState extends State {

    private Stage stage;
    private Skin skin;
    private Table table;
    private Array<PlayerResult> results;
    private String playerName;
    private int placement;
    private ResourceManager rm;

    public EndGameState(GameStateManager gsm, Array<PlayerResult> results, String playerName, int placement, ResourceManager rm) {
        super(gsm);
        this.results = results;
        this.playerName = playerName;
        this.placement = placement;
        this.rm = rm;

        stage = new Stage(new FitViewport(800, 480, cam));
        Gdx.input.setInputProcessor(stage);


        skin = new Skin();
        skin.add("font", rm.getDefaultFont());

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = rm.getDefaultFont();
        skin.add("default", buttonStyle);

        table = new Table();
        table.setFillParent(true);
        stage.addActor(table);


        Label.LabelStyle labelStyle = new Label.LabelStyle(rm.getDefaultFont(), null);
        String titleText = (placement == 1) ? "YOU WON!" : "You reached place #" + placement;
        Label titleLabel = new Label(titleText, labelStyle);
        table.add(titleLabel).padBottom(50);
        table.row();


        for (int i = 0; i < results.size; i++) {
            PlayerResult r = results.get(i);


            BitmapFont font = new BitmapFont();
            font = rm.getDefaultFont();
            Label.LabelStyle resultStyle = new Label.LabelStyle(font, null);

            if (r.name.equals(playerName)) {
                font.getData().setScale(1.2f);
            } else {
                font.getData().setScale(1.0f);
            }

            Label resultLabel = new Label((i + 1) + ". " + r.name + " - " + r.score + " points", resultStyle);
            table.add(resultLabel).padBottom(10);
            table.row();
        }

        TextButton menuButton = new TextButton("Return to Menu", skin);
        table.add(menuButton).padTop(30).width(200).height(50);

        menuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                gsm.set(new MenuState(gsm));
            }
        });
    }

    @Override
    protected void handleInput() {}

    @Override
    public void update(float dt) {
        stage.act(dt);
    }

    @Override
    public void render(SpriteBatch sb) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.draw();
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
