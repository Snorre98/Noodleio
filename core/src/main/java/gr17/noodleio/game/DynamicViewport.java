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
        // Calculate screen aspect ratio
        float aspectRatio = (float)screenWidth / screenHeight;
        
        // Calculate world dimensions that maintain this aspect ratio
        float worldWidth, worldHeight;
        
        if (aspectRatio > minWorldWidth / minWorldHeight) {
            // Screen is wider than minimum aspect ratio - use height as reference
            worldHeight = minWorldHeight;
            worldWidth = worldHeight * aspectRatio;
            
            // Cap to maximum width if needed
            if (worldWidth > maxWorldWidth) {
                worldWidth = maxWorldWidth;
                worldHeight = worldWidth / aspectRatio;
            }
        } else {
            // Screen is taller than minimum aspect ratio - use width as reference
            worldWidth = minWorldWidth;
            worldHeight = worldWidth / aspectRatio;
            
            // Cap to maximum height if needed
            if (worldHeight > maxWorldHeight) {
                worldHeight = maxWorldHeight;
                worldWidth = worldHeight * aspectRatio;
            }
        }
        
        // Update viewport with new dimensions
        setWorldSize(worldWidth, worldHeight);
        super.update(screenWidth, screenHeight, centerCamera);
    }
}
