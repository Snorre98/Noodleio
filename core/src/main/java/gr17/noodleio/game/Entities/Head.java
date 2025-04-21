package gr17.noodleio.game.Entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class Head extends BodyPart{
    public Vector2 vel;
    public Vector2 acc;
    public Circle collisionShape;
    public Circle magnetFoodShape;
    public int maxAcc;
    public int maxVel;

    public Head(Color bodyColor){
        super(bodyColor);
        vel = new Vector2();
        acc = new Vector2();
        maxAcc = 2;
        maxVel = 3;
        collisionShape = new Circle(pos.x, pos.y, size * 2);
        magnetFoodShape = new Circle(pos.x, pos.y, 120);
    }

    public void update(Vector3 mousePos){
        acc = new Vector2(mousePos.x, mousePos.y);

        acc.sub(pos);
        acc.setLength(maxAcc);

        vel.add(acc);
        vel.limit(maxVel);

        pos.add(vel);

        //System.out.println(pos);

        collisionShape.setPosition(pos.x, pos.y);
        magnetFoodShape.setPosition(pos.x, pos.y);
    }

    public boolean attractFoodDetection(Circle foodCircle){
        return magnetFoodShape.contains(foodCircle);
    }

    public boolean touchFood(Circle foodColShape){
        return collisionShape.contains(foodColShape);
    }

    @Override
    public void render(OrthographicCamera cam){
        shape.setProjectionMatrix(cam.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(color);
        shape.circle(pos.x, pos.y,size,15);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.BLACK);
        shape.circle(pos.x, pos.y,120,15);
        shape.end();
    }
}
