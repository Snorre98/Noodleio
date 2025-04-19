package gr17.noodleio.game.API;

import gr17.noodleio.game.config.EnvironmentConfig;
import gr17.noodleio.game.views.RealtimeCursorService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import gr17.noodleio.game.models.CursorPosition;
import gr17.noodleio.game.views.RealtimeCursorService.CursorPositionListener;

/*
*  This is only used for proof of concept!
* */
public class CursorRealtimeApi {
    private final RealtimeCursorService cursorService;
    private String statusMessage = "Initializing...";
    private boolean isConnected = false;

    // Use ConcurrentHashMap for thread safety
    private final Map<String, CursorPosition> cursorPositions = new ConcurrentHashMap<>();

    // Default channel name
    private static final String DEFAULT_CHANNEL = "cursor-test-channel";

    // Current channel
    private String currentChannel = DEFAULT_CHANNEL;

    public CursorRealtimeApi(EnvironmentConfig environmentConfig) {
        this.cursorService = new RealtimeCursorService(environmentConfig);

        // Add listener for cursor positions
        this.cursorService.addListener(new CursorPositionListener() {
            @Override
            public void onCursorPositionReceived(CursorPosition position) {
                // Update the position in our map
                cursorPositions.put(position.getUserId(), position);
            }
        });
    }

    public Map<String, CursorPosition> getCursorPositions() {
        return cursorPositions;
    }

    /**
     * Connect to the default channel
     * @return Status message
     */
    public String connect() {
        return connect(DEFAULT_CHANNEL);
    }

    /**
     * Connect to a specific channel
     * @param channelName The channel name to connect to
     * @return Status message
     */
    public String connect(String channelName) {
        try {
            this.currentChannel = channelName;
            statusMessage = cursorService.connect(channelName);
            isConnected = true;
            return statusMessage;
        } catch (Exception e) {
            statusMessage = "Failed to connect: " + e.getMessage();
            isConnected = false;
            e.printStackTrace();
            return statusMessage;
        }
    }

    /**
     * Send cursor position to the channel
     * @param x X coordinate
     * @param y Y coordinate
     * @return Status message
     */
    public String sendCursorPosition(float x, float y) {
        try {
            if (!isConnected) {
                statusMessage = "Not connected. Call connect() first.";
                return statusMessage;
            }

            statusMessage = cursorService.sendCursorPosition(x, y);
            return statusMessage;
        } catch (Exception e) {
            statusMessage = "Failed to send cursor position: " + e.getMessage();
            e.printStackTrace();
            return statusMessage;
        }
    }

    /**
     * Disconnect from the channel
     * @return Status message
     */
    public String disconnect() {
        try {
            statusMessage = cursorService.disconnect();
            isConnected = false;
            return statusMessage;
        } catch (Exception e) {
            statusMessage = "Failed to disconnect: " + e.getMessage();
            e.printStackTrace();
            return statusMessage;
        }
    }

    /**
     * Get current status message
     * @return Status message
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * Get current channel name
     * @return Channel name
     */
    public String getCurrentChannel() {
        return currentChannel;
    }

    /**
     * Get user ID
     * @return User ID
     */
    public String getUserId() {
        return cursorService.getUserId();
    }

    /**
     * Check if connected
     * @return true if connected
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Get the underlying service
     * @return RealtimeCursorService
     */
    public RealtimeCursorService getCursorService() {
        return cursorService;
    }
}


