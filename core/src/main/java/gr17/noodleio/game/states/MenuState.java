package gr17.noodleio.game.states;

import com.badlogic.gdx.scenes.scene2d.ui.TextField;

import gr17.noodleio.game.API.LobbyApi;
import gr17.noodleio.game.API.LobbyPlayerApi;
import gr17.noodleio.game.config.Config;
import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.states.ui.BaseUIState;
import gr17.noodleio.game.states.ui.UIComponents;

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

        playerNameField = UIComponents.getInstance().createTextField("Enter player alias...");
        table.add(playerNameField).width(200).height(40).padBottom(20); table.row();

        uiFactory.addButton(table, "Create lobby", () -> {
            String name = playerNameField.getText();
            if (name == null || name.trim().isEmpty()) {
                setStatus("Please enter a player name");
            } else {
                createLobbyWithOwner(name);
            }
        });

        lobbyCodeField = UIComponents.getInstance().createTextField("Enter lobby code...");
        table.add(lobbyCodeField).width(200).height(40).padBottom(20); table.row();

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
            if (result.contains("Lobby created with ID:")) {
                String[] parts = result.split("\\|");
                lobbyId = parts[0].split(":")[1].trim();
                playerId = parts[1].split(":")[1].trim();
                LobbyState lobbyState = new LobbyState(gsm);
                lobbyState.setLobbyData(lobbyId, playerId, playerName);
                gsm.set(lobbyState);
            } else {
                setStatus("Failed to create lobby");
            }
        } catch (Exception e) {
            logError("Error creating lobby", e);
            setStatus("Unresolved error");
        }
    }

    private void joinLobby(String playerName, String code) {
        setStatus("Joining lobby...");
        try {
            String result = lobbyPlayerApi.joinLobby(playerName, code);
            LobbyState lobbyState = new LobbyState(gsm);
            lobbyState.setLobbyData(code, "Not needed", playerName);
            gsm.set(lobbyState);
        } catch (Exception e) {
            logError("Error joining lobby", e);
            setStatus("Error joining lobby");
        }
    }
}
