package gr17.noodleio.game.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class ResourceManager {

    private BitmapFont defaultFont;
    private Texture wheatTexture;
    private Texture eggTexture;
    private Texture speedBoostTexture;
    private Texture magnetBoostTexture;
    private Texture backgroundTexture; // Add this field for background

    public void load() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("PressStart2P-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 24;
        parameter.minFilter = Texture.TextureFilter.Linear;
        parameter.magFilter = Texture.TextureFilter.Linear;

        defaultFont = generator.generateFont(parameter);
        generator.dispose();

        try {
            wheatTexture = new Texture(Gdx.files.internal("food/wheat.png"));
            eggTexture = new Texture(Gdx.files.internal("food/egg.png"));
            speedBoostTexture = new Texture(Gdx.files.internal("food/speedboost.png"));
            magnetBoostTexture = new Texture(Gdx.files.internal("food/magnetboost.png"));
            backgroundTexture = new Texture(Gdx.files.internal("food/bowl.png")); // Add this line
            
            // Apply texture filters for smoother scaling
            wheatTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            eggTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            speedBoostTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            magnetBoostTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear); // Add this line
        } catch (Exception e) {
            Gdx.app.error("ResourceManager", "Error loading textures", e);
        }
    }

    // Add getter for background texture
    public Texture getBackgroundTexture() {
        return backgroundTexture;
    }

    public BitmapFont getDefaultFont() {
        return defaultFont;
    }

    public Texture getWheatTexture() {
        return wheatTexture;
    }

    public Texture getEggTexture() {
        return eggTexture;
    }

    public Texture getRandomFoodTexture() {
        // 50/50 chance between wheat and egg
        return Math.random() < 0.5 ? wheatTexture : eggTexture;
    }

    public Texture getSpeedBoostTexture() {
        return speedBoostTexture;
    }

    public Texture getMagnetBoostTexture() {
        return magnetBoostTexture;
    }

    public void dispose() {
        if (defaultFont != null) {
            defaultFont.dispose();
        }
            
        // Dispose textures
        if (wheatTexture != null) wheatTexture.dispose();
        if (eggTexture != null) eggTexture.dispose();
        if (speedBoostTexture != null) speedBoostTexture.dispose();
        if (magnetBoostTexture != null) magnetBoostTexture.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
    }
}
