package gr17.noodleio.game.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import gr17.noodleio.game.Entities.Food;
import gr17.noodleio.game.Entities.Snake;

import java.util.ArrayList;

public class PlayState extends State {
    private SpriteBatch batch;
    public Vector3 mousePos;
    public Snake snake;
    public ArrayList<Food> foods;
    public OrthographicCamera cam;
    private BitmapFont font;

    public PlayState(GameStateManager gsm) {
        super(gsm);
        batch = new SpriteBatch();
        mousePos = new Vector3();

        snake = new Snake();

        foods = new ArrayList<>();
        foods.add(new Food(new Vector2(100,400)));
        foods.add(new Food(new Vector2(350,50)));
        foods.add(new Food(new Vector2(200,250)));
        foods.add(new Food(new Vector2(250,300)));

        cam = new OrthographicCamera(640, 480);
        cam.position.set(cam.viewportWidth / 20, cam.viewportHeight / 20, 0);

        font = new BitmapFont();
        font.getData().setScale(2);
    }

    @Override
    protected void handleInput() {
    }

    @Override
    public void update(float dt) {
        handleInput();
        mousePos.x = Gdx.input.getX();
        mousePos.y = Gdx.input.getY();
        cam.unproject(mousePos);

        if (Gdx.input.isTouched()) {
            snake.update(mousePos);
        }

        for(Food f: foods){
            snake.checkFoodCollision(f);
        }


        if (Gdx.input.isTouched()) {
            cam.translate(snake.snakeHead.vel);
        }

        cam.update();
    }

    @Override
    public void render(SpriteBatch sb) {
        sb.begin();
        cam.unproject(new Vector3(mousePos.x, mousePos.y, 0));

        this.update(Gdx.graphics.getDeltaTime());

        snake.render(cam);
        for(Food f: foods){
            f.render(cam);
        }

        batch.begin();
        font.draw(batch, "" + snake.score, 15, Gdx.graphics.getHeight() - 20);
        batch.end();
        sb.end();
    }

    @Override
    public void dispose() {
    }
}
