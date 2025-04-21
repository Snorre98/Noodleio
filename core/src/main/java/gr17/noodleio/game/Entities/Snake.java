package gr17.noodleio.game.Entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
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
        return (((point.sub(anchor)).nor()).scl(distance)).add(anchor);
    }

    public void changeBodySize(){
        for (BodyPart bp: body) {
            bp.increaseSize();
        }
    }

    public void mapLimit(){
        int mapLimitX = 500;
        int mapLimitY = 500;

        if(pos.x > mapLimitX){
            snakeHead.vel.add(new Vector2(-3,0));
        }

        if(pos.x < -mapLimitX){
            snakeHead.vel.add(new Vector2(3,0));
        }

        if(pos.y > mapLimitY){
            snakeHead.vel.add(new Vector2(0,-3));
        }

        if(pos.y < -mapLimitY){
            snakeHead.vel.add(new Vector2(0,3));
        }
    }

    public void update(Vector3 mousePos) {

        snakeHead.update(mousePos);
        for (int i = 1; i < body.size(); i++) {
            body.get(i).pos = constrainDistance(
                body.get(i).pos, body.get(i - 1).pos, body.get(i).size + 8
            );
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

    public void render(OrthographicCamera cam){
        for (BodyPart bp: body) {
            bp.render(cam);
        }
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
