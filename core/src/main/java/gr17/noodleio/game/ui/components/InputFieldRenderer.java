package gr17.noodleio.game.ui.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * A class to handle the creation and management of input fields.
 * This encapsulates the Stage, Skin, and TextField objects needed for input.
 */
public class InputFieldRenderer implements Disposable {
    private Stage stage;
    private Skin skin;
    private TextField textField;

    /**
     * Creates a new InputFieldRenderer with a default text field
     */
    public InputFieldRenderer() {
        this("", "Type something...");
    }

    /**
     * Creates a new InputFieldRenderer with the given initial text and hint
     *
     * @param initialText The text to show in the field initially
     * @param hintText The placeholder text when the field is empty
     */
    public InputFieldRenderer(String initialText, String hintText) {
        // Create stage with transparent background
        stage = new Stage(new FitViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        // Create and set up the skin
        setupSkin();

        // Create text field style
        //TextFieldStyle textFieldStyle = createTextFieldStyle();

        // Create and configure the text field
        textField = new TextField(initialText, textFieldStyle);
        textField.setMessageText(hintText);

        // Add text field to stage
        stage.addActor(textField);

        // Position and size
        setPosition(50, 50);
        setSize(300, 30);
    }

    /**
     * Creates and configures the skin for UI components
     */
    private void setupSkin() {
        skin = new Skin();

        // Create a 1x1 white texture for the cursor and background
        Pixmap whitePix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        whitePix.setColor(Color.WHITE);
        whitePix.fill();
        skin.add("white", new Texture(whitePix));
        whitePix.dispose();
    }

//    /**
//     * Creates a text field style using the skin
//     *
//     * @return The configured TextFieldStyle
//     */
//    private TextFieldStyle createTextFieldStyle() {
//        TextFieldStyle textFieldStyle = new TextFieldStyle();
//        textFieldStyle.font = new BitmapFont();  // Default font
//        textFieldStyle.fontColor = Color.WHITE;
//        textFieldStyle.cursor = skin.newDrawable("white", Color.WHITE);
//        textFieldStyle.selection = skin.newDrawable("white", new Color(0.5f, 0.5f, 0.5f, 0.5f));
//        textFieldStyle.background = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.8f));
//        return textFieldStyle;
//    }

    /**
     * Sets the position of the text field
     *
     * @param x X position
     * @param y Y position
     */
    public void setPosition(float x, float y) {
        textField.setPosition(x, y);
    }

    /**
     * Sets the size of the text field
     *
     * @param width Width
     * @param height Height
     */
    public void setSize(float width, float height) {
        textField.setSize(width, height);
    }


    /**
     * Disposes of resources used by this renderer
     */
    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
