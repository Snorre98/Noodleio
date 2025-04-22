package gr17.noodleio.game.Entities.Food;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

public class MagnetBoost extends PowerUp {

    public MagnetBoost(Vector2 pos, Texture texture) {
        super(pos, texture);
        type = "magnet";
    }
}
