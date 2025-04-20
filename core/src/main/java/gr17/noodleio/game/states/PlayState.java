package gr17.noodleio.game.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import gr17.noodleio.game.API.LobbyPlayerApi;
import gr17.noodleio.game.API.PlayerGameStateApi;
import gr17.noodleio.game.API.RealtimeGameStateApi;
import gr17.noodleio.game.config.Config;
import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.models.GameSession;
import gr17.noodleio.game.models.PlayerGameState;
import gr17.noodleio.game.util.ResourceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayState extends State implements RealtimeGameStateApi.GameStateCallback {
    // BASIC FRAMEWORK - Just enough to get started
    private ShapeRenderer shapes;
    private BitmapFont font;
    private ConcurrentHashMap<String, PlayerGameState> players = new ConcurrentHashMap<>();
    private String playerId;
    private String sessionId;
    private GameSession currentSession;
    private RealtimeGameStateApi realtimeGameStateApi;
    private PlayerGameStateApi playerGameStateApi;
    private float movementCooldown = 0;
    private static final float MOVEMENT_DELAY = 0.1f;

    public PlayState(GameStateManager gsm, String sessionId, String playerId, String playerName, ResourceManager rm) {
        super(gsm);
        this.sessionId = sessionId;
        this.playerId = playerId;

        // Simple setup - pure screen coordinates, no fancy viewport or camera
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(2);

        // Initialize API and connect
        EnvironmentConfig config = new EnvironmentConfig() {
            @Override public String getSupabaseUrl() { return Config.getSupabaseUrl(); }
            @Override public String getSupabaseKey() { return Config.getSupabaseKey(); }
        };

        realtimeGameStateApi = new RealtimeGameStateApi(config);
        playerGameStateApi = new PlayerGameStateApi(config);
        realtimeGameStateApi.addCallback(this);
        realtimeGameStateApi.connect(sessionId, playerId);
    }

    @Override
    protected void handleInput() {
        if (movementCooldown <= 0) {
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                playerGameStateApi.movePlayerUp(playerId, sessionId);
                movementCooldown = MOVEMENT_DELAY;
            } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                playerGameStateApi.movePlayerDown(playerId, sessionId);
                movementCooldown = MOVEMENT_DELAY;
            } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                playerGameStateApi.movePlayerLeft(playerId, sessionId);
                movementCooldown = MOVEMENT_DELAY;
            } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                playerGameStateApi.movePlayerRight(playerId, sessionId);
                movementCooldown = MOVEMENT_DELAY;
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            realtimeGameStateApi.disconnect();
            gsm.set(new MenuState(gsm));
        }
    }

    @Override
    public void update(float dt) {
        if (movementCooldown > 0) movementCooldown -= dt;
        handleInput();
    }

    @Override
    public void render(SpriteBatch sb) {
        // SIMPLE RENDERING - just screen coordinates
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.3f, 1); // Dark blue
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw giant test patterns - these should be visible no matter what
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Draw red crosshair in middle of screen
        shapes.setColor(Color.RED);
        shapes.rectLine(
            Gdx.graphics.getWidth()/2 - 100, Gdx.graphics.getHeight()/2,
            Gdx.graphics.getWidth()/2 + 100, Gdx.graphics.getHeight()/2, 10);
        shapes.rectLine(
            Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2 - 100,
            Gdx.graphics.getWidth()/2, Gdx.graphics.getHeight()/2 + 100, 10);

        // Draw corner markers
        shapes.setColor(Color.GREEN);
        shapes.circle(0, 0, 30);
        shapes.circle(Gdx.graphics.getWidth(), 0, 30);
        shapes.circle(0, Gdx.graphics.getHeight(), 30);
        shapes.circle(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 30);

        // Draw all players (screen coordinates)
        for (PlayerGameState player : players.values()) {
            float x = player.getX_pos() * Gdx.graphics.getWidth() / 1080;
            float y = player.getY_pos() * Gdx.graphics.getHeight() / 1080;

            shapes.setColor(Color.WHITE);
            shapes.circle(x, y, 30);

            shapes.setColor(Color.BLUE);
            shapes.circle(x, y, 20);
        }

        shapes.end();

        // Draw text overlay
        sb.begin();
        font.setColor(Color.WHITE);

        // Draw player positions
        float y = Gdx.graphics.getHeight() - 50;
        font.draw(sb, "Players: " + players.size(), 20, y);

        for (PlayerGameState player : players.values()) {
            y -= 40;
            String isLocal = player.getPlayer_id().replace("\"", "").equals(playerId) ? " (YOU)" : "";
            font.draw(sb, String.format("Player %s: (%.1f, %.1f)%s",
                player.getPlayer_id().substring(0, 4),
                player.getX_pos(), player.getY_pos(), isLocal), 20, y);
        }

        font.draw(sb, "Press ESC to exit", 20, 40);
        sb.end();
    }

    @Override
    public void dispose() {
        shapes.dispose();
        font.dispose();
        realtimeGameStateApi.removeCallback(this);
        realtimeGameStateApi.disconnect();
    }

    // Callback implementations
    @Override
    public void onPlayerStateChanged(PlayerGameState playerState) {
        String pid = playerState.getPlayer_id().replace("\"", "");
        players.put(pid, playerState);
    }

    @Override
    public void onGameSessionChanged(GameSession gameSession) {
        currentSession = gameSession;
    }

    @Override
    public void onGameOver() {
        // TODO
    }

//    @Override
//    public void onGameOver() {
//        if (!isGameOver) {
//            handleGameOver();
//        }
//    }
}
