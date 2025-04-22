package gr17.noodleio.game.states.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class UIFactory {

    private static final float DEFAULT_BUTTON_WIDTH = 200;
    private static final float DEFAULT_BUTTON_HEIGHT = 50;
    private static final float DEFAULT_PADDING = 20;

    private final UIComponents uiComponents;

    public UIFactory() {
        this.uiComponents = UIComponents.getInstance();
    }

    public Stage createStage() {
        return new Stage(new FitViewport(800, 480));
    }

    public Table createMainTable() {
        Table table = new Table();
        table.setFillParent(true);
        return table;
    }

    public TextButton addButton(Table table, String text, Runnable onClick) {
        TextButton button = uiComponents.createButton(text);

        if (onClick != null) {
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    onClick.run();
                }
            });
        }

        table.add(button)
            .width(DEFAULT_BUTTON_WIDTH)
            .height(DEFAULT_BUTTON_HEIGHT)
            .padBottom(DEFAULT_PADDING);
        table.row();

        return button;
    }

    public TextButton addButton(Table table, String text, float width, float height, Runnable onClick) {
        TextButton button = uiComponents.createButton(text);

        if (onClick != null) {
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    onClick.run();
                }
            });
        }

        table.add(button)
            .width(width)
            .height(height)
            .padBottom(DEFAULT_PADDING);
        table.row();

        return button;
    }

    public Label addLabel(Table table, String text) {
        Label label = uiComponents.createLabel(text);
        table.add(label).padBottom(DEFAULT_PADDING);
        table.row();
        return label;
    }

    public Label addLabel(Table table, String text, float padding) {
        Label label = uiComponents.createLabel(text);
        table.add(label).padBottom(padding);
        table.row();
        return label;
    }

    public Label addTitle(Table table, String text) {
        Label titleLabel = uiComponents.createTitleLabel(text);
        table.add(titleLabel).padBottom(40);
        table.row();
        return titleLabel;
    }

    public void addRow(Table table) {
        table.row();
    }

    public void setupDefaultStage(Stage stage) {
        Gdx.input.setInputProcessor(stage);
    }

    // Standard back button helper
    public TextButton createBackButton(Table table, final Runnable onBack) {
        return addButton(table, "Back to Menu", () -> {
            if (onBack != null) {
                onBack.run();
            }
        });
    }

    // Helper for creating error labels
    public Label createStatusLabel(Table table) {
        Label statusLabel = uiComponents.createLabel("");
        table.add(statusLabel).padTop(30);
        table.row();
        return statusLabel;
    }
}
