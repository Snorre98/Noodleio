package gr17.noodleio.game.android;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import gr17.noodleio.game.Core;
import gr17.noodleio.game.config.Config;
import gr17.noodleio.game.BuildConfig;

/** Launches the Android application. */
public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Setup configuration from BuildConfig
        Config.setupFromBuildConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        
        // Improved settings for mobile performance and battery life
        config.useAccelerometer = false;
        config.useCompass = false;
        
        // Use GPU for rendering
        config.useGL30 = false; // Set to true only if you need OpenGL ES 3.0
        
        initialize(new Core(), config);
    }
}
