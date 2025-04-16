package gr17.noodleio.game.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import gr17.noodleio.game.Core;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        // Create the environment config first
        DesktopEnvironmentConfig config = new DesktopEnvironmentConfig();
        System.out.println("Created desktop environment config");

        // Pass the config to Core
        Core game = new Core(config);

        // Get the application configuration
        Lwjgl3ApplicationConfiguration appConfig = getDefaultConfiguration();

        // Return the application with both the game and configuration
        return new Lwjgl3Application(game, appConfig);
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("noodleio");

        // Vsync and FPS configuration
        configuration.useVsync(true);
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);

        // Window size
        configuration.setWindowedMode(640, 480);

        // Window icon
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");

        return configuration;
    }
}
