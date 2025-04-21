package gr17.noodleio.game.Entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import gr17.noodleio.game.Entities.Food.Food;
import gr17.noodleio.game.util.Timer;

import java.util.ArrayList;

public class Snake {
    public ArrayList<BodyPart> body;
    public Head snakeHead;
    public int size = 5;
    public int score;
    public int growScore = 0;
    public Vector2 pos;
    public Timer speedBoostTimer;

    public boolean attractFood = false;
    public Timer magnetBoostTimer;
    
    // Cache for performance optimization
    private final Vector2 tempVec1 = new Vector2();
    private final Vector2 tempVec2 = new Vector2();

    public Snake() {
        snakeHead = new Head(Color.YELLOW);
        body = new ArrayList<>();
        body.add(snakeHead);
        for (int i = 0; i <= size; i++) {
            body.add(new BodyPart(Color.BROWN));
        }

        pos = new Vector2();
        speedBoostTimer = new Timer(4500,false);
        magnetBoostTimer = new Timer(8000,false);
    }

    public Vector2 constrainDistance(Vector2 point, Vector2 anchor, float distance) {
        // Use cached vectors to avoid allocations
        tempVec1.set(point).sub(anchor).nor().scl(distance).add(anchor);
        return tempVec1;
    }

    public void changeBodySize(){
        for (BodyPart bp: body) {
            bp.increaseSize();
        }
    }

    public void mapLimit(){
        int mapLimitX = 1080;
        int mapLimitY = 1080;
        
        // Use cached vector for calculations
        tempVec2.setZero();

        if(pos.x > mapLimitX){
            tempVec2.x = -3;
        } else if(pos.x < -mapLimitX){
            tempVec2.x = 3;
        }

        if(pos.y > mapLimitY){
            tempVec2.y = -3;
        } else if(pos.y < -mapLimitY){
            tempVec2.y = 3;
        }
        
        // Only add if needed
        if(!tempVec2.isZero()) {
            snakeHead.vel.add(tempVec2);
        }
    }

    public void update(Vector3 mousePos) {
        snakeHead.update(mousePos);
        
        // Optimization: Only update positions of visible segments
        int visibleSegments = Math.min(body.size(), 20); // Limit to 20 segments for performance
        
        for (int i = 1; i < visibleSegments; i++) {
            BodyPart current = body.get(i);
            BodyPart previous = body.get(i - 1);
            
            // Instead of creating new Vector2 objects, use inlined calculation
            Vector2 currentPos = current.pos;
            Vector2 prevPos = previous.pos;
            
            // Calculate direction and distance
            float dx = currentPos.x - prevPos.x;
            float dy = currentPos.y - prevPos.y;
            float distance = (float)Math.sqrt(dx*dx + dy*dy);
            
            // Only adjust if needed
            if (distance != (current.size + 8)) {
                float targetDistance = current.size + 8;
                float ratio = targetDistance / Math.max(distance, 0.0001f);
                
                // Update position to maintain the correct distance
                currentPos.x = prevPos.x + dx * ratio;
                currentPos.y = prevPos.y + dy * ratio;
            }
        }

        pos = snakeHead.pos;

        if(growScore == 2){
            growScore = 0;
            body.add(new BodyPart(Color.BROWN));
            size++;
            this.changeBodySize();
        }

        if(speedBoostTimer.isRunning()){
            snakeHead.maxAcc = 5;
            snakeHead.maxVel = 5;
            if(speedBoostTimer.tick()){
                speedBoostTimer.stop();
                snakeHead.maxAcc = 2;
                snakeHead.maxVel = 3;
            }
        }

        if(magnetBoostTimer.isRunning()){
            attractFood = true;
            if(magnetBoostTimer.tick()){
                magnetBoostTimer.stop();
                attractFood = false;
            }
        }
    }

    public void enableSpeedBoost(){
        speedBoostTimer.start();
    }

    public void enableMagnetBoost(){
        attractFood = true;
        magnetBoostTimer.start();
    }

    // This method is now handled by the PlayState for batch rendering
    public void render(OrthographicCamera cam){
        // Left empty as rendering is now batched in PlayState
    }

    public boolean checkFoodCollision(Food food){
        if(!food.isEat){
            if(snakeHead.touchFood(food.collisionShape)){
                food.delete();
                score += 1;
                growScore++;
                return true;
            }
        }
        return false;
    }
}