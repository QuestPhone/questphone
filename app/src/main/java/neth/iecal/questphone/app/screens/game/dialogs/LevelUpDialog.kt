package neth.iecal.questphone.app.screens.game.dialogs

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import neth.iecal.questphone.R
import nethical.questphone.core.core.utils.VibrationHelper
import nethical.questphone.data.game.InventoryItem


@Composable
fun LevelUpDialog(oldLevel: Int,newLevel: Int,onDismiss: () -> Unit,lvUpRew: HashMap<InventoryItem,Int> = hashMapOf()) {
    Dialog(onDismissRequest = onDismiss) {

        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val rotationAnimation = remember { Animatable(0f) }

            LaunchedEffect(key1 = true) {
                rotationAnimation.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(1000)
                )
            }

            Icon(
                painter = painterResource(R.drawable.star),
                contentDescription = "Level Up",
                tint = Color(0xFFFFC107), // Gold color
                modifier = Modifier
                    .size(50.dp)
                    .rotate(rotationAnimation.value)
            )

            Spacer(modifier = Modifier.size(16.dp))

            Text(
                text = "Level Up!",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Spacer(modifier = Modifier.size(8.dp))

            Text(
                text = "You advanced from level $oldLevel to level $newLevel",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (lvUpRew.isNotEmpty()) {
                Text(
                    text = "Rewards",
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                lvUpRew.forEach {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(it.key.icon),
                            contentDescription = it.key.simpleName,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.size(4.dp))

                        Text(
                            text = "${it.key.simpleName} x ${it.value}",
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                }
            }

            Spacer(Modifier.size(16.dp))
            Button(
                onClick = {
                    VibrationHelper.vibrate(50)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(0.7f),
            ) {
                Text("Continue")
            }
        }
    }
}
