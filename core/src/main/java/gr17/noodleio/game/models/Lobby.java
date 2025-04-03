package gr17.noodleio.game.models;

import java.util.ArrayList;
import java.util.List;

public class Lobby {
    private List<Player> players;

    public Lobby() {
        players = new ArrayList<>();
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void addPlayer(String name) {
        for (Player p : players) {
            if (p.getName().equals(name)) {
                return;
            }
        }
        players.add(new Player(name));
    }

    public void removePlayer(String name) {
        players.removeIf(p -> p.getName().equals(name));
    }

    public void clearLobby() {
        players.clear();
    }
}
