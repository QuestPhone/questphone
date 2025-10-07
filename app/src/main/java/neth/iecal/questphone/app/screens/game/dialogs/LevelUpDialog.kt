package neth.iecal.questphone.app.screens.game.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import neth.iecal.questphone.R
import neth.iecal.questphone.app.theme.LocalCustomTheme
import nethical.questphone.core.core.utils.VibrationHelper
import nethical.questphone.core.core.utils.managers.SoundManager
import nethical.questphone.data.game.InventoryItem
import kotlin.math.cos
import kotlin.math.sin

private data class LevelSpark(var randomAngle: Int, var speed: Int, var life: Int = 0)

@Composable
fun LevelUpDialog(
    newLevel: Int,
    onDismiss: () -> Unit,
    lvUpRew: HashMap<InventoryItem, Int> = hashMapOf(Pair(InventoryItem.QUEST_SKIPPER,25),Pair(InventoryItem.QUEST_DELETER,1)),
    coinReward: Int
) {
    val context = LocalContext.current
    val textColor = LocalCustomTheme.current.getExtraColorScheme().dialogText

    val previousLevel = newLevel - 1

    // Animation states
    var buttonVisible by remember { mutableStateOf(false) }
    var targetOffset by remember { mutableFloatStateOf(0f) }
    var punchScale by remember { mutableFloatStateOf(1f) }

    // Main entrance animation
    val animationSpec = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessVeryLow
    )

    val animatedOffset by animateFloatAsState(
        targetValue = targetOffset,
        animationSpec = animationSpec,
        label = "scroll_animation"
    )


    // Start animations on launch
    val soundManager = remember { SoundManager(context) }
    LaunchedEffect(newLevel) {
        kotlinx.coroutines.delay(100)
        VibrationHelper.vibrate(600)
        targetOffset = 1f
        soundManager.playSound(R.raw.level_up)
        kotlinx.coroutines.delay(650)
        animate(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = tween(100, easing = LinearEasing)
        ) { value, _ -> punchScale = value }
        animate(
            initialValue = 1.2f,
            targetValue = 1f,
            animationSpec = tween(150, easing = LinearEasing)
        ) { value, _ -> punchScale = value }
        VibrationHelper.vibrate(120)
        buttonVisible = true
    }

    val buttonAlpha by animateFloatAsState(
        targetValue = if (buttonVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 1000) // 1 second fade
    )

    Dialog(
        onDismissRequest = onDismiss,
    ) {
        // Top-level centering using fillMaxSize and Center alignments
        Box(
            modifier = Modifier
                .fillMaxSize(),
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
                // Level Up Content (fades/slides out for rewards)
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(350)) + scaleIn(initialScale = 0.92f),
                    exit = fadeOut(animationSpec = tween(220))
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        // Level Number
                        Box(
                            modifier = Modifier
                                .height(140.dp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            val textHeight = 140.dp
                            val offsetY = animatedOffset * (textHeight.value * 1.2f)

                            // Previous level (fade out)
                            Text(
                                text = previousLevel.toString(),
                                fontSize = 120.sp,
                                color = textColor.copy(alpha = (1f - animatedOffset * 1.2f).coerceAtLeast(0f)),
                                lineHeight = 120.sp,
                                modifier = Modifier.offset(y = (-offsetY * 0.8f).dp)
                            )

                            // Current level (punch + glow + particles)
                            Box(
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = punchScale
                                        scaleY = punchScale
                                    }
                                    .drawBehind {
                                        if (animatedOffset > 0.6f) {
                                            drawCircle(
                                                color = Color(0xFF2D2D2D),
                                                radius = size.maxDimension / 2 * animatedOffset,
                                                blendMode = BlendMode.Plus
                                            )
                                        }
                                    }
                            ) {
                                Text(
                                    text = newLevel.toString(),
                                    fontSize = 120.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor.copy(alpha = (animatedOffset * 1.1f).coerceAtMost(1f)),
                                    lineHeight = 120.sp,
                                    modifier = Modifier.offset(y = (textHeight.value * 1.5f - offsetY).dp)
                                )
                            }

                            if (animatedOffset > 0.73f) {
                                SparkParticles()
                            }
                        }

                        Spacer(modifier = Modifier.height(34.dp))

                        Text(
                            text = "LEVEL UP!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color(0xFF000000),
                                    offset = Offset(4f, 4f),
                                    blurRadius = 8f
                                )
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (coinReward > 0) {
                                RewardItem(
                                    icon = neth.iecal.questphone.R.drawable.coin_icon,
                                    amount = coinReward,
                                    name = "Coins"
                                )
                            }
                            lvUpRew.forEach { (item, amount) ->
                                RewardItem(
                                    icon = item.icon,
                                    amount = amount,
                                    name = item.toString()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                VibrationHelper.vibrate(50)
                                onDismiss()
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .padding(top = 18.dp)
                                .graphicsLayer(alpha = buttonAlpha)
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
fun SparkParticles() {
    val sparks = List(12) {
        remember { LevelSpark(randomAngle = (0..360).random(), speed = (30..80).random()) }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        sparks.forEach { spark ->
            val alphaDecay = 1f - spark.life / 100f
            val dx = cos(Math.toRadians(spark.randomAngle.toDouble())).toFloat() * spark.speed * (spark.life / 100f)
            val dy = sin(Math.toRadians(spark.randomAngle.toDouble())).toFloat() * spark.speed * (spark.life / 100f)
            drawCircle(
                color = Color.Yellow.copy(alpha = alphaDecay),
                radius = 4f,
                center = center + Offset(dx, dy)
            )
            spark.life += 5
        }
    }
}

@Composable
private fun RewardItem(icon: Int, amount: Int, name: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Image(
            painter = painterResource(icon),
            contentDescription = name,
            modifier = Modifier.size(25.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = "x $amount",
            textAlign = TextAlign.Center,
        )
    }
}
