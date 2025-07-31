package neth.iecal.questphone.ui.screens.launcher

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import neth.iecal.questphone.core.services.AppBlockerService
import neth.iecal.questphone.core.services.AppBlockerServiceInfo
import neth.iecal.questphone.core.services.INTENT_ACTION_UNLOCK_APP
import neth.iecal.questphone.core.utils.managers.getCachedApps
import neth.iecal.questphone.core.utils.managers.reloadApps
import neth.iecal.questphone.data.AppInfo
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.useCoins
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak")
    private val context = application.applicationContext

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps: StateFlow<List<AppInfo>> = _filteredApps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _showCoinDialog = MutableStateFlow(false)
    val showCoinDialog = _showCoinDialog.asStateFlow()

    private val _selectedPackage = MutableStateFlow("")
    val selectedPackage = _selectedPackage.asStateFlow()

    private val _distractions = MutableStateFlow<Set<String>>(emptySet())
    val distractions = _distractions.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    var minutesPerFiveCoins = MutableStateFlow(10)
        private set

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val prefs = context.getSharedPreferences("minutes_per_5", Context.MODE_PRIVATE)
            minutesPerFiveCoins.value = prefs.getInt("minutes_per_5", 10)

            val cached = getCachedApps(context)
            if (cached.isNotEmpty()) {
                _apps.value = cached
                _filteredApps.value = cached
                _isLoading.value = false
            }

            withContext(Dispatchers.IO) {
                reloadApps(context.packageManager, context).onSuccess {
                    _apps.value = it
                    _filteredApps.value = it
                    _isLoading.value = false
                }.onFailure {
                    _error.value = it.message
                    _isLoading.value = false
                }
            }

            val sp = context.getSharedPreferences("distractions", Context.MODE_PRIVATE)
            _distractions.value = sp.getStringSet("distracting_apps", emptySet()) ?: emptySet()
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        _filteredApps.value = _apps.value.filter { it.name.contains(query, ignoreCase = true) }
    }

    fun onAppClick(packageName: String) {
        val cooldownUntil = AppBlockerServiceInfo.unlockedApps[packageName] ?: 0L
        val isDistraction = _distractions.value.contains(packageName)

        if (isDistraction && (cooldownUntil == -1L || System.currentTimeMillis() > cooldownUntil)) {
            _selectedPackage.value = packageName
            _showCoinDialog.value = true
        } else {
            launchApp(context, packageName)
            onSearchQueryChange("")
        }
    }

    fun onLongAppClick(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun onConfirmUnlockApp(coins: Int) {
        val cooldownTime = (minutesPerFiveCoins.value * coins) * 60_000L
        val pkg = _selectedPackage.value
        val intent = Intent().apply {
            action = INTENT_ACTION_UNLOCK_APP
            putExtra("selected_time", cooldownTime)
            putExtra("package_name", pkg)
        }
        context.sendBroadcast(intent)

        if (!AppBlockerServiceInfo.isUsingAccessibilityService &&
            AppBlockerServiceInfo.appBlockerService == null
        ) {
            startForegroundService(context, Intent(context, AppBlockerService::class.java))
            AppBlockerServiceInfo.unlockedApps[pkg] = System.currentTimeMillis() + cooldownTime
        }

        User.useCoins(5)
        launchApp(context, pkg)
        _showCoinDialog.value = false
    }

    fun dismissDialog() {
        _showCoinDialog.value = false
    }
}

fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    intent?.let { context.startActivity(it) }
}
