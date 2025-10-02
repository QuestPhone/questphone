package neth.iecal.questphone.core

import android.content.Context
import android.util.Log
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsCodeVerifierCache
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
import nethical.questphone.backend.BuildConfig

object Supabase {
    val url = BuildConfig.SUPABASE_URL
    val apiKey = BuildConfig.SUPABASE_API_KEY


    private lateinit var appContext: Context
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
    val supabase by lazy {
        createSupabaseClient(url, apiKey) {
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                prettyPrint = false
                encodeDefaults = true
                explicitNulls = false
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
            install(Auth) {
                val sharedPreferences = appContext.getSharedPreferences(
                    "supabase_auth_pr",
                    Context.MODE_PRIVATE
                )
                val settings = SharedPreferencesSettings(sharedPreferences)
                sessionManager = SettingsSessionManager(settings)
                codeVerifierCache = SettingsCodeVerifierCache(settings)  // Add this line!

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

    suspend fun awaitSession(): String? {
        return try {
            // Force the client to initialize and load session
            supabase.auth.awaitInitialization()
            supabase.auth.currentSessionOrNull()?.user?.id
        } catch (e: Exception) {
            Log.e("Supabase", "Error loading session", e)
            null
        }
    }
}

