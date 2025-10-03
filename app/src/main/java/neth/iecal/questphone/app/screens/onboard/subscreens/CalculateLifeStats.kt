package neth.iecal.questphone.app.screens.onboard.subscreens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import nethical.questphone.core.core.utils.ScreenUsageStatsHelper
import nethical.questphone.core.core.utils.VibrationHelper

@Composable
fun CalculateLifeStats() {
    val context = LocalContext.current

    var yearsScrolled by remember { mutableFloatStateOf(0f) }

    var startNumberAnimation by remember { mutableStateOf(false) }
    var showFadingText by remember { mutableStateOf(false) }

    val animatedNumber by animateFloatAsState(
        targetValue = if (startNumberAnimation) yearsScrolled else 0f,
        animationSpec = spring(
            dampingRatio = 0.4f, // Lower ratio = more bounce
            stiffness = 50f      // How stiff the "spring" is
        ),
        label = "numberAnimation"
    )

    val statsPref = context.getSharedPreferences("onboard_stats", Context.MODE_PRIVATE)

    var isShown by remember {  mutableStateOf(statsPref.contains("is_shown"))}
    LaunchedEffect(Unit) {
        // Perform heavy calculation off the main thread
        val calculatedYears = withContext(Dispatchers.IO) {
            val stats = ScreenUsageStatsHelper(context).getStatsForLast7Days()
            var total7Days = 0.1
            stats.forEach { total7Days += it.totalTime / (1000 * 60 * 60) }
            // Assuming a default age of 18 for this calculation
            yearsLost(total7Days, 18).toFloat()
        }

        yearsScrolled = calculatedYears

        delay(500) // A small delay for dramatic effect before starting
        startNumberAnimation = true

        delay(1200)
        showFadingText = true
        statsPref.edit(commit = true) { putBoolean("is_shown", true) }
    }

    LaunchedEffect(animatedNumber) {
        if(!isShown){
            VibrationHelper.vibrate(100)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "%.1f".format(animatedNumber),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Black
        )


        AnimatedVisibility(
            visible = showFadingText,
            enter = fadeIn(animationSpec = tween(1000, easing = LinearEasing)) + expandVertically(animationSpec = tween(1000, easing = LinearEasing))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "YEARS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                MarkdownText(
                    markdown = "over the span of the next **54 years** if you don't fix your habits TODAY!\n(Based on your screen time the last 7 days)",
                    style = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Light),
                )
            }
        }
    }
}

fun yearsLost(weeklyHours: Double, age: Int, lifeExpectancy: Int = 72): Double {
    val yearsRemaining = lifeExpectancy - age
    val yearlyHours = weeklyHours * 52
    val lifetimeHours = yearlyHours * yearsRemaining
    return lifetimeHours / (24.0 * 365)
}