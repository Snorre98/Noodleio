package gr17.noodleio.game.Entities.Food;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class MagnetBoost extends PowerUp {

    public MagnetBoost(Vector2 pos) {
        super(pos);
        type = "magnet";
    }

    @Override
    public void render(OrthographicCamera cam){
        if(!isEat){
            shape.setProjectionMatrix(cam.combined);
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(Color.GREEN);
            shape.circle(pos.x, pos.y, size,25);
            shape.end();
        }

    }
}
