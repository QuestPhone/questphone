package neth.iecal.questphone.app.screens.launcher.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import neth.iecal.questphone.app.screens.launcher.widget.picker.PersistedWidget
import neth.iecal.questphone.app.screens.launcher.widget.picker.WidgetViewModel
import nethical.questphone.core.core.utils.managers.isSetToDefaultLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetManagementScreen(
    viewModel: WidgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    val context = LocalContext.current
    val appWidgetHost = LocalAppWidgetHost.current
    val appWidgetManager = AppWidgetManager.getInstance(context)

    val configureWidgetLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val appWidgetId =
                    result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                        ?: return@rememberLauncherForActivityResult
                val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
                if (info != null) {
                    viewModel.addWidget(appWidgetId, info)
                }
            }
        }

    val pickWidgetLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val appWidgetId = result.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
                if (appWidgetId != -1) {
                    val info = appWidgetManager.getAppWidgetInfo(appWidgetId)
                    if (info != null) {
                        viewModel.addWidget(appWidgetId, info)
                    }
                }
            }
        }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Widget Management",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if(isSetToDefaultLauncher(context)) {
                        val appWidgetId = appWidgetHost.allocateAppWidgetId()
                        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        pickWidgetLauncher.launch(pickIntent)
                    }else{
                        Toast.makeText(context,"Please set QuestPhone as your default launcher first",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Widget"
                )
            }
        }
    ) { padding ->
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No widgets added yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tap the + button to add your first widget",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    itemsIndexed(
                        items = uiState.widgets,
                        key = { _, widget -> widget.widgetId } // important for stable animations
                    ) { index, persistedWidget ->
                        WidgetManagementCard(
                            persistedWidget = persistedWidget,
                            index = index,
                            totalCount = uiState.widgets.size,
                            onMoveUp = {
                                if (index > 0) {
                                    viewModel.reorderWidgets(index, index - 1)
                                }
                            },
                            onMoveDown = {
                                if (index < uiState.widgets.size - 1) {
                                    viewModel.reorderWidgets(index, index + 1)
                                }
                            },
                            onDelete = { viewModel.removeWidget(persistedWidget.widgetId) },
                            onSizeChange = { width, height ->
                                val updatedConfig = persistedWidget.config.copy(
                                    width = width,
                                    height = height
                                )
                                viewModel.updateWidgetConfig(
                                    persistedWidget.widgetId,
                                    updatedConfig
                                )
                                Log.d("updated widget info", persistedWidget.toString())
                            },
                            modifier =  Modifier.animateItem(
                                placementSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                fadeInSpec = tween(180),
                                fadeOutSpec = tween(180)
                            )
                        )
                    }
                }

            }
        }
    }
}
@Composable
private fun WidgetManagementCard(
    persistedWidget: PersistedWidget,
    index: Int,
    totalCount: Int,
    modifier: Modifier,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onSizeChange: (Int, Int) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val width = remember { mutableStateOf(persistedWidget.config.width?.dp ?: 200.dp) }
    val height = remember { mutableStateOf(persistedWidget.config.height?.dp ?: 150.dp) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with position controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Widget info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = persistedWidget.widgetInfo.label ?: "Unknown Widget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Position: ${index + 1} of $totalCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${width.value.value.toInt()} × ${height.value.value.toInt()} dp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Controls
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Move up button
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                        )
                    }

                    // Move down button
                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                        )
                    }

                    // Delete button
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Widget",
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Size controls toggle
            TextButton(
                onClick = {
                    isExpanded = !isExpanded
                    onSizeChange(width.value.value.toInt(),height.value.value.toInt())
                          },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isExpanded) "Save Size" else "Show Size Controls",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Size controls
            if (isExpanded) {
              ResizableBox(
                    width = width,
                    height = height
                ) {
                    AppWidgetHost(
                        widgetInfo = persistedWidget.widgetInfo,
                        widgetId = persistedWidget.widgetId,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ResizableBox(
    modifier: Modifier = Modifier,
    width: MutableState<Dp> ,
    height: MutableState<Dp>,
    minWidth: Dp = 80.dp,
    minHeight: Dp = 80.dp,
    content: @Composable BoxScope.() -> Unit
) {
    var resizing by remember { mutableStateOf(true) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current

    // Helper to convert px → dp
    fun Float.pxToDp(): Dp = with(density) { this@pxToDp.toDp() }


    Box(
        modifier = modifier
            .width(width.value)
            .height(height.value)
            .border(1.dp, if (resizing) primaryColor else Color.Transparent)
    ) {
        content()

        if (resizing) {
            val handleSize = 20.dp

            fun Modifier.resizeHandle(
                onDrag: (Offset) -> Unit
            ) = this
                .size(handleSize)
                .background(primaryColor, shape = CircleShape)
                .border(1.dp, primaryColor, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                }

            // Corner handles
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .resizeHandle { drag ->
                        width.value = (width.value - drag.x.pxToDp()).coerceAtLeast(minWidth)
                        height.value = (height.value - drag.y.pxToDp()).coerceAtLeast(minHeight)
                    }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .resizeHandle { drag ->
                        width.value = (width.value + drag.x.pxToDp()).coerceAtLeast(minWidth)
                        height.value = (height.value - drag.y.pxToDp()).coerceAtLeast(minHeight)
                    }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .resizeHandle { drag ->
                        width.value = (width.value - drag.x.pxToDp()).coerceAtLeast(minWidth)
                        height.value = (height.value + drag.y.pxToDp()).coerceAtLeast(minHeight)
                    }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .resizeHandle { drag ->
                        width.value = (width.value + drag.x.pxToDp()).coerceAtLeast(minWidth)
                        height.value = (height.value + drag.y.pxToDp()).coerceAtLeast(minHeight)
                    }
            )

            // Edge handles
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .resizeHandle { drag ->
                        height.value = (height.value - drag.y.pxToDp()).coerceAtLeast(minHeight)
                    }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .resizeHandle { drag ->
                        height.value = (height.value + drag.y.pxToDp()).coerceAtLeast(minHeight)
                    }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .resizeHandle { drag ->
                        width.value  = (width.value - drag.x.pxToDp()).coerceAtLeast(minWidth)
                    }
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .resizeHandle { drag ->
                        width.value = (width.value + drag.x.pxToDp()).coerceAtLeast(minWidth)
                    }
            )
        }
    }
}
