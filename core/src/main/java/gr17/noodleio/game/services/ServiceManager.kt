package gr17.noodleio.game.services

import gr17.noodleio.game.config.EnvironmentConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime

class ServiceManager(private val config: EnvironmentConfig) {
    private val supabaseClient: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = config.supabaseUrl,
            supabaseKey = config.supabaseKey
        ) {
            install(Auth) {
                // alwaysAutoRefresh = false // default: true
                //autoLoadFromStorage = false // default: true
            }
            install(Postgrest) {
                //defaultSchema = "schema" // default: "public"
                //propertyConversionMethod = PropertyConversionMethod.SERIAL_NAME // default: PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE
            }
            install(Realtime){
                //reconnectDelay = 5.seconds
            }
            /*
            install(Storage) {
                // transferTimeout = 90.seconds // Default: 120 seconds
            }
            install(Functions){
                // no custom settings
            }*/
        }
    }

    // Expose service getters at the class level
    val db: Postgrest get() = supabaseClient.postgrest
    val realtime: Realtime get() = supabaseClient.realtime
}
