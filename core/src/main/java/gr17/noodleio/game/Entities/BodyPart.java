package gr17.noodleio.game.Entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class BodyPart {
    public int size;
    public Vector2 pos;
    public ShapeRenderer shape;
    public Color color;

    public BodyPart(Color bodyColor) {
        shape = new ShapeRenderer();
        pos = new Vector2();
        size = 15;
        color = bodyColor;
    }

    public void increaseSize(){
        size++;
    }

    public void render(OrthographicCamera cam) {
        shape.setProjectionMatrix(cam.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(color);
        shape.circle(pos.x, pos.y,size,15);
        shape.end();
    }
}
