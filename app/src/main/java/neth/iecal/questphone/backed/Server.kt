package neth.iecal.questphone.backed

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import neth.iecal.questphone.core.workers.ProfileSyncWorker
import neth.iecal.questphone.core.workers.QuestSyncWorker
import neth.iecal.questphone.core.workers.StatsSyncWorker
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




fun triggerProfileSync(context: Context, isFirstLoginSync:Boolean = false) {
    try {
        Log.d("Sync", "Syncing profile")
        val workRequest = OneTimeWorkRequestBuilder<ProfileSyncWorker>()
            .setInputData(ProfileSyncWorker.buildInputData(isFirstLoginPull = isFirstLoginSync))
            .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("profile_sync", ExistingWorkPolicy.REPLACE, workRequest)
    }catch (e: Exception){
        Log.e("Error occured starting profile sync",e.stackTrace.toString())
    }
}


fun triggerStatsSync(context: Context, isFirstSync: Boolean = false, pullAllForToday: Boolean = false) {
    try {
        Log.d("Sync", "Syncing Stats")

        val input = Data.Builder()
            .putBoolean(StatsSyncWorker.EXTRA_IS_FIRST_TIME, isFirstSync)
            .putBoolean(StatsSyncWorker.EXTRA_IS_PULL_FOR_TODAY, pullAllForToday)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<StatsSyncWorker>()
            .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
            .setInputData(input)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("stat_sync", ExistingWorkPolicy.REPLACE, workRequest)
    }catch (e: Exception){
    Log.e("Error occured starting stats sync",e.stackTrace.toString())
}

}
fun triggerQuestSync(context: Context, isFirstSync: Boolean = false, pullForQuest: String? = null) {
    try {
        Log.d("Sync", "Syncing Quest")

        val input = Data.Builder()
            .putBoolean(QuestSyncWorker.EXTRA_IS_FIRST_TIME, isFirstSync)

        pullForQuest?.let { input.putString(QuestSyncWorker.EXTRA_IS_PULL_SPECIFIC_QUEST, it) }

        val workRequest = OneTimeWorkRequestBuilder<QuestSyncWorker>()
            .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
            .setInputData(input.build())
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("quest_sync", ExistingWorkPolicy.REPLACE, workRequest)
    }catch (e: Exception){
        Log.e("Error occured starting Quest sync",e.stackTrace.toString())
    }
}
