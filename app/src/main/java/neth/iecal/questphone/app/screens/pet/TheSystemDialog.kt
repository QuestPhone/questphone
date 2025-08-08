package neth.iecal.questphone.app.screens.pet

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.delay
import nethical.questphone.data.game.StoryNode
import nethical.questphone.data.game.introductionStory
import neth.iecal.questphone.app.screens.components.NeuralMeshSymmetrical
import neth.iecal.questphone.app.theme.JetBrainMono
import kotlin.random.Random

@Composable
fun TheSystemDialog() {
    val context = LocalContext.current
    var isDialogVisible by remember { mutableStateOf(false) }


    var currentNode by remember { mutableStateOf<StoryNode?>(introductionStory["welcome"]) }
    var currentStory = remember { mutableStateOf<Map<String,StoryNode>>(mapOf()) }

    val isFullTextShown = remember { mutableStateOf(false) }

    // Animation for options fade-in
    val optionsAlpha by animateFloatAsState(
        targetValue = if (isFullTextShown.value) 1f else 0f,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        label = "optionsAlpha"
    )

    LaunchedEffect(Unit) {
        val sp = context.getSharedPreferences("systemDialog",Context.MODE_PRIVATE)
        val isIntroDone = sp.getBoolean("isIntroDone",false)
//        if(!isIntroDone){
//            isDialogVisible = true
//            currentStory.value = introductionStory
//            currentNode = introductionStory["welcome"]
//        }
    }

    // Dialog exit animation
    fun onDismiss() {
        isDialogVisible = false
    }

    if (isDialogVisible) {
        Dialog(
            onDismissRequest = { onDismiss() },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .safeDrawingPadding()
                    .padding(16.dp)
                    .animateContentSize(
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = LinearEasing
                        )
                    )
                    .fillMaxWidth(),
                tonalElevation = 3.dp,
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .height(200.dp)
                    ){
                        NeuralMeshSymmetrical()
                    }
                    Spacer(Modifier.height(8.dp))
                    BadassTypingText(
                        fullText = currentNode?.text ?: "",
                        isFullTextShown = isFullTextShown,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    if (isFullTextShown.value) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier
                                .graphicsLayer(alpha = optionsAlpha)
                                .fillMaxWidth()
                        ) {
                            currentNode?.options?.forEachIndexed { index, option ->
                                OutlinedButton(
                                    onClick = {
                                        if(option.nextNodeId == null){
                                            onDismiss()
                                            val sp = context.getSharedPreferences("systemDialog",Context.MODE_PRIVATE)
                                            sp.edit(commit = true) {
                                                putBoolean(
                                                    "isIntroDone",
                                                    true
                                                )
                                            }
                                        }
                                        currentNode = currentStory.value[option.nextNodeId]
                                        isFullTextShown.value = false // Reset typing animation
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = option.text,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(25.dp))
                }
            }
        }
    }
}

@Composable
fun BadassTypingText(
    fullText: String,
    isFullTextShown: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    typingSpeed: Long = 10L,
    glitchChance: Float = 0.05f,
    cursorBlinkSpeed: Long = 350L
) {
    var visibleText by remember { mutableStateOf("") }
    var showCursor by remember { mutableStateOf(true) }
    val haptics = LocalHapticFeedback.current
    val randomChars = listOf('|', '/', '\\', '_', '-', '!', '@', '#', '$', '%', '^', '&')

    // Typing animation with glitch effect
    LaunchedEffect(fullText) {
        visibleText = ""
        isFullTextShown.value = false
        for (i in fullText.indices) {
            if (Random.nextFloat() < glitchChance) {
                visibleText += randomChars.random()
                delay(typingSpeed / 2)
                visibleText = visibleText.dropLast(1)
            }
            visibleText += fullText[i]
            if (i % 10 == 0 && i != 0) {
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            }
            delay(typingSpeed)
        }
        isFullTextShown.value = true
    }

    // Blinking cursor with glow effect
    val cursorAlpha by animateFloatAsState(
        targetValue = if (showCursor && visibleText.length < fullText.length) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(cursorBlinkSpeed.toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorBlink"
    )

    LaunchedEffect(Unit) {
        while (true) {
            showCursor = !showCursor
            delay(cursorBlinkSpeed)
        }
    }

    MarkdownText(
        markdown = buildAnnotatedString {
            append(visibleText)
            if (showCursor && visibleText.length < fullText.length) {
                withStyle(
                    SpanStyle(
                        color = Color.White.copy(alpha = cursorAlpha),
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = Color(0xFF00FF00).copy(alpha = cursorAlpha * 0.5f),
                            offset = Offset(0f, 0f),
                            blurRadius = 8f
                        )
                    )
                ) {
                    append("|")
                }
            }
        }.toString(),
        style = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = JetBrainMono,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
            .shadow(4.dp, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp),
    )
}