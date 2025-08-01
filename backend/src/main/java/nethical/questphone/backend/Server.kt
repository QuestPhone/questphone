package nethical.questphone.backend

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nethical.questphone.backend.worker.ProfileSyncWorker
import nethical.questphone.backend.worker.QuestSyncWorker
import nethical.questphone.backend.worker.StatsSyncWorker
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



fun triggerQuestSync(context: Context, isFirstSync: Boolean = false) {

    val inputData = workDataOf(
        "is_first_time" to isFirstSync
    )
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)  // run only when internet is available
        .build()

    val syncWorkRequest = OneTimeWorkRequestBuilder<QuestSyncWorker>()
        .setConstraints(constraints)
        .setInputData(inputData)
        .build()

    // Enqueue uniquely so multiple calls don’t pile up duplicate workers
    WorkManager.getInstance(context).enqueueUniqueWork(
        "sync_quests_work",
        ExistingWorkPolicy.KEEP,  // if the work is already enqueued or running, keep it and do not enqueue a new one
        syncWorkRequest
    )
}

fun triggerProfileSync(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)  // run only when internet is available
        .build()

    val syncWorkRequest = OneTimeWorkRequestBuilder<ProfileSyncWorker>()
        .setConstraints(constraints)
        .build()

    // Enqueue uniquely so multiple calls don’t pile up duplicate workers
    WorkManager.getInstance(context).enqueueUniqueWork(
        "sync_profile_work",
        ExistingWorkPolicy.KEEP,  // if the work is already enqueued or running, keep it and do not enqueue a new one
        syncWorkRequest
    )
}

fun triggerStatsSync(context: Context, isFirstSync: Boolean = false) {
    val inputData = workDataOf(
        "is_first_time" to isFirstSync
    )
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)  // run only when internet is available
        .build()

    val syncWorkRequest = OneTimeWorkRequestBuilder<StatsSyncWorker>()
        .setConstraints(constraints)
        .setInputData(inputData)
        .build()

    // Enqueue uniquely so multiple calls don’t pile up duplicate workers
    WorkManager.getInstance(context).enqueueUniqueWork(
        "sync_profile_work",
        ExistingWorkPolicy.KEEP,  // if the work is already enqueued or running, keep it and do not enqueue a new one
        syncWorkRequest
    )
}