package gr17.noodleio.game.Entities.Food;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;

public class MagnetBoost extends PowerUp {

    public MagnetBoost(Vector2 pos, Texture texture) {
        super(pos, texture);
        type = "magnet";
    }
}