package gr17.noodleio.game.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class ResourceManager {

    private BitmapFont defaultFont;

    public void load() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("PressStart2P-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 24;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;

        defaultFont = generator.generateFont(parameter);
        generator.dispose();
    }


    public BitmapFont getDefaultFont() {
        return defaultFont;
    }

    public void dispose() {
        if (defaultFont != null) {
            defaultFont.dispose();
        }
    }
}
