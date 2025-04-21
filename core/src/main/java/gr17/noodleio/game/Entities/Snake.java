package gr17.noodleio.game.Entities;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import gr17.noodleio.game.Entities.Food.Food;
import gr17.noodleio.game.util.Timer;

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

    public void changeBodySize(){
        for (BodyPart bp: body) {
            bp.increaseSize();
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

            // Get the last body segment's position
            BodyPart lastSegment = body.get(body.size() - 1);

            // Create new body part with the same position as the last one
            BodyPart newPart = new BodyPart(Color.BROWN);
            newPart.pos.set(lastSegment.pos);  // Initialize position!

            // Add the properly positioned body part
            body.add(newPart);

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
