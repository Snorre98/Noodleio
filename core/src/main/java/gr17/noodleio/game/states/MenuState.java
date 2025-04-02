package gr17.noodleio.game.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class MenuState extends State {
    private Stage stage;
    private Skin skin;
    private Table table;

    public MenuState(GameStateManager gsm) {
        super(gsm);

        // Create stage with viewport that matches our camera
        stage = new Stage(new FitViewport(800, 480, cam));
        Gdx.input.setInputProcessor(stage);

        try {
            // Create skin with required resources
            skin = new Skin();

            // Load font and textures safely - check if they exist first
            if (Gdx.files.internal("default.fnt").exists()) {
                skin.add("default-font", new BitmapFont(Gdx.files.internal("default.fnt")));
            } else {
                // If not found, create a default font
                skin.add("default-font", new BitmapFont());
            }

            // Load button textures if they exist
            if (Gdx.files.internal("default-round.png").exists()) {
                skin.add("default-round", new Texture(Gdx.files.internal("default-round.png")));
            } else {
                // Use white pixel texture as fallback
                Texture whiteTex = new Texture(1, 1, Pixmap.Format.RGB888);
                skin.add("default-round", whiteTex);
            }

            if (Gdx.files.internal("default-round-down.png").exists()) {
                skin.add("default-round-down", new Texture(Gdx.files.internal("default-round-down.png")));
            } else {
                // Use gray pixel texture as fallback
                Texture grayTex = new Texture(1, 1, Pixmap.Format.RGB888);
                skin.add("default-round-down", grayTex);
            }

            // Create button styles
            TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
            textButtonStyle.font = skin.getFont("default-font");
            textButtonStyle.up = skin.newDrawable("default-round");
            textButtonStyle.down = skin.newDrawable("default-round-down");
            textButtonStyle.checked = skin.newDrawable("default-round-down");
            skin.add("default", textButtonStyle);
        } catch (Exception e) {
            Gdx.app.error("MenuState", "Error loading skin resources", e);

            // Create a minimal fallback skin
            skin = new Skin();
            skin.add("default-font", new BitmapFont());

            TextButton.TextButtonStyle fallbackStyle = new TextButton.TextButtonStyle();
            fallbackStyle.font = skin.getFont("default-font");
            skin.add("default", fallbackStyle);
        }

        // Create table for UI layout
        table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        try {
            // Add title to the top of the menu
            BitmapFont titleFont;
            if (Gdx.files.internal("default.fnt").exists()) {
                titleFont = new BitmapFont(Gdx.files.internal("default.fnt"));
            } else {
                titleFont = new BitmapFont();
            }
            titleFont.getData().setScale(2.0f);  // Make the title larger

            // Create a label style for the title
            Label.LabelStyle titleStyle = new Label.LabelStyle();
            titleStyle.font = titleFont;
            Label titleLabel = new Label("NOODLEIO", titleStyle);

            // Add the title to the table
            table.add(titleLabel).padBottom(50).colspan(1);
            table.row();
        } catch (Exception e) {
            Gdx.app.error("MenuState", "Error creating title", e);
            // Add a simple title as fallback
            Label titleLabel = new Label("NOODLEIO", new Label.LabelStyle(skin.getFont("default-font"), null));
            table.add(titleLabel).padBottom(50).colspan(1);
            table.row();
        }

        // Create buttons
        TextButton createGameButton = new TextButton("Create Game", skin);
        TextButton joinGameButton = new TextButton("Join Game", skin);
        TextButton leaderboardButton = new TextButton("Leaderboard", skin);

        // Add buttons to table with spacing
        table.add(createGameButton).padBottom(20).width(200).height(50);
        table.row();
        table.add(joinGameButton).padBottom(20).width(200).height(50);
        table.row();
        table.add(leaderboardButton).width(200).height(50);


        // Add button listeners
        createGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                gsm.set(new LobbyState(gsm));
                dispose(); // Dispose resources when changing states
            }
        });

        joinGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                gsm.set(new LobbyState(gsm));
                dispose(); // Dispose resources when changing states
            }
        });

        leaderboardButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // TODO: Implement leaderboard state
                // gsm.set(new LeaderboardState(gsm));
                // dispose(); // Uncomment when implemented
            }
        });
    }

    @Override
    protected void handleInput() {
        // Input is handled by Scene2D stage
    }

    @Override
    public void update(float dt) {
        stage.act(dt);
    }

    @Override
    public void render(SpriteBatch sb) {
        // Clear screen
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // We don't use the SpriteBatch from the parameter because Stage has its own batch
        stage.draw();
    }

    @Override
    public void dispose() {
        try {
            // Dispose all resources to prevent memory leaks
            if (stage != null) {
                stage.dispose();
            }

            if (skin != null) {
                // Make sure to dispose all textures added to the skin
                if (skin.has("default-round", Texture.class)) {
                    Texture tex = skin.get("default-round", Texture.class);
                    if (tex != null) tex.dispose();
                }

                if (skin.has("default-round-down", Texture.class)) {
                    Texture tex = skin.get("default-round-down", Texture.class);
                    if (tex != null) tex.dispose();
                }

                // Also dispose of any fonts we created directly (not through skin)
                if (stage != null) {
                    for (Actor actor : stage.getActors()) {
                        if (actor instanceof Table) {
                            Table table = (Table) actor;
                            for (Actor child : table.getChildren()) {
                                if (child instanceof Label) {
                                    Label label = (Label) child;
                                    if (label.getStyle() != null &&
                                        label.getStyle().font != null &&
                                        skin.has("default-font", BitmapFont.class) &&
                                        !label.getStyle().font.equals(skin.getFont("default-font"))) {
                                        label.getStyle().font.dispose();
                                    }
                                } else if (child instanceof TextButton) {
                                    TextButton button = (TextButton) child;
                                    if (button.getLabel() != null &&
                                        button.getLabel().getStyle() != null &&
                                        button.getLabel().getStyle().font != null &&
                                        skin.has("default-font", BitmapFont.class) &&
                                        !button.getStyle().font.equals(skin.getFont("default-font"))) {
                                        button.getLabel().getStyle().font.dispose();
                                    }
                                }
                            }
                        }
                    }
                }

                // Finally dispose the skin itself
                skin.dispose();
            }
        } catch (Exception e) {
            // Log any errors during disposal but don't crash
            Gdx.app.error("MenuState", "Error during dispose", e);
        }
    }
}
