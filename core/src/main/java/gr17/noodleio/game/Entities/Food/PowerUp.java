package gr17.noodleio.game.Entities.Food;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;


public class PowerUp extends Food {
    public String type;

    public PowerUp(Vector2 pos, Texture texture) {
        super(pos, texture);
    }

    public String getType(){
        return type;
    }
}
