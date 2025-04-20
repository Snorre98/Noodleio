package gr17.noodleio.game.states;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.*;

import gr17.noodleio.game.API.*;
import gr17.noodleio.game.config.*;
import gr17.noodleio.game.models.*;
import gr17.noodleio.game.util.ResourceManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayState extends State implements RealtimeGameStateApi.GameStateCallback {
    private final String sessionId, playerId;
    private boolean isGameOver = false;

    private final PlayerGameStateApi playerGameStateApi;
    private final RealtimeGameStateApi realtimeGameStateApi;
    private final LobbyPlayerApi lobbyPlayerApi;

    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final BitmapFont font;
    private final Viewport gameViewport, uiViewport;
    private final OrthographicCamera uiCamera = new OrthographicCamera();

    private final ConcurrentHashMap<String, PlayerGameState> playerStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> playerNames = new ConcurrentHashMap<>();
    private final Map<String, Color> playerColors = new HashMap<>();
    private final Color[] colorPalette = { Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW, Color.PURPLE, Color.CYAN };

    private GameSession currentSession;
    private float movementCooldown = 0;
    private static final float PLAYER_SIZE = 20, MOVEMENT_DELAY = 0.1f;

    public PlayState(GameStateManager gsm, String sessionId, String playerId, String playerName, ResourceManager rm) {
        super(gsm);
        this.sessionId = sessionId;
        this.playerId = playerId;
        this.font = rm != null ? rm.getDefaultFont() : new BitmapFont();

        // Initialize environment config
        EnvironmentConfig environmentConfig = new EnvironmentConfig() {
            @Override
            public String getSupabaseUrl() {
                return Config.getSupabaseUrl();
            }

            @Override
            public String getSupabaseKey() {
                return Config.getSupabaseKey();
            }
        };
        playerGameStateApi = new PlayerGameStateApi(environmentConfig);
        realtimeGameStateApi = new RealtimeGameStateApi(environmentConfig);
        lobbyPlayerApi = new LobbyPlayerApi(environmentConfig);
        realtimeGameStateApi.addCallback(this);

        gameViewport = new FitViewport(1080, 1080, cam);
        uiViewport = new FitViewport(800, 480, uiCamera);
        playerNames.put(playerId, playerName);
        playerColors.put(playerId, colorPalette[0]);

        realtimeGameStateApi.connect(sessionId, playerId);
    }

    @Override
    protected void handleInput() {
        if (movementCooldown > 0) return;

        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))
            sendMove(playerGameStateApi::movePlayerUp);
        else if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))
            sendMove(playerGameStateApi::movePlayerDown);
        else if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))
            sendMove(playerGameStateApi::movePlayerLeft);
        else if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT))
            sendMove(playerGameStateApi::movePlayerRight);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
            disconnectAndReturnToMenu();
    }

    private void sendMove(MoveCommand move) {
        move.execute(playerId, sessionId);
        movementCooldown = MOVEMENT_DELAY;
    }

    @Override
    public void update(float dt) {
        if (movementCooldown > 0) movementCooldown -= dt;
        if (!isGameOver) handleInput();
        updateCamera();

        if (currentSession != null && currentSession.getEnded_at() != null && !isGameOver)
            handleGameOver();
    }

    private void updateCamera() {
        PlayerGameState player = playerStates.get(playerId);
        if (player == null || currentSession == null) return;

        float lerp = 0.1f;
        cam.position.x += (player.getX_pos() - cam.position.x) * lerp;
        cam.position.y += (player.getY_pos() - cam.position.y) * lerp;

        float hw = cam.viewportWidth * cam.zoom / 2;
        float hh = cam.viewportHeight * cam.zoom / 2;
        cam.position.x = Math.max(hw, Math.min(currentSession.getMap_length() - hw, cam.position.x));
        cam.position.y = Math.max(hh, Math.min(currentSession.getMap_height() - hh, cam.position.y));
        cam.update();
    }

    @Override
    public void render(SpriteBatch sb) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        gameViewport.apply();
        sb.setProjectionMatrix(cam.combined);
        renderGameElements();
        sb.begin();
        renderPlayerNames(sb);
        sb.end();

        uiViewport.apply();
        sb.setProjectionMatrix(uiCamera.combined);
        sb.begin();
        renderUI(sb);
        sb.end();
    }

    private void renderGameElements() {
        if (currentSession == null) return;

        shapeRenderer.setProjectionMatrix(cam.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.DARK_GRAY);
        for (int x = 0; x <= currentSession.getMap_length(); x += 100)
            shapeRenderer.line(x, 0, x, currentSession.getMap_height());
        for (int y = 0; y <= currentSession.getMap_height(); y += 100)
            shapeRenderer.line(0, y, currentSession.getMap_length(), y);

        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(0, 0, currentSession.getMap_length(), currentSession.getMap_height());
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (PlayerGameState ps : playerStates.values()) {
            shapeRenderer.setColor(playerColors.getOrDefault(ps.getPlayer_id(), Color.RED));
            shapeRenderer.rect(ps.getX_pos() - PLAYER_SIZE / 2, ps.getY_pos() - PLAYER_SIZE / 2, PLAYER_SIZE, PLAYER_SIZE);
        }
        shapeRenderer.end();
    }

    private void renderPlayerNames(SpriteBatch sb) {
        for (PlayerGameState ps : playerStates.values()) {
            String name = playerNames.getOrDefault(ps.getPlayer_id(), "Player");
            font.setColor(playerColors.getOrDefault(ps.getPlayer_id(), Color.WHITE));
            font.draw(sb, name, ps.getX_pos() - name.length() * 4, ps.getY_pos() + PLAYER_SIZE + 10);
        }
    }

    private void renderUI(SpriteBatch sb) {
        float y = uiViewport.getWorldHeight() - 20;
        font.setColor(Color.WHITE);
        font.draw(sb, "SCORES:", 10, y);
        y -= 30;

        for (PlayerGameState ps : playerStates.values()) {
            String name = playerNames.getOrDefault(ps.getPlayer_id(), "Player");
            font.setColor(playerColors.getOrDefault(ps.getPlayer_id(), Color.WHITE));
            font.draw(sb, name + ": " + ps.getScore(), 10, y);
            y -= 25;
        }

        font.setColor(Color.WHITE);
        font.draw(sb, realtimeGameStateApi.getConnectionStatus(), 10, 20);
    }

    private void handleGameOver() {
        isGameOver = true;
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Gdx.app.postRunnable(this::disconnectAndReturnToMenu);
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private void disconnectAndReturnToMenu() {
        realtimeGameStateApi.disconnect();
        gsm.set(new MenuState(gsm));
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        if (font != null) font.dispose();
        realtimeGameStateApi.disconnect();
        realtimeGameStateApi.removeCallback(this);
    }

    @Override
    public void onPlayerStateChanged(PlayerGameState state) {
        playerStates.put(state.getPlayer_id(), state);
        playerNames.computeIfAbsent(state.getPlayer_id(), id -> {
            String result = lobbyPlayerApi.getPlayerById(id.replace("\"", ""));
            if (result != null && result.contains("Player found:")) {
                int start = result.indexOf("Player found: ") + 14;
                int end = result.indexOf(" (ID:", start);
                return result.substring(start, end);
            }
            return "Player";
        });
    }

    @Override
    public void onGameSessionChanged(GameSession gameSession) {
        currentSession = gameSession;
        if (gameSession != null) {
            cam.position.set(gameSession.getMap_length() / 2f, gameSession.getMap_height() / 2f, 0);
            cam.update();
        }
    }

    @Override
    public void onGameOver() {
        if (!isGameOver) handleGameOver();
    }

    private interface MoveCommand {
        String execute(String playerId, String sessionId);
    }
}
