package gr17.noodleio.game.API;

import java.util.List;
import java.util.Random;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.models.LeaderboardEntry;
import gr17.noodleio.game.views.LeaderboardView;

public class LeaderboardApi {
    private final LeaderboardView leaderboardView;
    private String leaderboardMessage = "";
    private String addEntryMessage = "";

    public LeaderboardApi(EnvironmentConfig environmentConfig) {
        this.leaderboardView = new LeaderboardView(environmentConfig);
    }

    /**
     * Adds a random test entry to the leaderboard
     * @return Status message
     */
    public String addTestLeaderboardEntry() {
        try {
            // Generate a random score between 100 and 10000
            Random random = new Random();
            int randomScore = random.nextInt(9901) + 100; // Random score between 100 and 10000

            // Generate a player name with a timestamp to make it unique
            String playerName = "TestPlayer_" + System.currentTimeMillis();

            // Add the entry
            LeaderboardEntry newEntry = leaderboardView.addLeaderboardEntry(playerName, randomScore, null);

            if (newEntry != null) {
                addEntryMessage = "Added new entry: " + playerName + " with score " + randomScore;
            } else {
                addEntryMessage = "Failed to add new leaderboard entry";
            }

            return addEntryMessage;
        } catch (Exception e) {
            addEntryMessage = "Error adding leaderboard entry: " + e.getMessage();
            e.printStackTrace();
            return addEntryMessage;
        }
    }

    /**
     * Fetches the top leaderboard entries
     * @param limit Number of entries to fetch
     * @return Formatted string of leaderboard entries
     */
    public String fetchLeaderboard(long limit) {
        try {
            // Get top entries
            List<LeaderboardEntry> topEntries = leaderboardView.getTopLeaderboard(limit);

            // Format a message to display
            StringBuilder sb = new StringBuilder("Top " + limit + " Players:\n");

            if (topEntries.isEmpty()) {
                sb.append("No entries found");
            } else {
                for (LeaderboardEntry entry : topEntries) {
                    sb.append(entry.getPlayer_name())
                        .append(": ")
                        .append(entry.getScore())
                        .append("\n");
                }
            }

            leaderboardMessage = sb.toString();
            return leaderboardMessage;
        } catch (Exception e) {
            leaderboardMessage = "Failed to load leaderboard: " + e.getMessage();
            e.printStackTrace();
            return leaderboardMessage;
        }
    }

    /**
     * Gets the most recent leaderboard message
     * @return The formatted leaderboard message
     */
    public String getLeaderboardMessage() {
        return leaderboardMessage;
    }

    /**
     * Gets the most recent add entry message
     * @return The add entry status message
     */
    public String getAddEntryMessage() {
        return addEntryMessage;
    }

    /**
     * Gets the underlying LeaderboardService
     * @return The LeaderboardService instance
     */
    public LeaderboardView getLeaderboardService() {
        return leaderboardView;
    }
}
