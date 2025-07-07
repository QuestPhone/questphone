package neth.iecal.questphone.ui.screens.onboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import neth.iecal.questphone.utils.isIgnoringBatteryOptimizations
import neth.iecal.questphone.utils.openBatteryOptimizationSettings

@Composable
fun BackgroundUsagePermission(isFromOnboardingScreen: Boolean = true) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isBatteryOptimDisabled = remember { mutableStateOf(false) }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            isBatteryOptimDisabled.value = isIgnoringBatteryOptimizations(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Allow Unrestricted Background Usage",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = if (!isBatteryOptimDisabled.value)
                "To make sure QuestPhone works reliably in the background, please allow it to ignore battery optimizations. This prevents Android from force-stopping the app or delaying its actions."
            else
                "Unrestricted background usage is enabled. You’re all set!",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!isBatteryOptimDisabled.value && !isFromOnboardingScreen) {
            Button(
                onClick = {
                    openBatteryOptimizationSettings(context)
                }
            ) {
                Text(text = "Open Settings")
            }
        }
    }
}
