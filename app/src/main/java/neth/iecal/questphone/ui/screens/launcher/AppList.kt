package neth.iecal.questphone.ui.screens.launcher

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.net.toUri
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import neth.iecal.questphone.data.AppInfo
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.useCoins
import neth.iecal.questphone.services.AppBlockerService
import neth.iecal.questphone.services.INTENT_ACTION_UNLOCK_APP
import neth.iecal.questphone.services.ServiceInfo
import neth.iecal.questphone.ui.screens.launcher.components.CoinDialog
import neth.iecal.questphone.ui.screens.launcher.components.LowCoinsDialog
import neth.iecal.questphone.utils.getCachedApps
import neth.iecal.questphone.utils.reloadApps


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppList(navController: NavController) {
    val context = LocalContext.current

    var appsState by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var filteredAppState by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorState by remember { mutableStateOf<String?>(null) }

    var showCoinDialog by remember { mutableStateOf(false) }
    var selectedPackage by remember { mutableStateOf("") }

    var distractions by remember { mutableStateOf(emptySet<String>()) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery by remember { mutableStateOf("") }

    var textFieldLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        filteredAppState =
            appsState.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    LaunchedEffect(Unit) {
        val cachedApps = getCachedApps(context)
        if (cachedApps.isNotEmpty()) {
            appsState = cachedApps
            isLoading = false
        }
        val packageManager = context.packageManager
        withContext(Dispatchers.IO) {
            reloadApps(packageManager, context)
                .onSuccess { apps ->
                    appsState = apps
                    filteredAppState = appsState
                    isLoading = false
                }
                .onFailure { error ->
                    errorState = error.message
                    isLoading = false
                }
        }
        val sp = context.getSharedPreferences("distractions", Context.MODE_PRIVATE)
        distractions = sp.getStringSet("distracting_apps", emptySet<String>()) ?: emptySet()

    }


    Scaffold { innerPadding ->
        if (showCoinDialog) {
            if (User.userInfo.coins > 5) {
                CoinDialog(
                    coins = User.userInfo.coins,
                    onDismiss = { showCoinDialog = false },
                    onConfirm = {
                        val cooldownTime = 10 * 60_000
                        val intent = Intent().apply {
                            action = INTENT_ACTION_UNLOCK_APP
                            putExtra("selected_time", cooldownTime)
                            putExtra("package_name", selectedPackage)
                        }
                        context.sendBroadcast(intent)
                        if (!ServiceInfo.isUsingAccessibilityService && ServiceInfo.appBlockerService == null) {
                            startForegroundService(
                                context,
                                Intent(context, AppBlockerService::class.java)
                            )
                            ServiceInfo.unlockedApps[selectedPackage] =
                                System.currentTimeMillis() + cooldownTime
                        }
                        User.useCoins(5)
                        launchApp(context, selectedPackage)
                        showCoinDialog = false
                    },
                    pkgName = selectedPackage
                )
            } else {
                LowCoinsDialog(
                    coins = User.userInfo.coins,
                    onDismiss = { showCoinDialog = false },
                    navController = navController,
                    pkgName = selectedPackage
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding)
                .padding(12.dp),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Apps") },
                    placeholder = { Text("Type app name...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search"
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onGloballyPositioned {
                            if (!textFieldLoaded) {
                                focusRequester.requestFocus()
                                textFieldLoaded = true // stop cyclic recompositions
                            }
                        },
                    singleLine = true
                )
                Spacer(Modifier.size(4.dp))
            }
            items(filteredAppState) { app ->
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                val packageName = app.packageName
                                if (distractions.contains(packageName)) {
                                    val cooldownUntil = ServiceInfo.unlockedApps[packageName] ?: 0L
                                    if (cooldownUntil == -1L || System.currentTimeMillis() > cooldownUntil) {
                                        // Not under cooldown - show dialog
                                        showCoinDialog = true
                                        selectedPackage = packageName
                                    } else {
                                        launchApp(context, packageName)
                                    }
                                } else {
                                    // Not a distraction - launch directly
                                    launchApp(context, packageName)
                                }
                            },
                            onLongClick = {
                                val intent =
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = "package:${app.name}".toUri()
                                    }
                                context.startActivity(intent)
                            })
                )
            }
            item {
                Spacer(
                    Modifier.windowInsetsBottomHeight(
                        WindowInsets.systemBars
                    )
                )
            }
        }
    }
}
fun launchApp(context:Context, packageName: String){
    val intent =
        context.packageManager.getLaunchIntentForPackage(
            packageName
        )
    intent?.let { context.startActivity(it) }
}