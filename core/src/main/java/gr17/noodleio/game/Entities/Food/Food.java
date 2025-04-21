package gr17.noodleio.game.Entities.Food;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;

public class Food {
    public Vector2 pos;
    public Circle collisionShape;
    public final int size = 24;
    public boolean isEat;
    public Vector2 vel;
    public Texture texture; // Added texture field
    
    // Temporary vector to reduce allocations
    private final Vector2 tempVector = new Vector2();

    public Food(Vector2 pos) {
        this.pos = new Vector2(pos);
        vel = new Vector2(0,0);
        collisionShape = new Circle(pos.x, pos.y, size);
        isEat = false;
    }
    
    // Add a new constructor that accepts both position and texture
    public Food(Vector2 pos, Texture texture) {
        this(pos); // Call the existing constructor to reuse initialization code
        this.texture = texture;
    }

    public void update() {
        // Only update if there's actual movement
        if (vel.x != 0 || vel.y != 0) {
            pos.add(vel);
            collisionShape.setPosition(pos.x, pos.y);
        }
    }

    public void getAttracted(Vector2 snakePos) {
        // Use tempVector to reduce garbage collection
        tempVector.set(snakePos).sub(pos);
        
        // Only normalize if vector has length
        if (tempVector.len2() > 0.0001f) {
            tempVector.nor();
            vel.add(tempVector);
            
            // Use len2() for efficiency and limit velocity
            if (vel.len2() > 16) { // 4^2 = 16
                vel.nor().scl(4);
            }
        }
    }

    public void delete() {
        isEat = true;
    }
}