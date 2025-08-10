package neth.iecal.questphone.app.screens.quest.setup.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import neth.iecal.questphone.data.QuestInfoState
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelector(title:String = "Auto Destroy",onSelected: (String)->Unit) {
    val haptic = LocalHapticFeedback.current
    var showDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val todayMillis = remember { Instant.now().toEpochMilli() }

    val options = listOf<String>("Never", "Select Date")
    val selectedOption = remember { mutableStateOf(options[0]) }

    // Animation states
    val buttonScale by animateFloatAsState(
        targetValue = if (expanded) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        ),
        label = "button_scale"
    )

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "arrow_rotation"
    )

    val buttonColor by animateColorAsState(
        targetValue = if (expanded)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.primary,
        animationSpec = tween(durationMillis = 200),
        label = "button_color"
    )

    val textColor by animateColorAsState(
        targetValue = if (expanded)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onPrimary,
        animationSpec = tween(durationMillis = 200),
        label = "text_color"
    )

    Column {
        // Animated Button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .scale(buttonScale)
                .clip(RoundedCornerShape(8.dp)),
            shadowElevation = if (expanded) 8.dp else 4.dp,
            tonalElevation = if (expanded) 4.dp else 0.dp
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    expanded = !expanded },
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonColor,
                    contentColor = textColor
                ),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$title: " + if (selectedOption.value == options[0]) "Never" else selectedDate,
                        modifier = Modifier.padding(end = 24.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dropdown arrow",
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .rotate(arrowRotation)
                    )
                }
            }
        }

        // Animated DropdownMenu
        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically(
                initialOffsetY = { -it / 2 },
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 400f
                )
            ) + expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 400f
                )
            ) + fadeIn(
                animationSpec = tween(
                    durationMillis = 200,
                    delayMillis = 50
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it / 2 },
                animationSpec = spring(
                    dampingRatio = 0.9f,
                    stiffness = 500f
                )
            ) + shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = spring(
                    dampingRatio = 0.9f,
                    stiffness = 500f
                )
            ) + fadeOut(
                animationSpec = tween(durationMillis = 150)
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(8.dp)),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp)
                    )
                ) {
                    options.forEachIndexed { index, option ->

                        val itemScale by animateFloatAsState(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 200,
                                delayMillis = index * 50
                            ),
                            label = "item_scale_$index"
                        )

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .scale(itemScale)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                expanded = false
                                selectedOption.value = option
                                if (option == "Select Date") {
                                    showDialog = true
                                } else {
                                    onSelected("9999-06-21")
                                }
                            },
                            color = Color.Transparent
                        ) {
                            Text(
                                text = option,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // Animated DatePicker Dialog
    AnimatedVisibility(
        visible = showDialog,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 300)
        ) + slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 300f
            )
        ),
        exit = fadeOut(
            animationSpec = tween(durationMillis = 200)
        ) + slideOutVertically(
            targetOffsetY = { it / 4 },
            animationSpec = spring(
                dampingRatio = 0.9f,
                stiffness = 400f
            )
        )
    ) {

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                val buttonScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.6f,
                        stiffness = 300f
                    ),
                    label = "confirm_button_scale"
                )

                TextButton(
                    onClick = {
                        showDialog = false
                        onSelected(
                            selectedDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")).toString())
                    },
                    modifier = Modifier.scale(buttonScale)
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                val buttonScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.6f,
                        stiffness = 300f
                    ),
                    label = "dismiss_button_scale"
                )

                TextButton(
                    onClick = { showDialog = false },
                    modifier = Modifier.scale(buttonScale)
                ) {
                    Text("Cancel")
                }
            }
        ) {

            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = todayMillis,
                // Set the minimum selectable date to today
                initialDisplayedMonthMillis = todayMillis,
                yearRange = (LocalDate.now().year)..(LocalDate.now().year + 10)
            )
            DatePicker(
                state = datePickerState,
                showModeToggle = true
            )

            LaunchedEffect(datePickerState.selectedDateMillis) {
                datePickerState.selectedDateMillis?.let { millis ->
                    selectedDate = LocalDate.ofEpochDay(millis / 86_400_000)
                }
            }
        }
    }
}

@Composable
fun AutoDestruct(questInfoState: QuestInfoState){
    DateSelector {
        questInfoState.initialAutoDestruct = it
    }
}