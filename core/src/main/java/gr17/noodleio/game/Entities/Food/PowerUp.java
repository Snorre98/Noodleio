package gr17.noodleio.game.Entities.Food;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class PowerUp extends Food {
    public String type;

    public PowerUp(Vector2 pos) {
        super(pos);
    }

    public String getType(){
        return type;
    }

    @Override
    public void render(OrthographicCamera cam){
        super.render(cam);
    }
}