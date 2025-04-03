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
        // Set up configuration from BuildConfig
        Config.setupFromBuildConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY);

        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true;
        initialize(new Core(), configuration);
    }
}
