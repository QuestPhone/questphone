package neth.iecal.questphone.app.screens.launcher.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import neth.iecal.questphone.app.theme.LocalCustomTheme

@Composable
fun FreePassInfo(
    onShowAllQuests: () -> Unit,
    pkgName: String,
    remainingFreePassesToday: Int,
    onFreePassUsed:()->Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val textColor = LocalCustomTheme.current.getExtraColorScheme().dialogText

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
            Text(
                text = "😤 I THOUGHT YOU DOWNLOADED THIS APP TO FIX YOUR LIFE",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6F00)
            )

            Text(
                text = "You have $remainingFreePassesToday free ${if (remainingFreePassesToday == 1) "pass" else "passes"} today for this app. Each pass gives 10 minutes of app usage",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )

            Text(
                text = "These free passes adapt to how consistent and committed you’ve been.\n" +
                        "Keep up the grind, or the boosts slow down. And so does your progress. \uD83D\uDC40",
                fontSize = 13.sp,
                color = textColor.copy(alpha = 0.85f),
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onShowAllQuests,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)), // Green
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "🔥 Start a Quest",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if(remainingFreePassesToday>0){
                OutlinedButton(
                    onClick = {
                        onFreePassUsed()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    border = BorderStroke(1.dp, Color.LightGray),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "😐 Use Free Pass",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.LightGray
                    )
                }
            }

        }
    }
}

@Composable
fun MakeAChoice(
    onQuestClick: () -> Unit,
    onFreePassClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // QUEST CARD
        Card(
            onClick = onQuestClick,
            modifier = Modifier
                .weight(0.65f)
                .shadow(16.dp, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF00C853)), // green
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🔥\n\nTake the Quest!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text("Earn coins + XP + streak boost", fontSize = 14.sp, color = Color.White)
                Spacer(Modifier.height(12.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    "🚀 Only takes a few mins!",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // FREE PASS CARD
        Card(
            onClick = onFreePassClick,
            modifier = Modifier
                .weight(0.35f)
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFB0BEC5)), // dull gray
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "😐 Use Free Pass",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray
                )
                Text(
                    "No XP. No progress.",
                    fontSize = 12.sp,
                    color = Color.DarkGray.copy(alpha = 0.8f)
                )
                Text("Lame route", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}


