package neth.iecal.questphone.ui.screens.pet

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import neth.iecal.questphone.ui.screens.components.NeuralNetworkCanvas
import neth.iecal.questphone.ui.theme.JetBrainMono
import kotlin.random.Random

@Composable
fun TheSystemDialog(){
    var isDialogVisible by remember { mutableStateOf(true) }

    fun onDismiss(){
        isDialogVisible = false
    }

    val systemText = "Hello I am system, a personalised assistant designed to help you crush your goals"
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        val scale = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 500,
                    easing = LinearEasing
                )
            )
        }
        Surface(
            modifier = Modifier
                .safeDrawingPadding()
                .padding(16.dp)
                .animateContentSize( // <- 👈 This is the magic line
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = FastOutSlowInEasing
                    )
                )
                .graphicsLayer(
                    scaleY = scale.value,
                    transformOrigin = TransformOrigin.Center
                )

                .fillMaxWidth(),
            tonalElevation = 3.dp,
            shape = RoundedCornerShape(12.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                NeuralNetworkCanvas(Modifier.size(200.dp))
                Spacer(Modifier.size(4.dp))
                BadassTypingText(systemText)
                Spacer(Modifier.size(12.dp))
            }
        }

    }
}

@Composable
fun BadassTypingText(
    fullText: String,
    modifier: Modifier = Modifier,
    typingSpeed: Long = 50L,
    glitchChance: Float = 0.1F, // chance to show glitch char
    cursorBlinkSpeed: Long = 400L
) {
    var visibleText by remember { mutableStateOf("") }
    var showCursor by remember { mutableStateOf(true) }
    val haptics = LocalHapticFeedback.current
    val randomChars = listOf('|', '/', '\\', '_', '-', '!', '@', '#', '$', '%', '^', '&')

    LaunchedEffect(fullText) {
        withContext(Dispatchers.Main) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        visibleText = ""
        for (i in fullText.indices) {

            if (Random.nextFloat() < glitchChance) {
                visibleText += randomChars.random()
                delay(typingSpeed / 2)
                visibleText = visibleText.dropLast(1)
            }

            visibleText += fullText[i]
            if (i % 10 == 0 && i != 0) {
                withContext(Dispatchers.Main) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            delay(typingSpeed)

        }
    }

    // Blinking cursor
    LaunchedEffect(Unit) {
        while (true) {
            showCursor = !showCursor
            delay(cursorBlinkSpeed)
        }
    }

    Text(
        text = buildAnnotatedString {
            append(visibleText)
            if (showCursor && visibleText.length != fullText.length) {
                withStyle(SpanStyle(color = Color.White)) {
                    append("|")
                }
            }
        },
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainMono, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.5.sp),
        modifier = modifier.shadow(8.dp),
        lineHeight = 24.sp
    )
}
