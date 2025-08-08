package neth.iecal.questphone.app.screens.launcher.dialogs

import android.content.Context
import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.content.edit
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import neth.iecal.questphone.app.screens.launcher.launchApp
import neth.iecal.questphone.core.services.AppBlockerService
import neth.iecal.questphone.core.utils.managers.User
import neth.iecal.questphone.core.services.AppBlockerServiceInfo
import neth.iecal.questphone.core.services.INTENT_ACTION_UNLOCK_APP
import nethical.questphone.core.core.utils.ScreenUsageStatsHelper
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun FreePassInfo(
    onShowAllQuests: () -> Unit,
    pkgName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var remainingFreePassesToday by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val lastUsedDate = prefs.getString("last_freepass_date", null)

        if (lastUsedDate == today) {
            remainingFreePassesToday = prefs.getInt("freepass_count", 0)
        } else {
            val stats = ScreenUsageStatsHelper(context).getStatsForLast7Days()
            val filteredTimes = stats.filter { it.packageName == pkgName }.map { it.totalTime.toDouble() }
            remainingFreePassesToday = calculateFreeUnlocks(filteredTimes)
            prefs.edit {
                putString("last_freepass_date", today)
                putInt("freepass_count", remainingFreePassesToday)
            }
        }
    }

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
                color = Color.White
            )

            Text(
                text = "These free passes adapt to how consistent and committed you’ve been.\n" +
                        "Keep up the grind, or the boosts slow down. And so does your progress. \uD83D\uDC40",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f),
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
                    color = Color.White
                )
            }

            if(remainingFreePassesToday>0){
                OutlinedButton(
                    onClick = {
                        useFreePass(context, pkgName, onDismiss)
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

private fun calculateFreeUnlocks(screenTimes: List<Double>): Int {
    if (screenTimes.size < 7) return 3 // Fallback for partial data

    val now = Clock.System.now()
    val questStreak = User!!.userInfo.streak.currentStreak
    val daysSinceCreated = User!!.userInfo.created_on.daysUntil(now, TimeZone.currentSystemDefault())
    val weeksSinceFirstUse = daysSinceCreated / 7.0
    val userLevel = User!!.userInfo.level

    val weights = listOf(0.25, 0.2, 0.15, 0.15, 0.1, 0.1, 0.05)
    val weightedAvg = screenTimes.zip(weights).sumOf { (t, w) -> t * w }

    val today = screenTimes[0]
    val yesterday = screenTimes[1]
    val yesterdayAvg = screenTimes.drop(1).average()

    val isNewUser = daysSinceCreated < 7
    val isImproving = today < yesterdayAvg - 1
    val isConsistent = questStreak >= (2 + userLevel / 2)

    val generosityBoost = if (isNewUser) 2.0 else (1.5 - 0.1 * weeksSinceFirstUse).coerceAtLeast(0.5)
    val difficulty = 2.0 + (userLevel * 0.25)
    val baseUnlocks = ((weightedAvg / difficulty) * generosityBoost)

    val progressBonus = if (isImproving) 1 else 0
    val streakBonus = if (isConsistent) 1 else 0

    // 📊 Dynamic max based on yesterday's screen time
    val baseCap = (yesterday * 60 / 10).roundToInt() // 10 min = 1 unlock
    val trendFactor = if (isImproving) 0.75 else 1.0
    val generosityDecay = (1.2 - 0.1 * weeksSinceFirstUse).coerceAtLeast(0.5)
    val dynamicMax = (baseCap * trendFactor * generosityDecay).roundToInt().coerceIn(2, 10)

    return (baseUnlocks + progressBonus + streakBonus).roundToInt().coerceIn(1, dynamicMax)
}


private fun useFreePass(
    context: Context,
    pkgName: String,
    onDismiss: () -> Unit
) {

    val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    val today = LocalDate.now().toString()
    val remainingFreePassesToday = prefs.getInt("freepass_count", 0) - 1

    prefs.edit {
        putString("last_freepass_date", today)
        putInt("freepass_count", remainingFreePassesToday)
    }

    val cooldownTime = 10 * 60_000
    context.sendBroadcast(Intent().apply {
        action = INTENT_ACTION_UNLOCK_APP
        putExtra("selected_time", cooldownTime)
        putExtra("package_name", pkgName)
    })

    if (!AppBlockerServiceInfo.isUsingAccessibilityService && AppBlockerServiceInfo.appBlockerService == null) {
        startForegroundService(context, Intent(context, AppBlockerService::class.java))
        AppBlockerServiceInfo.unlockedApps[pkgName] = System.currentTimeMillis() + cooldownTime
    }

    launchApp(context, pkgName)
    onDismiss()
}
