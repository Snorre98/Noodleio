package gr17.noodleio.game.Entities;

import com.badlogic.gdx.graphics.Color;
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

    // Cache for performance optimization
    private final Vector2 tempVec = new Vector2();

    public Head(Color bodyColor){
        super(bodyColor);
        vel = new Vector2();
        acc = new Vector2();
        maxAcc = 2;
        maxVel = 3;
        collisionShape = new Circle(pos.x, pos.y, size * 3);
        magnetFoodShape = new Circle(pos.x, pos.y, 120);
    }

    public void update(Vector3 mousePos){
        // Use temporary vector to avoid allocations
        tempVec.set(mousePos.x, mousePos.y);
        tempVec.sub(pos);

        // Only calculate new acceleration if significant movement
        if (tempVec.len2() > 0.01f) {
            tempVec.nor().scl(maxAcc);
            acc.set(tempVec);

            vel.add(acc);

            // Apply velocity limit
            if (vel.len2() > maxVel * maxVel) {
                vel.nor().scl(maxVel);
            }

            // Update position
            pos.add(vel);

            // Update collision shapes
            collisionShape.setPosition(pos.x, pos.y);
            magnetFoodShape.setPosition(pos.x, pos.y);
        }
    }

    public boolean attractFoodDetection(Circle foodCircle){
        return magnetFoodShape.contains(foodCircle);
    }

    public boolean touchFood(Circle foodColShape){
        return collisionShape.contains(foodColShape);
    }

}
