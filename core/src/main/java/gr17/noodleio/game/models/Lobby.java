package gr17.noodleio.game.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// Using Observer pattern for notifying state changes
public class Lobby {
    private String lobbyCode;
    private List<Player> players;
    private Player host;
    private static final int MAX_PLAYERS = 8;

    public Lobby() {
        lobbyCode = UUID.randomUUID().toString();
        players = new ArrayList<>();
    }

    public boolean addPlayer(Player player) {
        if (players.size() < MAX_PLAYERS) {
            players.add(player);
            if (player.isHost()) {
                host = player;
            }
            return true;
        }
        return false;
    }

    public boolean removePlayer(Player player) {
        boolean result = players.remove(player);
        if (result) {
            if (player.isHost() && !players.isEmpty()) {
                players.get(0).setHost(true);
                host = players.get(0);
            }
        }
        return result;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public Player getHost() {
        return host;
    }

    public int getPlayerCount() {
        return players.size();
    }

    public boolean isFull() {
        return players.size() >= MAX_PLAYERS;
    }
}
