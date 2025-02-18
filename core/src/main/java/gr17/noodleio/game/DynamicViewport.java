package gr17.noodleio.game;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class DynamicViewport extends FitViewport {
    private float minWorldWidth;
    private float minWorldHeight;
    private float maxWorldWidth;
    private float maxWorldHeight;

    public DynamicViewport(float minWorldWidth, float minWorldHeight,
                           float maxWorldWidth, float maxWorldHeight,
                           OrthographicCamera camera) {
        super(minWorldWidth, minWorldHeight, camera);
        this.minWorldWidth = minWorldWidth;
        this.minWorldHeight = minWorldHeight;
        this.maxWorldWidth = maxWorldWidth;
        this.maxWorldHeight = maxWorldHeight;
    }

    @Override
    public void update(int screenWidth, int screenHeight, boolean centerCamera) {
        // Calculate new world size based on screen size
        float aspectRatio = (float)screenWidth / screenHeight;
        float width = minWorldWidth;
        float height = minWorldHeight;

        if (aspectRatio > maxWorldWidth / maxWorldHeight) {
            // Wider than maximum
            width = maxWorldWidth;
            height = width / aspectRatio;
        } else if (aspectRatio < minWorldWidth / minWorldHeight) {
            // Taller than minimum
            height = maxWorldHeight;
            width = height * aspectRatio;
        } else {
            // Within bounds, scale to fit
            width = minWorldHeight * aspectRatio;
            height = minWorldHeight;
        }

        // Update viewport with new dimensions
        setWorldSize(width, height);
        super.update(screenWidth, screenHeight, centerCamera);
    }
}
