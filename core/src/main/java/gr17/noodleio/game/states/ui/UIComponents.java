package gr17.noodleio.game.states.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

public class UIComponents {

    private static UIComponents instance;
    private Skin skin;
    private boolean initialized = false;

    private UIComponents() {
        // Initialize immediately on construction
        initialize();
    }

    public static UIComponents getInstance() {
        if (instance == null) {
            instance = new UIComponents();
        }
        return instance;
    }

    public Skin getSkin() {
        if (!initialized) {
            initialize();
        }
        return skin;
    }

    private void initialize() {
        if (initialized) {
            return;
        }

        try {
            skin = new Skin();
            Gdx.app.log("UIComponents", "Initializing skin");

            // Load default font
            if (Gdx.files.internal("default.fnt").exists()) {
                skin.add("default-font", new BitmapFont(Gdx.files.internal("default.fnt")));
            } else {
                skin.add("default-font", new BitmapFont());
            }

            // Create pixel textures
            createPixelTextures();

            // Create button styles
            createButtonStyle();

            // Create label styles
            createLabelStyle();

            // Create text field style
            createTextFieldStyle();

            initialized = true;
            Gdx.app.log("UIComponents", "Skin initialization complete");
        } catch (Exception e) {
            Gdx.app.error("UIComponents", "Error initializing skin", e);
            createFallbackSkin();
            initialized = true; // Even if fallback, mark as initialized
        }
    }

    private void createPixelTextures() {
        // White pixel
        Pixmap whitePix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        whitePix.setColor(Color.WHITE);
        whitePix.fill();
        skin.add("white", new Texture(whitePix));
        whitePix.dispose();

        // Black pixel
        Pixmap blackPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        blackPix.setColor(Color.BLACK);
        blackPix.fill();
        skin.add("black", new Texture(blackPix));
        blackPix.dispose();

        // Gray pixel
        Pixmap grayPix = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        grayPix.setColor(new Color(0.5f, 0.5f, 0.5f, 1f));
        grayPix.fill();
        skin.add("gray", new Texture(grayPix));
        grayPix.dispose();
    }

    private void createButtonStyle() {
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = skin.getFont("default-font");

        // Load button textures if exist, otherwise use pixel textures
        if (Gdx.files.internal("default-round.png").exists()) {
            skin.add("default-round", new Texture(Gdx.files.internal("default-round.png")));
        } else {
            skin.add("default-round", skin.get("black", Texture.class));
        }

        if (Gdx.files.internal("default-round-down.png").exists()) {
            skin.add("default-round-down", new Texture(Gdx.files.internal("default-round-down.png")));
        } else {
            skin.add("default-round-down", skin.get("gray", Texture.class));
        }

        textButtonStyle.up = skin.newDrawable("default-round");
        textButtonStyle.down = skin.newDrawable("default-round-down");
        textButtonStyle.checked = skin.newDrawable("default-round-down");
        skin.add("default", textButtonStyle);
    }

    private void createLabelStyle() {
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);
    }

    private void createTextFieldStyle() {
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = skin.getFont("default-font");
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.cursor = skin.newDrawable("white", Color.WHITE);
        textFieldStyle.selection = skin.newDrawable("white", new Color(0.5f, 0.5f, 0.5f, 0.5f));
        textFieldStyle.background = skin.newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 0.8f));
        skin.add("default", textFieldStyle);
    }

    private void createFallbackSkin() {
        if (skin == null) {
            skin = new Skin();
        }
        skin.add("default-font", new BitmapFont());

        TextButton.TextButtonStyle fallbackStyle = new TextButton.TextButtonStyle();
        fallbackStyle.font = skin.getFont("default-font");
        skin.add("default", fallbackStyle);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);
    }

    // Helper methods for creating common UI elements
    public TextButton createButton(String text) {
        // Ensure skin is initialized
        if (!initialized || skin == null) {
            initialize();
        }
        return new TextButton(text, skin);
    }

    public TextButton createButton(String text, float width, float height) {
        // Ensure skin is initialized
        if (!initialized || skin == null) {
            initialize();
        }
        TextButton button = new TextButton(text, skin);
        button.getLabel().setFontScale(1);
        return button;
    }

    public Label createLabel(String text) {
        // Ensure skin is initialized
        if (!initialized || skin == null) {
            initialize();
        }
        return new Label(text, skin);
    }

    public Label createLabel(String text, float fontScale) {
        // Ensure skin is initialized
        if (!initialized || skin == null) {
            initialize();
        }
        Label label = new Label(text, skin);
        label.setFontScale(fontScale);
        return label;
    }

    public Label createTitleLabel(String text) {
        return createLabel(text, 2.0f);
    }

    public TextField createTextField(String placeholder) {
        // Ensure skin is initialized
        if (!initialized || skin == null) {
            initialize();
        }
        TextField field = new TextField("", skin);
        field.setMessageText(placeholder);
        return field;
    }

    public void dispose() {
        if (skin != null) {
            // Dispose all textures
            if (skin.has("white", Texture.class)) {
                skin.get("white", Texture.class).dispose();
            }
            if (skin.has("black", Texture.class)) {
                skin.get("black", Texture.class).dispose();
            }
            if (skin.has("gray", Texture.class)) {
                skin.get("gray", Texture.class).dispose();
            }
            if (skin.has("default-round", Texture.class)) {
                skin.get("default-round", Texture.class).dispose();
            }
            if (skin.has("default-round-down", Texture.class)) {
                skin.get("default-round-down", Texture.class).dispose();
            }

            skin.dispose();
            skin = null;
            initialized = false;
        }
    }
}
