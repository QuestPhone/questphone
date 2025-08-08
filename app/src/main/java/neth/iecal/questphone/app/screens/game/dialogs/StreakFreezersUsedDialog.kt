package neth.iecal.questphone.app.screens.game.dialogs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import nethical.questphone.core.core.utils.VibrationHelper
import nethical.questphone.data.game.StreakData

@Composable
fun StreakFreezersUsedDialog( streakFreezersUsed:Int, streakData: StreakData, xpEarned: Int,onDismiss: () -> Unit,) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier
            .clip(RoundedCornerShape(11.dp))) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated level up icon
                val rotationAnimation = remember { Animatable(0f) }

                LaunchedEffect(Unit) {
                    rotationAnimation.animateTo(
                        targetValue = 360f,
                        animationSpec = tween(1000)
                    )
                }

                Icon(
                    painter = painterResource(nethical.questphone.data.R.drawable.streak_freezer),
                    contentDescription = "Streak",
                    modifier = Modifier
                        .size(50.dp)
                        .rotate(rotationAnimation.value)
                )

                Spacer(modifier = Modifier.size(16.dp))

                Text(
                    text = "$streakFreezersUsed streak freezers were used to save your streak!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(8.dp))


                Text(
                    text = "New Streak: ${streakData.currentStreak} days",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Rewards",
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "XP: $xpEarned",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                )

                Spacer(Modifier.size(16.dp))

                Button(
                    onClick = {
                        VibrationHelper.vibrate(50)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(0.7f),
                ) {
                    Text("Continue", fontSize = 16.sp)
                }
            }
        }
    }
}