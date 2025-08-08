package neth.iecal.questphone.app.screens.quest.setup.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import neth.iecal.questphone.data.QuestInfoState
import nethical.questphone.data.DayOfWeek

@Composable
fun SelectDaysOfWeek(
    baseQuest: QuestInfoState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Choose Days",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Select the days on which you want to perform this quest.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DayOfWeek.entries.forEach { day ->
                AddictiveDayButton(
                    day = day,
                    isSelected = day in baseQuest.selectedDays,
                    onSelected = { selected ->
                        baseQuest.selectedDays = if (selected) {
                            baseQuest.selectedDays + day
                        } else {
                            baseQuest.selectedDays - day
                        }
                    }
                )
            }
        }
    }
}


@Composable
private fun AddictiveDayButton(
    day: DayOfWeek,
    isSelected: Boolean,
    onSelected: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var pulseCount by remember { mutableStateOf(0) }
    var showRipple by remember { mutableStateOf(false) }

    // ADDICTIVE COLOR ANIMATION - super smooth and satisfying
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 800f
        ),
        label = "backgroundColor"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 800f
        ),
        label = "contentColor"
    )


    val elevation by animateDpAsState(
        targetValue = when {
            showRipple -> 16.dp  // MASSIVE elevation during ripple
            isSelected -> 12.dp
            else -> 6.dp
        },
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 1000f
        ),
        label = "elevation"
    )

    val pulseScale by animateFloatAsState(
        targetValue = if (pulseCount > 0) 1.8f else 1f,
        animationSpec = spring(
            dampingRatio = 0.3f,
            stiffness = 800f
        ),
        label = "pulseScale"
    )

    val rippleScale by animateFloatAsState(
        targetValue = if (showRipple) 2.5f else 1f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 400f
        ),
        label = "rippleScale"
    )

    val rippleAlpha by animateFloatAsState(
        targetValue = if (showRipple) 0f else 0.3f,
        animationSpec = tween(durationMillis = 600),
        label = "rippleAlpha"
    )


    LaunchedEffect(pulseCount) {
        if (pulseCount > 0) {
            delay(200)
            pulseCount = 0
        }
    }

    LaunchedEffect(showRipple) {
        if (showRipple) {
            delay(600)
            showRipple = false
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(50.dp)
    ) {
        // RIPPLE EFFECT BACKGROUND
        Surface(
            color = backgroundColor.copy(alpha = rippleAlpha),
            shape = CircleShape,
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = rippleScale
                    scaleY = rippleScale
                }
        ) {}

        // PULSE BACKGROUND
        Surface(
            color = backgroundColor.copy(alpha = 0.4f),
            shape = CircleShape,
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    alpha = if (pulseCount > 0) 0.6f else 0f
                }
        ) {}

        // MAIN BUTTON - the star of the show
        Surface(
            onClick = {
                // TRIPLE HAPTIC FEEDBACK - feels amazing
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)

                pulseCount++
                showRipple = true

                onSelected(!isSelected)
            },
            color = backgroundColor,
            contentColor = contentColor,
            shape = CircleShape,
            tonalElevation = elevation,
            shadowElevation = elevation,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)

        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = day.name.first().toString(),
                    fontSize = if (isSelected) 16.sp else 14.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                )
            }
        }
    }
}