package gr17.noodleio.game.Entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import java.util.ArrayList;

public class Snake {
    public ArrayList<BodyPart> body;
    public Head snakeHead;
    public int numOfParts = 5;
    public int score;
    public int growScore = 0;

    public Snake() {
        snakeHead = new Head(Color.YELLOW);

        body = new ArrayList<>();
        body.add(snakeHead);
        for (int i = 0; i <= numOfParts; i++) {
            body.add(new BodyPart(Color.BROWN));
        }
    }

    public Vector2 constrainDistance(Vector2 point, Vector2 anchor, float distance) {
        return (((point.sub(anchor)).nor()).scl(distance)).add(anchor);
    }

    public void changeBodySize(){
        for (BodyPart bp: body) {
            bp.increaseSize();
        }
    }

    public void update(Vector3 mousePos) {

        snakeHead.update(mousePos);
        for (int i = 1; i < body.size(); i++) {
            body.get(i).pos = constrainDistance(
                body.get(i).pos, body.get(i - 1).pos, body.get(i).size + 8
            );
        }

        if(growScore == 2){
            growScore = 0;
            body.add(new BodyPart(Color.BROWN));
            numOfParts++;
            this.changeBodySize();

        }
    }

    public void render(OrthographicCamera cam){
        for (BodyPart bp: body) {
            bp.render(cam);
        }
    }

    public void checkFoodCollision(Food food){
        if(!food.isEat){
            if(snakeHead.touchFood(food.collisionShape)){

                food.delete();
                score += 1;
                growScore++;

            }
        }

    }
}
