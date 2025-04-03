package gr17.noodleio.game.models;

import java.util.ArrayList;
import java.util.List;

public class Lobby {
    private List<String> players;

    public Lobby() {
        players = new ArrayList<>();
    }

    public List<String> getPlayers() {
        return players;
    }

    public void addPlayer(String playerName) {
        if (!players.contains(playerName)) {
            players.add(playerName);
        }
    }

    public void removePlayer(String playerName) {
        players.remove(playerName);
    }

    public void clearLobby() {
        players.clear();
    }
}
