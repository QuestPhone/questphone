
package neth.iecal.questphone.app.screens.game.dialogs

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import neth.iecal.questphone.R
import nethical.questphone.core.core.utils.VibrationHelper
import nethical.questphone.core.core.utils.managers.SoundManager
import nethical.questphone.data.game.StreakData
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


private data class FireSpark(
    val startPos: Offset,
    val velocity: Offset,
    val color: Color,
    val size: Float,
    val lifetime: Float,
    val gravity: Float = 0.5f,
    val twinkle: Boolean = false
)

@Composable
fun StreakUpDialog(
    streakData: StreakData,
    xpEarned: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val transition = rememberInfiniteTransition()

    // Entrance animation state
    val enterAnimationPlayed = remember { mutableStateOf(false) }

    // Scale animation for explosive entrance
    val scaleAnimation = animateFloatAsState(
        targetValue = if (enterAnimationPlayed.value) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    // Alpha fade-in
    val alphaAnimation = animateFloatAsState(
        targetValue = if (enterAnimationPlayed.value) 1f else 0f,
        animationSpec = tween(600, delayMillis = 200)
    )

    // Explosive burst effect
    val burstScale = animateFloatAsState(
        targetValue = if (enterAnimationPlayed.value) 1f else 3f,
        animationSpec = tween(400, easing = FastOutLinearInEasing)
    )

    val burstAlpha = animateFloatAsState(
        targetValue = if (enterAnimationPlayed.value) 0f else 1f,
        animationSpec = tween(400)
    )

    val flickerRadius = transition.animateFloat(
        initialValue = 180f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val flickerAlpha = transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val jitter = remember { mutableStateOf(0f) }


    // Enhanced spark system for realistic sparks
    val sparks = remember {
        mutableStateOf((0..24).map {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 8f + 4f
            val sparkColor = when (Random.nextInt(4)) {
                0 -> Color(0xFFFFD700) // Gold
                1 -> Color(0xFFFF8C00) // Orange
                2 -> Color(0xFFFF4500) // Red-Orange
                else -> Color(0xFFFFFFFF) // White hot
            }

            FireSpark(
                startPos = Offset.Zero,
                velocity = Offset(
                    cos(angle) * speed,
                    sin(angle) * speed - Random.nextFloat() * 2f // Slight upward bias
                ),
                color = sparkColor,
                size = Random.nextFloat() * 3f + 1f,
                lifetime = Random.nextFloat() * 2f + 1f,
                gravity = Random.nextFloat() * 0.3f + 0.2f,
                twinkle = Random.nextBoolean()
            )
        })
    }

    // Continuous sparks animation
    val sparkTime = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    // Particle system for sparks
    val sparkPositions = remember {
        mutableStateOf((0..12).map {
            val angle = (it * 30f) * (PI / 180f).toFloat()
            val distance = Random.nextFloat() * 100f + 50f
            Offset(
                cos(angle) * distance,
                sin(angle) * distance
            )
        })
    }

    val sparkAnimation = animateFloatAsState(
        targetValue = if (enterAnimationPlayed.value) 150f else 0f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing)
    )

    LaunchedEffect(Unit) {
        // Trigger entrance animation
        delay(100)
        enterAnimationPlayed.value = true

        // Start jitter effect
        while (true) {
            jitter.value = Random.nextFloat() * 6 - 3
            delay(80)
        }
    }

    val soundManager = remember { SoundManager(context) }
    LaunchedEffect(enterAnimationPlayed.value) {
        if (enterAnimationPlayed.value) {
            VibrationHelper.vibrate(600)
            soundManager.playSound(R.raw.game_streak_up)

        }
    }


    DisposableEffect(Unit) {
        onDispose {
            soundManager.release()
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(24.dp)
                    .offset(y = 16.dp)
                    .graphicsLayer {
                        scaleX = scaleAnimation.value
                        scaleY = scaleAnimation.value
                        alpha = alphaAnimation.value
                    }
            ) {
                // Explosive burst background
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(alpha = burstAlpha.value)
                ) {
                    val center = Offset(size.width / 2, size.height / 2 - 122.dp.toPx())
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700),
                                Color(0xFFFF6A00),
                                Color(0xFFFF0000),
                                Color.Transparent
                            ),
                            center = center,
                            radius = 300f * burstScale.value
                        ),
                        radius = 300f * burstScale.value,
                        center = center
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(999f)
                        .offset(y = (-122).dp)
                        .align(Alignment.Center)
                ) {
                    // Animated sparks/particles
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = 0.8f)
                    ) {
                        val center = Offset(size.width / 2, size.height / 2)

                        sparkPositions.value.forEachIndexed { index, basePos ->
                            val progress = sparkAnimation.value / 150f
                            val sparkPos = Offset(
                                center.x + basePos.x * progress,
                                center.y + basePos.y * progress
                            )

                            val sparkSize = (1f - progress) * 8f
                            if (sparkSize > 0) {
                                drawCircle(
                                    color = when (index % 3) {
                                        0 -> Color(0xFFFFD700)
                                        1 -> Color(0xFFFF6A00)
                                        else -> Color(0xFFFF4444)
                                    }.copy(alpha = 1f - progress),
                                    radius = sparkSize,
                                    center = sparkPos
                                )
                            }
                        }
                    }

                    // Existing glow effect
                    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.3f)) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = flickerRadius.value + jitter.value

                        // Inner intense yellow-orange core with pulsing
                        val pulseMultiplier = 1f + sin(System.currentTimeMillis() * 0.01f) * 0.2f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFB300).copy(alpha = flickerAlpha.value),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = (radius / 2) * pulseMultiplier
                            ),
                            radius = (radius / 2) * pulseMultiplier,
                            center = center
                        )

                        // Mid orange glow with secondary pulse
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF6A00).copy(alpha = flickerAlpha.value),
                                    Color(0xFFD22B00).copy(alpha = flickerAlpha.value * 0.8f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = radius
                            ),
                            radius = radius,
                            center = center
                        )

                        // Outer red haze
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF8B0000).copy(alpha = 0.4f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = radius * 1.4f
                            ),
                            radius = radius * 1.4f,
                            center = center
                        )
                    }

                    // Flame GIF with slight scaling animation
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(R.raw.streak_fire)
                                .decoderFactory(GifDecoder.Factory())
                                .build()
                        ),
                        contentDescription = "Flame animation",
                        modifier = Modifier
                            .size(200.dp)
                            .graphicsLayer {
                                alpha = 0.9f
                                val breathe = 1f + sin(System.currentTimeMillis() * 0.005f) * 0.05f
                                scaleX = breathe
                                scaleY = breathe
                            }
                            .zIndex(99999f)
                    )
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(9995f)
                        .offset(y = (-122).dp)
                ) {
                    if (enterAnimationPlayed.value) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val currentTime = sparkTime.value

                        sparks.value.forEach { spark ->
                            val age = (currentTime % 3f) // Reset every 3 seconds
                            if (age <= spark.lifetime) {
                                val progress = age / spark.lifetime
                                val fadeProgress = if (progress > 0.7f) {
                                    1f - ((progress - 0.7f) / 0.3f) // Fade out in last 30%
                                } else 1f

                                // Physics simulation
                                val currentPos = Offset(
                                    center.x + spark.startPos.x + spark.velocity.x * age * 30f,
                                    center.y + spark.startPos.y + spark.velocity.y * age * 30f + spark.gravity * age * age * 15f
                                )

                                // Only draw if spark is within canvas bounds
                                if (currentPos.x >= 0 && currentPos.x <= size.width &&
                                    currentPos.y >= 0 && currentPos.y <= size.height) {

                                    val alpha = fadeProgress * (if (spark.twinkle) {
                                        0.5f + 0.5f * sin(currentTime * 10f + spark.hashCode())
                                    } else 1f)

                                    val sparkSize = spark.size * (1.2f - progress * 0.7f) // Shrink over time

                                    // Draw spark with trailing effect
                                    if (sparkSize > 0.3f) {
                                        // Main spark
                                        drawCircle(
                                            color = spark.color.copy(alpha = alpha),
                                            radius = sparkSize,
                                            center = currentPos
                                        )

                                        // Trailing glow for larger sparks
                                        if (sparkSize > 1.5f) {
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    colors = listOf(
                                                        spark.color.copy(alpha = alpha * 0.3f),
                                                        Color.Transparent
                                                    ),
                                                    radius = sparkSize * 2f
                                                ),
                                                radius = sparkSize * 2f,
                                                center = currentPos
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Main content surface with slide-up animation
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .graphicsLayer {
                            translationY = (1f - scaleAnimation.value) * 100f
                        },
                    color = Color(0xFF2D2D2D),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Day ${streakData.currentStreak}",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                        Button(
                            onClick = {
                                VibrationHelper.vibrate(50)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .padding(top = 16.dp),
                        ) {
                            Text("Collect $xpEarned xp", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}