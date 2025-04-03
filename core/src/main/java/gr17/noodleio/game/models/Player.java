package gr17.noodleio.game.models;

public class Player {
    private String name;
    private boolean isHost;

    public Player(String name, boolean isHost) {
        this.name = name;
        this.isHost = isHost;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        isHost = host;
    }
}
