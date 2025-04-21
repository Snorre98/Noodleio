package gr17.noodleio.game.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import gr17.noodleio.game.Entities.Food.Food;
import gr17.noodleio.game.Entities.Food.MagnetBoost;
import gr17.noodleio.game.Entities.Food.PowerUp;
import gr17.noodleio.game.Entities.Snake;
import gr17.noodleio.game.Entities.Food.SpeedBoost;
import gr17.noodleio.game.util.Timer;

import java.util.ArrayList;
import java.util.Objects;

public class PlayState extends State {
    private SpriteBatch batch;
    public Vector3 mousePos;
    public Snake snake;
    public ArrayList<Food> foods;
    public ArrayList<PowerUp> powerUps;
    public OrthographicCamera cam;
    private BitmapFont font;
    public ShapeRenderer shape;

    public Timer testTimer;


    public PlayState(GameStateManager gsm) {
        super(gsm);
        shape = new ShapeRenderer();
        batch = new SpriteBatch();
        mousePos = new Vector3();

        testTimer = new Timer(1000,false);

        snake = new Snake();

        foods = new ArrayList<Food>();
        spawnFoods();

        powerUps = new ArrayList<PowerUp>();
        spawnPowerUps();

        cam = new OrthographicCamera(640, 480);
        cam.position.set(cam.viewportWidth / 20, cam.viewportHeight / 20, 0);

        font = new BitmapFont();
        font.getData().setScale(2);
    }

    public void spawnPowerUps(){
        powerUps.add(new SpeedBoost(new Vector2(150,150)));
        powerUps.add(new MagnetBoost(new Vector2(300,100)));
    }

    public void spawnFoods(){
        for(int i = 0; i < 50; i++){
            int x = -800 + (int)(Math.random() * ((800 - (-800)) + 1));
            int y = -800 + (int)(Math.random() * ((800 - (-800)) + 1));
            foods.add(new Food(new Vector2(x,y)));
        }
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



        int snakeSize = snake.size;
        if (Gdx.input.isTouched()) {
            snake.update(mousePos);
        }

        for(Food f: foods){
            snake.checkFoodCollision(f);
            if(snake.attractFood && snake.snakeHead.attractFoodDetection(f.collisionShape)){
                Vector2 snakePos = new Vector2(snake.pos.x, snake.pos.y);
                f.getAttracted(snakePos);
            }
            f.update();
        }

        for(PowerUp p: powerUps){
            if(snake.checkFoodCollision(p)){
                if(Objects.equals(p.getType(), "speed")){
                    snake.enableSpeedBoost();
                }
                if(Objects.equals(p.getType(), "magnet")){
                    snake.enableMagnetBoost();
                }
            }
            p.update();

        }




        if(snakeSize < snake.size){
            cam.zoom += 0.1;
        }

        if (Gdx.input.isTouched()) {
            cam.translate(snake.snakeHead.vel);
        }

        //snake.mapLimit();
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

        for(PowerUp p: powerUps){
            p.render(cam);
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
