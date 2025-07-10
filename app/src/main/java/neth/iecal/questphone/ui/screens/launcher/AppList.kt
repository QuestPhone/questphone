package neth.iecal.questphone.ui.screens.launcher

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val haptic = LocalHapticFeedback.current
        AppListWithScrollbar(
            groupedApps = groupedAppsState.value,
            isLoading = isLoading.value,
            error = errorState.value,
            innerPadding = innerPadding,
            onAppClick = { packageName ->
                if (distractions.contains(packageName)) {
                    val cooldownUntil = ServiceInfo.unlockedApps[packageName] ?:0L
                    if (cooldownUntil==-1L || System.currentTimeMillis() > cooldownUntil) {
                        // Not under cooldown - show dialog
                        showCoinDialog.value = true
                        selectedPackage.value = packageName
                    } else {
                        launchApp(context, packageName)
                    }
                } else {
                    // Not a distraction - launch directly
                    launchApp(context, packageName)
                }
            },
            searchBar = {
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
            },
            onBackFromDrawer = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                navController.popBackStack()
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListWithScrollbar(
    groupedApps: List<AppGroup>,
    isLoading: Boolean,
    error: String?,
    innerPadding: PaddingValues,
    onAppClick: (String) -> Unit,
    searchBar: @Composable () -> Unit,
    onBackFromDrawer: () -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val overscrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Detect downward pull at top
                if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                    onBackFromDrawer()
                    return Offset.Zero
                }
                return super.onPreScroll(available, source)
            }
        }
    }

    // Map to store the starting index of each letter group
    val groupPositions = remember(groupedApps) {
        mutableMapOf<Char, Int>().apply {
            var currentIndex = 0
            groupedApps.forEach { group ->
                this[group.letter] = currentIndex
                currentIndex += 1 + group.apps.size // 1 for header + number of apps
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(overscrollConnection)
            .padding(innerPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            // Main app list
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                when {
                    isLoading -> {
                        Text(
                            text = "Loading apps...",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    error != null -> {
                        Text(
                            text = "Error: $error",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    else -> {
                        LazyColumn(state = listState) {
                            item { searchBar() }

                            groupedApps.forEach { group ->
                                stickyHeader {
                                    Text(
                                        text = group.letter.toString(),
                                        style = MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .background(MaterialTheme.colorScheme.surface)
                                    )
                                }
                                items(group.apps) { app ->
                                    AppItem(
                                        name = app.name,
                                        packageName = app.packageName,
                                        onAppPressed = onAppClick
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Minimal scrollbar
            if (!isLoading && error == null && groupedApps.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .width(24.dp)
                        .fillMaxHeight()
                        .padding(vertical = 16.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    groupedApps.forEach { group ->
                        Text(
                            text = group.letter.toString(),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    coroutineScope.launch {
                                        val targetIndex = groupPositions[group.letter] ?: 0
                                        listState.scrollToItem(targetIndex)
                                    }
                                }
                        )
                    }
                }
            }
        }
    }


}

private fun groupAppsByLetter(apps: List<AppInfo>): List<AppGroup> {
    return apps
        .sortedBy { it.name }
        .groupBy { it.name.first().uppercaseChar() }
        .map { (letter, apps) -> AppGroup(letter, apps) }
}

private suspend fun loadInitialApps(
    appsState: MutableState<List<AppInfo>>,
    isLoading: MutableState<Boolean>,
    context: Context
) {
    val cachedApps = getCachedApps(context)
    if (cachedApps.isNotEmpty()) {
        appsState.value = cachedApps
        isLoading.value = false
    }
}

private fun launchApp(context: Context, packageName: String) {
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)
    intent?.let { context.startActivity(it) }
}
