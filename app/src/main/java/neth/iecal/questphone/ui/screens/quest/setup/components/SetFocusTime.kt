package neth.iecal.questphone.ui.screens.quest.setup.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nethical.questphone.data.quest.focus.FocusTimeConfig

@Composable
fun SetFocusTimeUI(
    focusTime: MutableState<FocusTimeConfig>
) {
//
//    Text(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(top = 16.dp),
//        text = "Duration",
//        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
//    )
    TimeInputRow(
        label = "Initial Focus Time",
        description = "Starting duration for your focus sessions",
        time = focusTime.value.initialTime,
        unit = focusTime.value.initialUnit,
        onUpdate = { value, unit ->
            val initial = convertToMinutes(value, unit)
            val goal = convertToMinutes(focusTime.value.finalTime, focusTime.value.finalUnit)
            if (initial in 0..goal) {
                focusTime.value = focusTime.value.copy(initialTime = value, initialUnit = unit)
            }
        }
    )

    TimeInputRow(
        label = "Increment Daily by",
        description = "How much to increase each day",
        time = focusTime.value.incrementTime,
        unit = focusTime.value.incrementUnit,
        availableUnits = listOf("m"),
        onUpdate = { value, unit ->
            focusTime.value = focusTime.value.copy(incrementTime = value, incrementUnit = unit)
        }
    )

    TimeInputRow(
        label = "Goal Focus Time",
        description = "Target duration to build up to",
        time = focusTime.value.finalTime,
        unit = focusTime.value.finalUnit,
        onUpdate = { value, unit ->
            val initial = convertToMinutes(focusTime.value.initialTime, focusTime.value.initialUnit)
            val goal = convertToMinutes(value, unit)
            if (goal >= initial) {
                focusTime.value = focusTime.value.copy(finalTime = value, finalUnit = unit)
            }
        }
    )
}

@Composable
fun TimeInputRow(
    label: String,
    description: String = "",
    time: String,
    unit: String,
    onUpdate: (String, String) -> Unit,
    availableUnits: List<String> = listOf("h", "m")
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        if (description.isNotEmpty()) {
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        TimeUnitSelector(
            selectedUnit = unit,
            onSelect = { onUpdate(time, it) },
            units = availableUnits,
            time = time,
            onUpdate = onUpdate,
            unit = unit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TimeUnitSelector(
    selectedUnit: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    time: String,
    unit: String,
    onUpdate: (String, String) -> Unit,
    units: List<String>
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedCard(
            modifier = Modifier.weight(0.4f)
        ) {
            BasicTextField(
                value = time,
                onValueChange = { newValue ->
                    // Only accept digits
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        onUpdate(newValue, unit)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp)
                    .background(MaterialTheme.colorScheme.surface),
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (time.isEmpty()) {
                            Text(
                                text = "0",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }

        Row(
            modifier = Modifier
                .weight(0.6f)
                .padding(start = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            units.forEach { currentUnit ->
                ElevatedButton(
                    onClick = { onSelect(currentUnit) },
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (selectedUnit == currentUnit)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedUnit == currentUnit)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text(
                        text = if (currentUnit == "h") "Hours" else "Minutes",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selectedUnit == currentUnit) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * Converts time to minutes based on the unit ("h" or "m").
 */
fun convertToMinutes(value: String, unit: String): Int {
    val time = value.toIntOrNull() ?: return 0
    return if (unit == "h") time * 60 else time
}