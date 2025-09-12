package neth.iecal.questphone.app.screens.launcher.widget

import android.appwidget.AppWidgetHost
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.awaitCancellation
import neth.iecal.questphone.app.navigation.RootRoute
import neth.iecal.questphone.app.screens.launcher.widget.picker.PersistedWidget
import neth.iecal.questphone.app.screens.launcher.widget.picker.WidgetViewModel

@Composable
fun WidgetScreen(
    navController: NavController,
    viewModel: WidgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isWidgetManagement by remember { mutableStateOf(false) }

    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    BackHandler(isWidgetManagement) { isWidgetManagement = false }
    ProvideAppWidgetHost {
        if (!isWidgetManagement) {
            val context = LocalContext.current
            val appWidgetHost = LocalAppWidgetHost.current

            val listState = rememberLazyListState()
            var pulledDownHard by remember { mutableStateOf(false) }
            // NestedScroll to capture overscroll
            val nestedScrollConnection = remember {
                object : NestedScrollConnection {
                    override fun onPreScroll(
                        available: Offset,
                        source: NestedScrollSource
                    ): Offset {
                        // User is scrolling down (pulling) AND at the top
                        if (available.y > 0 && listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) {
                            if (!pulledDownHard) {
                                pulledDownHard = true
                                navController.navigate(RootRoute.HomeScreen.route) {
                                    restoreState = true
                                }
                            }
                        }
                        return Offset.Zero
                    }
                }
            }
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    Column(
                        modifier = Modifier.padding(padding).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        when {
                            uiState.isLoading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }

                            uiState.widgets.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No widgets added yet",
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                isWidgetManagement = true
                                            }
                                        ) {
                                            Text("Add Your First Widget")
                                        }
                                    }
                                }
                            }

                            else -> {

                                LazyColumn(
                                    modifier = Modifier
                                        .consumeWindowInsets(padding),
                                    state = listState,
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    items(uiState.widgets) { persistedWidget ->
                                        WidgetCard(
                                            persistedWidget = persistedWidget,
                                        )
                                    }
                                    item {

                                        if (uiState.widgets.isNotEmpty()) {
                                            Button(
                                                onClick = {
                                                    isWidgetManagement = true
                                                }) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit Widgets"
                                                )
                                                Spacer(Modifier.size(8.dp))
                                                Text("Edit Widgets")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }else{
            WidgetManagementScreen(viewModel)
        }

    }
}
@Composable
private fun WidgetCard(
    persistedWidget: PersistedWidget,
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(persistedWidget.config.width?.dp ?: 200.dp)
                .height(persistedWidget.config.height.dp)
        ) {
            AppWidgetHost(
                widgetInfo = persistedWidget.widgetInfo,
                widgetId = persistedWidget.widgetId,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

val LocalAppWidgetHost =
    staticCompositionLocalOf<AppWidgetHost> { error("AppWidgetHost not provided") }

@Composable
fun ProvideAppWidgetHost(content: @Composable () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val widgetHost = remember { AppWidgetHost(context.applicationContext, 44203) }

    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            widgetHost.startListening()
            try {
                awaitCancellation()
            } finally {
                try {
                    widgetHost.stopListening()
                } catch (e: Exception) {
                    Log.e("WidgetHost", "Error stopping listener", e)
                }
            }
        }
    }

    CompositionLocalProvider(LocalAppWidgetHost provides widgetHost, content = content)
}