package gr17.noodleio.game.services

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.functions.Functions
//import kotlin.time.Duration.Companion.seconds

val supabase = createSupabaseClient(
    supabaseUrl = "https://xyzcompany.supabase.co",
    supabaseKey = "public-anon-key"
) {
    install(Auth) {
       // alwaysAutoRefresh = false // default: true
        //autoLoadFromStorage = false // default: true
        //and more...
    }
    install(Postgrest) {
        //defaultSchema = "schema" // default: "public"
        //propertyConversionMethod = PropertyConversionMethod.SERIAL_NAME // default: PropertyConversionMethod.CAMEL_CASE_TO_SNAKE_CASE
    }
    install(Storage) {
       // transferTimeout = 90.seconds // Default: 120 seconds
    }
    install(Realtime){
        //reconnectDelay = 5.seconds
    }
    install(Functions){
        // no custom setttings
    }
}
