package gr17.noodleio.game.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
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
        // Add title at the top spanning both columns
        table.row();
        uiFactory.addTitle(table, "NOODLEIO");
        table.getCell(table.getChildren().peek()).colspan(2);
        
        // Create two columns using nested tables
        Table leftColumn = new Table();
        Table rightColumn = new Table();
        
        // Configure the left column
        setupLeftColumn(leftColumn);
        
        // Configure the right column
        setupRightColumn(rightColumn);
        
        // Add both columns to the main table with equal width
        table.row();
        table.add(leftColumn).width(200).pad(10);
        table.add(rightColumn).width(200).pad(10);
        
        // Add status label at the bottom spanning both columns
        table.row();
        statusLabel = uiFactory.createStatusLabel(table);
        table.getCell(statusLabel).colspan(2);
    }
    
    private void setupLeftColumn(Table leftColumn) {
        // Create player field in left column
        playerNameField = uiFactory.addTextField(leftColumn, "Enter player alias...", 200, 40);
        playerNameField.setTextFieldListener((textField, c) -> {
            if (c == '\n') { // Detect Enter key
                Gdx.input.setOnscreenKeyboardVisible(false);
            }
        });
        
        // Create lobby button in left column
        uiFactory.addButton(leftColumn, "Create lobby", () -> {
            String name = playerNameField.getText();
            if (name == null || name.trim().isEmpty()) {
                setStatus("Please enter a player name");
            } else {
                createLobbyWithOwner(name);
            }
        });
        uiFactory.addButton(leftColumn, "Leaderboard", () -> gsm.set(new LeaderboardState(gsm)));
    }
    
    private void setupRightColumn(Table rightColumn) {
        // Create lobby code field in right column - changed hint text to indicate only 5 chars needed
        lobbyCodeField = uiFactory.addTextField(rightColumn, "Enter first 5 chars of lobby code...", 200, 40);
        lobbyCodeField.setTextFieldListener((textField, c) -> {
            if (c == '\n') {
                Gdx.input.setOnscreenKeyboardVisible(false);
            }
        });
        
        // Join lobby button in right column
        uiFactory.addButton(rightColumn, "Join lobby", () -> {
            String name = playerNameField.getText();
            String code = lobbyCodeField.getText();
            if (name == null || name.trim().isEmpty()) {
                setStatus("Please enter a player name");
            } else if (code == null || code.trim().isEmpty()) {
                setStatus("Please enter a lobby code");
            } else if (code.length() < 5) {
                setStatus("Please enter at least 5 characters of the lobby code");
            } else {
                joinLobby(name, code);
            }
        });
        
        // Add additional buttons to right column
        uiFactory.addButton(rightColumn, "Exit", () -> Gdx.app.exit());
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
                
                // Get short code (first 5 chars) for display
                String shortCode = lobbyId.substring(0, Math.min(5, lobbyId.length()));
                
                LobbyState lobbyState = new LobbyState(gsm);
                lobbyState.setLobbyData(lobbyId, playerId, playerName, shortCode);
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