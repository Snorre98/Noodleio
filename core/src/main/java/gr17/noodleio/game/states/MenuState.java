package gr17.noodleio.game.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

import gr17.noodleio.game.API.LobbyApi;
import gr17.noodleio.game.API.LobbyPlayerApi;
import gr17.noodleio.game.config.Config;
import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.states.ui.BaseUIState;

public class MenuState extends BaseUIState {

    private TextField playerNameField;
    private TextField lobbyCodeField;
    private LobbyApi lobbyApi;
    private LobbyPlayerApi lobbyPlayerApi;
    private String lobbyId;
    private String playerId;

    public MenuState(GameStateManager gsm) {
        super(gsm);
        initializeApis();
    }

    @Override
    protected void setupUI() {
        uiFactory.addTitle(table, "NOODLEIO");

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        uiFactory.addTitle(table, "NOODLEIO");

        // w:750 x h:1334 on 375 x 667 device ==> 2dpi
        uiFactory.addLabel(table, "Resolution: " + screenWidth + " x " + screenHeight, 10);


        playerNameField = uiFactory.addTextField(table, "Enter player alias...", 200, 40);
        playerNameField.setTextFieldListener((textField, c) -> {
            if (c == '\n') { // Detect Enter key
                Gdx.input.setOnscreenKeyboardVisible(false);
            }
        });
        uiFactory.addButton(table, "Create lobby", () -> {
            String name = playerNameField.getText();
            if (name == null || name.trim().isEmpty()) {
                setStatus("Please enter a player name");
            } else {
                createLobbyWithOwner(name);
            }
        });

        lobbyCodeField = uiFactory.addTextField(table, "Enter lobby code...", 200, 40);
        lobbyCodeField.setTextFieldListener((textField, c) -> {
            if (c == '\n') {
                Gdx.input.setOnscreenKeyboardVisible(false);
            }
        });
        uiFactory.addButton(table, "Join lobby", () -> {
            String name = playerNameField.getText();
            String code = lobbyCodeField.getText();
            if (name == null || name.trim().isEmpty()) {
                setStatus("Please enter a player name");
            } else if (code == null || code.trim().isEmpty()) {
                setStatus("Please enter a lobby code");
            } else {
                joinLobby(name, code);
            }
        });

        uiFactory.addButton(table, "Leaderboard", () -> gsm.set(new LeaderboardState(gsm)));

        uiFactory.addButton(table, "Exit", () -> Gdx.app.exit());

        statusLabel = uiFactory.createStatusLabel(table);
    }


    private void initializeApis() {
        EnvironmentConfig config = new EnvironmentConfig() {
            public String getSupabaseUrl() { return Config.getSupabaseUrl(); }
            public String getSupabaseKey() { return Config.getSupabaseKey(); }
        };
        lobbyApi = new LobbyApi(config);
        lobbyPlayerApi = new LobbyPlayerApi(config);
    }

    private void createLobbyWithOwner(String playerName) {
        setStatus("Creating lobby...");
        try {


            String result = lobbyApi.createLobbyWithOwner(playerName);
            log("LobbyApi result: " + result);

            if (result.contains("Lobby created with ID:")) {
                String[] parts = result.split("\\|");
                lobbyId = parts[0].split(":")[1].trim();
                playerId = parts[1].split(":")[1].trim();
                LobbyState lobbyState = new LobbyState(gsm);
                lobbyState.setLobbyData(lobbyId, playerId, playerName);
                gsm.set(lobbyState);
            } else {
                setStatus("Failed to create lobby: " + result);
                log("Lobby creation failed with result: " + result);
            }
        } catch (Exception e) {
            logError("Error creating lobby", e);
            setStatus("Error: " + e.getMessage());
        }
    }

    private void joinLobby(String playerName, String code) {
        setStatus("Joining lobby...");
        try {
            lobbyPlayerApi.joinLobby(playerName, code);
            LobbyState lobbyState = new LobbyState(gsm);
            lobbyState.setLobbyData(code, "Not needed", playerName);
            gsm.set(lobbyState);
        } catch (Exception e) {
            logError("Error joining lobby", e);
            setStatus("Error joining lobby");
        }
    }
}
