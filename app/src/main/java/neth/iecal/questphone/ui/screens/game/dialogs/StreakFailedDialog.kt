package neth.iecal.questphone.ui.screens.game.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import nethical.questphone.core.core.utils.VibrationHelper
import nethical.questphone.data.game.StreakCheckReturn


@Composable
fun StreakFailedDialog(streakCheckReturn: StreakCheckReturn, onDismiss: () -> Unit,) {
    val streakDaysLost = streakCheckReturn.streakDaysLost ?: 0
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier
            .clip(RoundedCornerShape(11.dp))) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Spacer(modifier = Modifier.size(16.dp))

                Text(
                    text = "You lost your $streakDaysLost day streak!!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = "Don't worry, you can rise again....",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Spacer(modifier = Modifier.size(8.dp))

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

