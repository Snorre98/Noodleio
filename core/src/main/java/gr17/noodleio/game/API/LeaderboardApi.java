package gr17.noodleio.game.API;

import java.util.List;
import java.util.Random;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.models.GameSession;
import gr17.noodleio.game.models.LeaderboardEntry;
import gr17.noodleio.game.views.LeaderboardViews;

public class LeaderboardApi {
    private final LeaderboardViews leaderboardView;
    private String leaderboardMessage = "";
    private String addEntryMessage = "";

    public LeaderboardApi(EnvironmentConfig environmentConfig) {
        this.leaderboardView = new LeaderboardViews(environmentConfig);
    }

    /**
     * Adds a given entry to the leaderboard
     * @param playerName The player name
     * @param score The score achieved
     * @param durationSeconds The time in seconds it took to achieve the score
     * @return Status message
     */
    public String addLeaderboardEntry(String playerName, int score, Double durationSeconds) {
        try {
            // Add the entry
            LeaderboardEntry newEntry = leaderboardView.addLeaderboardEntry(playerName, score, durationSeconds, null);
            if (newEntry != null) {
                String durationText = durationSeconds != null ?
                    " in " + formatDuration(durationSeconds) : "";

                addEntryMessage = "Added new entry: " + playerName + " with score " + score + durationText;
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
     * Adds a leaderboard entry from a completed game session
     * @param playerName The player name
     * @param score The final score
     * @param gameSession The completed game session with timing information
     * @return Status message
     */
    public String addLeaderboardEntryFromSession(String playerName, int score, GameSession gameSession) {
        try {
            LeaderboardEntry newEntry = leaderboardView.addLeaderboardEntryFromSession(playerName, score, gameSession);

            if (newEntry != null) {
                Double duration = newEntry.getDuration_seconds();
                String durationText = duration != null ?
                    " in " + formatDuration(duration) : "";

                addEntryMessage = "Added new entry: " + playerName + " with score " + score + durationText;
            } else {
                addEntryMessage = "Failed to add new leaderboard entry from session";
            }
            return addEntryMessage;
        } catch (Exception e) {
            addEntryMessage = "Error adding leaderboard entry from session: " + e.getMessage();
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
            StringBuilder sb = new StringBuilder("TOP " + limit + " PLAYERS\n");
            sb.append("------------------------\n");

            if (topEntries.isEmpty()) {
                sb.append("No entries found");
            } else {
                for (LeaderboardEntry entry : topEntries) {
                    sb.append(entry.getPlayer_name())
                        .append(": ")
                        .append(entry.getScore())
                        .append(" pts");

                    // Add duration information if available
                    if (entry.getDuration_seconds() != null) {
                        sb.append(" (").append(formatDuration(entry.getDuration_seconds())).append(")");
                    }

                    sb.append("\n");
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
     * Format duration in seconds to a human-readable string (mm:ss)
     * @param seconds Duration in seconds
     * @return Formatted string
     */
    private String formatDuration(Double seconds) {
        int minutes = (int) (seconds / 60);
        int remainingSeconds = (int) (seconds % 60);
        return String.format("%d:%02d", minutes, remainingSeconds);
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
    public LeaderboardViews getLeaderboardService() {
        return leaderboardView;
    }
}
