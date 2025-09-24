package neth.iecal.questphone.app.screens.etc

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetCoinRewardRatio() {
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    var minutesPerFiveCoins by remember { mutableIntStateOf(10) }

    LaunchedEffect(Unit) {
        val sp = context.getSharedPreferences("minutes_per_5", Context.MODE_PRIVATE)
        minutesPerFiveCoins = sp.getInt("minutes_per_5", minutesPerFiveCoins)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Coin-Reward Ratio", style = MaterialTheme.typography.headlineSmall)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Adjust how many minutes of screen time are granted for every 5 coins:",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "${minutesPerFiveCoins.toInt()} minutes for every 5 coins",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Slider(
                value = minutesPerFiveCoins.toFloat(),
                onValueChange = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    minutesPerFiveCoins = it.toInt() },
                valueRange = 5f..60f,
                steps = 10, // for steps 5,10,15...60
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val sp = context.getSharedPreferences("minutes_per_5", Context.MODE_PRIVATE)
                    sp.edit { putInt("minutes_per_5", minutesPerFiveCoins) }
                    Toast.makeText(context,"Saved. You can press back press safely", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Preference")
            }
        }
    }
}
