package gr17.noodleio.game.API;

import java.util.List;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.models.GameSession;
import gr17.noodleio.game.models.LeaderboardEntry;
import gr17.noodleio.game.services.LeaderboardService;

public class LeaderboardApi {
    private final LeaderboardService leaderboardView;
    private String leaderboardMessage = "";

    public LeaderboardApi(EnvironmentConfig environmentConfig) {
        this.leaderboardView = new LeaderboardService(environmentConfig);
    }

    /**
     * Adds a given entry to the leaderboard
     *
     * @param playerName      The player name
     * @param score           The score achieved
     * @param durationSeconds The time in seconds it took to achieve the score
     */
    public void addLeaderboardEntry(String playerName, int score, Double durationSeconds) {
        try {
            // Add the entry
            leaderboardView.addLeaderboardEntry(playerName, score, durationSeconds, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a leaderboard entry from a completed game session
     *
     * @param playerName  The player name
     * @param score       The final score
     * @param gameSession The completed game session with timing information
     */
    public void addLeaderboardEntryFromSession(String playerName, int score, GameSession gameSession) {
        try {
            leaderboardView.addLeaderboardEntryFromSession(playerName, score, gameSession);
        } catch (Exception e) {
            e.printStackTrace();
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
}
