package gr17.noodleio.game.Entities;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
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
    //Sound sound = Gdx.audio.newSound(Gdx.files.internal("sounds/eat.mp3"));

    public Food(Vector2 pos){
        this.pos = pos;
        shape = new ShapeRenderer();
        collisionShape = new Circle(pos.x, pos.y, size);
        isEat = false;
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
