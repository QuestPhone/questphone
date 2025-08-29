package neth.iecal.questphone.app.screens.game.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import neth.iecal.questphone.app.theme.LocalCustomTheme
import nethical.questphone.core.core.utils.VibrationHelper
import nethical.questphone.core.core.utils.managers.SoundManager
import nethical.questphone.data.game.StreakFreezerReturn
import kotlin.math.cos
import kotlin.math.sin

private data class BreakSpark(var randomAngle: Int, var speed: Int, var life: Int = 0)

@Composable
fun StreakFailedDialog(
    streakFreezerReturn: StreakFreezerReturn,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val streakDaysLost = streakFreezerReturn.streakDaysLost ?: 0

    // Animation states
    var buttonVisible by remember { mutableStateOf(false) }
    var currentNumber by remember { mutableIntStateOf(streakDaysLost) }
    var countdownFinished by remember { mutableStateOf(false) }
    var shakeScale by remember { mutableFloatStateOf(1f) }
    var glowIntensity by remember { mutableFloatStateOf(0f) }
    val textColor = LocalCustomTheme.current.getExtraColorScheme().dialogText

    // Start the countdown animation
    val soundManager = remember { SoundManager(context) }
    LaunchedEffect(streakDaysLost) {
        kotlinx.coroutines.delay(500) // Initial pause for dramatic effect
        // Slow countdown with increasing intensity
        var current = streakDaysLost
        while (current > 0) {
            val delayTime = when {
                current > 20 -> 150L  // Fast for high numbers
                current > 10 -> 200L  // Medium for medium numbers
                current > 5 -> 300L   // Slower for low numbers
                else -> 500L          // Very slow for final numbers
            }

            kotlinx.coroutines.delay(delayTime)
            current--
            currentNumber = current

            // Increasing vibration intensity as we get closer to 0
            val vibrationStrength = when {
                current <= 3 -> 100L
                current <= 7 -> 60L
                else -> 30L
            }
            VibrationHelper.vibrate(vibrationStrength)

            // soundManager.playSound(R.raw.tick)
        }

        // Final dramatic effect when reaching 0
        countdownFinished = true
        soundManager.playSound(neth.iecal.questphone.R.raw.streap_fail)
        VibrationHelper.vibrate(800)

        // Shake and glow effects
        glowIntensity = 1f
        animate(
            initialValue = 1f,
            targetValue = 0.85f,
            animationSpec = tween(100, easing = LinearEasing)
        ) { value, _ -> shakeScale = value }
        animate(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = tween(100, easing = LinearEasing)
        ) { value, _ -> shakeScale = value }
        animate(
            initialValue = 1.15f,
            targetValue = 0.95f,
            animationSpec = tween(80, easing = LinearEasing)
        ) { value, _ -> shakeScale = value }
        animate(
            initialValue = 0.95f,
            targetValue = 1f,
            animationSpec = tween(120, easing = LinearEasing)
        ) { value, _ -> shakeScale = value }

        kotlinx.coroutines.delay(500)
        buttonVisible = true
    }

    val buttonAlpha by animateFloatAsState(
        targetValue = if (buttonVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )

    val glowAlpha by animateFloatAsState(
        targetValue = glowIntensity,
        animationSpec = tween(durationMillis = 300)
    )

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(350)) + scaleIn(initialScale = 0.92f),
                    exit = fadeOut(animationSpec = tween(220))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Countdown Number Display
                        Box(
                            modifier = Modifier
                                .height(140.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            // Slot machine style number with dramatic effects
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = shakeScale
                                        scaleY = shakeScale
                                    }
                                    .drawBehind {
                                        // Glow effect that gets more intense as we approach 0
                                        val glowColor = when {
                                            currentNumber == 0 -> Color(0xFFFF0000)
                                            currentNumber <= 3 -> Color(0xFFFF4444)
                                            currentNumber <= 7 -> Color(0xFFFF8800)
                                            else -> Color(0xFFFFAA00)
                                        }

                                        if (countdownFinished && currentNumber == 0) {
                                            drawCircle(
                                                color = glowColor.copy(alpha = glowAlpha * 0.6f),
                                                radius = size.maxDimension / 1.5f,
                                                blendMode = BlendMode.Plus
                                            )
                                        } else if (currentNumber <= 5) {
                                            drawCircle(
                                                color = glowColor.copy(alpha = 0.3f),
                                                radius = size.maxDimension / 3f,
                                                blendMode = BlendMode.Plus
                                            )
                                        }
                                    }
                            ) {
                                Text(
                                    text = currentNumber.toString(),
                                    fontSize = 120.sp,
                                    fontWeight = FontWeight.Bold,

                                    color = when {
                                        currentNumber == 0 -> Color(0xFFFF0000)
                                        currentNumber <= 3 -> Color(0xFFFF4444)
                                        currentNumber <= 7 -> Color(0xFFFF6600)
                                        else -> Color.White
                                    },
                                    lineHeight = 120.sp
                                )
                            }

                            // Particles effect when countdown finishes
                            if (countdownFinished && currentNumber == 0) {
                                BreakParticles()
                            }
                        }

                        Spacer(modifier = Modifier.height(34.dp))

                        // Title that appears based on countdown state
                        if (countdownFinished) {
                            Text(
                                text = "STREAK BROKEN!",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFF4444),

                            )
                        } else {
                            Text(
                                text = "LOSING STREAK...",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (countdownFinished) {
                            Text(
                                text = "You lost your $streakDaysLost day streak!",
                                color = textColor,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,

                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Don't worry, you can rise again....",
                                color = textColor,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        } else {
                            Text(
                                text = "Watch your ${streakDaysLost} day streak disappear...",
                                color = textColor,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                VibrationHelper.vibrate(50)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .graphicsLayer(alpha = buttonAlpha),
                            enabled = buttonVisible
                        ) {
                            Text("Continue", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BreakParticles() {
    val sparks = List(20) {
        remember { BreakSpark(randomAngle = (0..360).random(), speed = (15..50).random()) }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        sparks.forEach { spark ->
            val alphaDecay = 1f - spark.life / 120f
            val dx = cos(Math.toRadians(spark.randomAngle.toDouble())).toFloat() * spark.speed * (spark.life / 100f)
            val dy = sin(Math.toRadians(spark.randomAngle.toDouble())).toFloat() * spark.speed * (spark.life / 100f)

            // Red particles for break effect
            val particleColor = when (spark.randomAngle % 3) {
                0 -> Color.Red
                1 -> Color(0xFFFF4444)
                else -> Color(0xFFFF8800)
            }

            drawCircle(
                color = particleColor.copy(alpha = alphaDecay),
                radius = 4f,
                center = center + Offset(dx, dy)
            )
            spark.life += 3
        }
    }
}