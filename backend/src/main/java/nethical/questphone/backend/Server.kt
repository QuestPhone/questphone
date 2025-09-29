package nethical.questphone.backend

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nethical.questphone.backend.services.sync.ProfileSyncService
import nethical.questphone.backend.services.sync.QuestSyncService
import nethical.questphone.backend.services.sync.QuestSyncService.Companion.EXTRA_IS_FIRST_TIME
import nethical.questphone.backend.services.sync.QuestSyncService.Companion.EXTRA_IS_PULL_SPECIFIC_QUEST
import nethical.questphone.backend.services.sync.StatsSyncService
import okhttp3.OkHttpClient
import okhttp3.Request

suspend fun fetchUrlContent(url: String): String? {
    val okHttpClient = OkHttpClient()
    return withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .build()

            val response = okHttpClient.newCall(request).execute()
            response.body?.string()
        } catch (e: Exception) {
            Log.d("Error fetching url", e.toString())
            null // Return null on error
        }
    }
}

@RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
fun Context.isOnline(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}



fun triggerQuestSync(context: Context, isFirstSync: Boolean = false, pullForQuest:String? = null) {
    val intent = Intent(context, QuestSyncService::class.java).apply {
        putExtra(EXTRA_IS_FIRST_TIME, isFirstSync)
        if(pullForQuest!=null){
            putExtra(EXTRA_IS_PULL_SPECIFIC_QUEST, pullForQuest)
        }
    }
    context.startForegroundService(intent)
}

fun triggerProfileSync(context: Context, isFirstLoginSync:Boolean = false) {
    ProfileSyncService.start(context,isFirstLoginSync)
}

fun triggerStatsSync(context: Context, isFirstSync: Boolean = false,pullAllForToday:Boolean = false) {
    StatsSyncService.start(context,isFirstSync,pullAllForToday)
}