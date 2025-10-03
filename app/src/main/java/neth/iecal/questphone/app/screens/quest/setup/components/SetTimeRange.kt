package neth.iecal.questphone.app.screens.quest.setup.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.QuestInfoState
import nethical.questphone.core.core.utils.formatHour

@Composable
fun SetTimeRange(initialTimeRange: QuestInfoState) {
    var showDialog by remember { mutableStateOf(false) }
    var startHour by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(24) } // 24 represents midnight (12 AM next day)

    var haptic = LocalHapticFeedback.current
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showDialog = true }
    ) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        Text("Perform Between: " + if(startHour == 0 && endHour == 24) "Full Day" else "(${
            formatHour(
                startHour
            )
        } - ${formatHour(endHour)})")
    }

    if (showDialog) {
        TimeRangeDialog(
            initialStartHour = startHour,
            initialEndHour = endHour,
            onDismiss = { showDialog = false },
            onConfirm = { newStart, newEnd ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                startHour = newStart
                endHour = if (newEnd == 0) 24 else newEnd // Midnight fix
                showDialog = false
                initialTimeRange.initialTimeRange = listOf(startHour,endHour)
            }
        )
    }
}


@Composable
fun TimeRangeDialog(
    initialStartHour: Int,
    initialEndHour: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var startHour by remember { mutableStateOf(initialStartHour) }
    var endHour by remember { mutableStateOf(initialEndHour) }
    var haptic = LocalHapticFeedback.current

    // Dialog entrance animation
    val dialogAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "dialogAlpha"
    )
    val dialogScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = Spring.DampingRatioLowBouncy),
        label = "dialogScale"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(startHour, endHour) },
                modifier = Modifier.scale(animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(400, delayMillis = 200),
                    label = "confirmButtonScale"
                ).value)
            ) {
                Text("OK", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.scale(animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(400, delayMillis = 300),
                    label = "dismissButtonScale"
                ).value)
            ) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = dialogScale
                        scaleY = dialogScale
                        alpha = dialogAlpha
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Start Time",
                            fontSize = 16.sp,
                            modifier = Modifier
                                .padding(bottom = 8.dp)

                        )
                        HourPicker(
                            selectedHour = startHour,
                            availableHours = (0..23).toList(),
                            onHourSelected = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                startHour = it
                                if (endHour <= startHour) endHour = startHour + 1
                            }
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "End Time",
                            fontSize = 16.sp,
                            modifier = Modifier
                                .padding(bottom = 8.dp)

                        )
                        HourPicker(
                            selectedHour = endHour,
                            availableHours = ((startHour + 1)..24).toList(),
                            onHourSelected = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                endHour = it }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 6.dp,
        modifier = Modifier.graphicsLayer {
            scaleX = dialogScale
            scaleY = dialogScale
            alpha = dialogAlpha
        }
    )
}

@Composable
fun HourPicker(selectedHour: Int, availableHours: List<Int>, onHourSelected: (Int) -> Unit) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to selected hour when it changes
    LaunchedEffect(selectedHour) {
        val index = availableHours.indexOf(selectedHour)
        if (index >= 0) {
            coroutineScope.launch {
                listState.animateScrollToItem(index, scrollOffset = -60) // Center item
            }
        }
    }

    Box(
        modifier = Modifier
            .height(160.dp)
            .width(90.dp)
    ) {
        LazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 60.dp) // Adjusted for centering
        ) {
            items(availableHours) { hour ->
                val isSelected = hour == selectedHour
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.2f else 1f,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioLowBouncy
                    ),
                    label = "hourScale_$hour"
                )
                val alpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.6f,
                    animationSpec = tween(200),
                    label = "hourAlpha_$hour"
                )

                Text(
                    text =   formatHour(hour),
                    fontSize = if (isSelected) 22.sp else 18.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .clickable {
                            onHourSelected(hour)
                            coroutineScope.launch {
                                val index = availableHours.indexOf(hour)
                                listState.animateScrollToItem(index, scrollOffset = -60)
                            }
                        }
                        .padding(vertical = 6.dp)
                )
            }
        }
    }
}
