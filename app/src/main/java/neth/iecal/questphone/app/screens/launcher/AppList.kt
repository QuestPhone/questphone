package neth.iecal.questphone.app.screens.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import neth.iecal.questphone.app.navigation.LauncherDialogRoutes
import neth.iecal.questphone.app.navigation.RootRoute
import neth.iecal.questphone.app.screens.launcher.dialogs.LauncherDialog


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppList(navController: NavController, viewModel: AppListViewModel) {
    val apps by viewModel.filteredApps.collectAsState()
    val showDialog by viewModel.showCoinDialog.collectAsState()
    val selectedPackage by viewModel.selectedPackage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var textFieldLoaded by remember { mutableStateOf(false) }
    val minutesPer5Coins by viewModel.minutesPerFiveCoins.collectAsState()
    val areHardLockedQuestsAvailable by viewModel.isHardLockedQuestsToday.collectAsState()

    val coins by viewModel.coins.collectAsState()
    val remainingFreePasses by viewModel.remainingFreePassesToday.collectAsState()
    val listState = rememberLazyListState()

    var pulledDownHard by remember { mutableStateOf(false)}
    // NestedScroll to capture overscroll
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // User is scrolling down (pulling) AND at the top
                if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                   if(!pulledDownHard) {
                       pulledDownHard = true
                       viewModel.onSearchQueryChange("")
                       if(textFieldLoaded) {
                           keyboardController?.hide()
                       }
                       navController.navigate(RootRoute.HomeScreen.route){
                           restoreState = true
                       }
                   }
                }
                if (available.y != 0f || available.x != 0f) {
                    if(textFieldLoaded){
                         keyboardController?.hide()
                    }
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(Unit) { viewModel.loadHardLockedQuests() }

    Scaffold { innerPadding ->
        if (showDialog) {
            LauncherDialog(
                coins = coins,
                onDismiss = { viewModel.dismissDialog() },
                pkgName = selectedPackage,
                rootNavController = navController,
                minutesPerFiveCoins = minutesPer5Coins,
                unlockApp = {
                    viewModel.onConfirmUnlockApp(it)
                },
                startDestination = if (areHardLockedQuestsAvailable)
                    LauncherDialogRoutes.ShowAllQuest.route
                        else if (coins >= 5) {
                    LauncherDialogRoutes.UnlockAppDialog.route
                } else {
                    LauncherDialogRoutes.LowCoins.route
                },
                remainingFreePasses = remainingFreePasses,
                onFreePassUsed = { viewModel.useFreePass() },
                areHardLockQuestsPresent = areHardLockedQuestsAvailable
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            LazyColumn(
                modifier = Modifier

                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding)
                    .padding(12.dp),
                contentPadding = innerPadding,
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
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
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
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
                                    textFieldLoaded = true // stop cyclic recompositions
                                    focusRequester.requestFocus()
                                }
                            },
                        singleLine = true
                    )
                    Spacer(Modifier.size(4.dp))
                }
                items(apps) { app ->
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Normal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    viewModel.onAppClick(app.packageName)
                                },
                                onLongClick = { viewModel.onLongAppClick(app.packageName) })
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
}