package nethical.questphone.backend

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

object Supabase {
    val url = BuildConfig.SUPABASE_URL
    val apiKey = BuildConfig.SUPABASE_API_KEY
    val supabase by lazy {
        createSupabaseClient(url, apiKey) {
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                encodeDefaults = true
                explicitNulls = false
            })
            install(Auth) {

                host = "signup"
                scheme = "blankphone"
                autoSaveToStorage = true
                autoLoadFromStorage = true
//                defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
            }
            install(Storage)
            install(Postgrest)
        }
    }
}
