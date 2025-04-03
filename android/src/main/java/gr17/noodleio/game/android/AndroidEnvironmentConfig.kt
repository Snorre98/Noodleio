
package gr17.noodleio.game.android

import gr17.noodleio.game.BuildConfig
import gr17.noodleio.game.config.EnvironmentConfig

class AndroidEnvironmentConfig : EnvironmentConfig {
    override val supabaseUrl: String
        get() = BuildConfig.SUPABASE_URL

    override val supabaseKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY
}
