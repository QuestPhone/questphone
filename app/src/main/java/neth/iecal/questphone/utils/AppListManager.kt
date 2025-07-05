package neth.iecal.questphone.utils

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.inputmethod.InputMethodManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import neth.iecal.questphone.data.AppInfo


// Cache the app list in SharedPreferences
fun cacheApps(context: Context, apps: List<AppInfo>) {
    val sharedPreferences = context.getSharedPreferences("app_cache", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val json = Json.encodeToString(apps)
    editor.putString("apps", json)
    editor.apply()
}

// Retrieve the cached app list from SharedPreferences
fun getCachedApps(context: Context): List<AppInfo> {
    val sharedPreferences = context.getSharedPreferences("app_cache", Context.MODE_PRIVATE)
    val json = sharedPreferences.getString("apps", null)
    return if (json != null) {
        Json.decodeFromString(json)
    } else {
        emptyList()
    }
}

suspend fun reloadApps(
    packageManager: PackageManager,
    context: Context
): Result<List<AppInfo>> {
    return withContext(Dispatchers.IO) {
        try {
            // Fetch the latest app list from the PackageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val apps = packageManager.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
                .mapNotNull { resolveInfo ->
                    resolveInfo.activityInfo?.applicationInfo?.let { appInfo ->
                        AppInfo(
                            name = appInfo.loadLabel(packageManager).toString(),
                            packageName = appInfo.packageName
                        )
                    }
                }

            // Cache the app list in SharedPreferences
            cacheApps(context, apps)

            Result.success(apps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
suspend fun getBackgroundSystemApps(context: Context): List<ApplicationInfo> {
    return withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val launchableApps = pm.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo.applicationInfo.packageName }
            .toSet()

        allApps.filter { app ->
            (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && // system app
                    app.packageName !in launchableApps                  // not launchable
        }
    }
}

fun getKeyboards(context: Context): List<String> {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val enabledMethods = imm.enabledInputMethodList
    return enabledMethods.map { it.packageName }
}

fun formatAppList(packageNames: List<String>, context: Context): String {
    val pm = context.packageManager
    val appNames = packageNames.mapNotNull { packageName ->
        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            null // skip if not found
        }
    }
    return appNames.joinToString(", ")
}