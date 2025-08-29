package neth.iecal.questphone.app.screens.game.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import neth.iecal.questphone.R
import neth.iecal.questphone.app.theme.LocalCustomTheme
import nethical.questphone.core.core.utils.VibrationHelper
import nethical.questphone.core.core.utils.managers.SoundManager


@Composable
fun QuestCompletionDialog(coinReward: Int,xpReward:Int, onDismiss: () -> Unit) {
    val textColor = LocalCustomTheme.current.getExtraColorScheme().dialogText

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val bounceAnimation = remember { Animatable(0f) }

            val context = LocalContext.current
            val soundManager = SoundManager(context)
            LaunchedEffect(Unit) {
                VibrationHelper.vibrate(400)
                soundManager.playSound(R.raw.quest_complete)
                while (true) { // Infinite bounce loop
                    bounceAnimation.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.3f,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    bounceAnimation.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.3f,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
            // Apply bounce animation to the coin image
            Image(
                painter = painterResource(R.drawable.coin_icon),
                contentDescription = "coin",
                modifier = Modifier
                    .size(50.dp)
                    .scale(1f + (bounceAnimation.value * 0.2f))
                    .offset(y = (-20 * bounceAnimation.value).dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.size(16.dp))

            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically(animationSpec = tween(1000, easing = LinearEasing))
            ) {
                Column {
                    Text(
                        text = "Quest Complete",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "You earned $coinReward ${if (coinReward > 1) "coins" else "coin"} + $xpReward xp!",
                        textAlign = TextAlign.Center,
                        color = textColor,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.size(8.dp))

            Button(
                onClick = {
                    VibrationHelper.vibrate(50)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(0.7f),
            ) {
                Text("Collect")
            }
        }
    }
}
