package neth.iecal.questphone.app.screens.game.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import neth.iecal.questphone.R
import neth.iecal.questphone.app.theme.LocalCustomTheme
import nethical.questphone.core.core.utils.VibrationHelper
import nethical.questphone.core.core.utils.managers.SoundManager
import nethical.questphone.data.game.StreakData

private enum class AnimationState {
    PRE_ANIMATION, FREEZING, FROZEN
}

private val GlacialBlue = Color(0xFF8CFBFF)
private val DeepNavy = Color(0xFF0A192F)
private val IceWhite = Color(0xFFF0F8FF)

@Composable
fun StreakFreezersUsedDialog(
    streakFreezersUsed: Int,
    streakData: StreakData,
    xpEarned: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }

    var animationState by remember { mutableStateOf(AnimationState.PRE_ANIMATION) }

    // Animation values
    val dialogScale = remember { Animatable(0.8f) }
    val snowflakeRotation = remember { Animatable(0f) }
    val snowflakeScale = remember { Animatable(0.6f) }
    val snowflakeAlpha = remember { Animatable(1f) }
    val frostNovaScale = remember { Animatable(0f) }
    val frostNovaAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shimmer_alpha"
    )
    val textColor = LocalCustomTheme.current.getExtraColorScheme().dialogText

    LaunchedEffect(Unit) {
        launch {
            dialogScale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 300f))
        }
        launch {
            snowflakeScale.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        }

        delay(400)
        animationState = AnimationState.FREEZING

        delay(200)
        // ACT II: Freeze effect
        soundManager.playSound(R.raw.streak_freezer)
        VibrationHelper.vibrate(400)

        launch {
            frostNovaAlpha.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
            frostNovaScale.animateTo(5f, tween(500, easing = FastOutLinearInEasing))
        }
        launch {
            snowflakeRotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(600, easing = LinearEasing)
            )
        }
        launch {
            snowflakeAlpha.animateTo(0f,tween(400, easing = LinearEasing))
        }

        // Show text after snowflake is gone
        delay(400)
        animationState = AnimationState.FROZEN
    }

    Dialog(onDismissRequest = onDismiss) {

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .graphicsLayer { scaleX = dialogScale.value; scaleY = dialogScale.value }
                .clip(RoundedCornerShape(24.dp))
        ) {

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(GlacialBlue, Color.Transparent)
                    ),
                    radius = size.minDimension / 2 * frostNovaScale.value,
                    alpha = shimmerAlpha
                )
            }

            Column(
                modifier = Modifier.padding(vertical = 32.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp)
                ) {

                    Image(
                        painter = painterResource(R.drawable.snowflake),
                        contentDescription = "Streak Freezer",
                        modifier = Modifier
                            .size(70.dp)
                            .graphicsLayer {
                                rotationZ = snowflakeRotation.value
                                scaleX = snowflakeScale.value
                                scaleY = snowflakeScale.value
                                alpha = snowflakeAlpha.value
                            }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(
                    visible = animationState == AnimationState.FROZEN,
                    enter = fadeIn(
                        animationSpec = tween(1000, easing = FastOutSlowInEasing)
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "STREAK PRESERVED",
                            color = IceWhite,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp,
                        )

                        Text(
                            text = "Current Streak: ${streakData.currentStreak} Days",
                            color = GlacialBlue,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )

                        Text(
                            text = "XP Earned: +$xpEarned",
                            color = IceWhite.copy(alpha = 0.9f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                VibrationHelper.vibrate(50)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlacialBlue,
                                contentColor = DeepNavy
                            )
                        ) {
                            Text("CONTINUE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
