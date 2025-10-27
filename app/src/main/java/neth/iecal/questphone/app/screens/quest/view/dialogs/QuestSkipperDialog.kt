package neth.iecal.questphone.app.screens.quest.view.dialogs

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import neth.iecal.questphone.app.screens.quest.view.ViewQuestVM
import nethical.questphone.core.core.utils.VibrationHelper
import nethical.questphone.data.game.InventoryItem
import kotlin.math.roundToInt

@Composable
fun QuestSkipperDialog(viewModel: ViewQuestVM) {
    val isDialogVisible by viewModel.isQuestSkippedDialogVisible.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun getAntiQuitMessage(context: Context, progress: Int): String {
        val resId = context.resources.getIdentifier("antiquit_$progress", "string", context.packageName)
        if (resId != 0) return context.getString(resId)

        val fallback = listOf(
            99, 98, 97, 96, 93, 90, 85, 80, 75, 70, 65,
            60, 55, 50, 45, 40, 35, 30, 25, 20, 15,
            10, 9, 8, 7, 6, 5, 4, 3, 2, 1
        ).firstOrNull { progress >= it }?.let {
            val id = context.resources.getIdentifier("antiquit_$it", "string", context.packageName)
            if (id != 0) context.getString(id) else null
        }

        return fallback ?: "Stay strong. You're almost there."
    }

    var isPressed by remember { mutableStateOf(false) }
    var pressProgress by remember { mutableFloatStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = pressProgress,
        animationSpec = tween(durationMillis = 50),
        label = "pressProgress"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            val holdTime = 1500L
            val startTime = System.currentTimeMillis()
            while (isPressed) {
                val elapsed = System.currentTimeMillis() - startTime
                pressProgress = (elapsed / holdTime.toFloat()).coerceIn(0f, 1f)

                if (pressProgress >= 1f) {
                    VibrationHelper.vibrate(200)
                    viewModel.isQuestSkippedDialogVisible.value = false
                    viewModel.useItem(InventoryItem.QUEST_SKIPPER) {
                        viewModel.saveMarkedQuestToDb()
                    }
                    isPressed = false
                    pressProgress = 0f
                    break
                }
                delay(16)
            }
        } else {
            pressProgress = 0f
        }
    }

    if (isDialogVisible) {
        Dialog(
            onDismissRequest = { /* Prevent accidental closing */ },
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 6.dp,
                color = Color(0xFF0D0D0D),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val message = if ((progress * 100).roundToInt() != 0)
                        getAntiQuitMessage(context, progress.roundToInt())
                    else
                        "You opened the app, stared at the button, and decided to quit? Legendary move 😭"

                    Text(
                        text = message,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Quest Skippers left: ${viewModel.getInventoryItemCount(InventoryItem.QUEST_SKIPPER)}",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hold to Quit Button
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1E1E1E))
                                .pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown()
                                        isPressed = true
                                        VibrationHelper.vibrate(50)

                                        waitForUpOrCancellation()
                                        isPressed = false
                                    }
                                }
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Progress background
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .height(48.dp)
                                    .background(
                                        Color(0xFFFF4444).copy(alpha = 0.6f),
                                        RoundedCornerShape(16.dp)
                                    )
                            )

                            // Text
                            Text(
                                text = "Hold to Skip",
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        TextButton(
                            onClick = {
                                VibrationHelper.vibrate(100)
                                viewModel.isQuestSkippedDialogVisible.value = false
                            },
                        ) {
                            Text("X")
                        }
                    }
                }
            }
        }
    }
}