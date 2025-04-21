package gr17.noodleio.game.Entities.Food;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;

public class Food {
    public Vector2 pos;
    public ShapeRenderer shape;
    public Circle collisionShape;
    public final int size = 12;
    public boolean isEat;
    public Vector2 vel;
    //Sound sound = Gdx.audio.newSound(Gdx.files.internal("sounds/eat.mp3"));

    public Food(Vector2 pos){
        this.pos = pos;
        vel = new Vector2(0,0);
        shape = new ShapeRenderer();
        collisionShape = new Circle(pos.x, pos.y, size);
        isEat = false;
    }

    public void update(){
        pos.add(vel);
        collisionShape.setPosition(pos.x, pos.y);
    }

    public void getAttracted(Vector2 snakePos){
        Vector2 acc = snakePos.sub(pos);
        acc.setLength(1);
        vel.add(acc);
        vel.setLength(4);
    }


    public void render(OrthographicCamera cam){
        if(!isEat){
            shape.setProjectionMatrix(cam.combined);
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(Color.RED);
            shape.circle(pos.x, pos.y, size,25);
            shape.end();
        }
    }

    public void delete(){
        isEat = true;
        //sound.play(0.2f);

    }
}
