package neth.iecal.questphone.ui.screens.onboard.subscreens

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
import neth.iecal.questphone.core.utils.managers.checkUsagePermission
import neth.iecal.questphone.core.utils.managers.openBatteryOptimizationSettings

@Composable
fun UsageAccessPerm(isFromOnboardingScreen : Boolean = true) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasUsagePermission = remember { mutableStateOf(false) }


    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            hasUsagePermission.value = checkUsagePermission(context)
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
            text = "Grant Usage Access",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = if (!hasUsagePermission.value)
                "Please allow QuestPhone to access app usage data to show you detailed statistics about your screen time usage and help find ways to reduce it. All of this data is processed 100% locally and nothing is sent to our servers."
            else
                "Usage access granted. You’re all set!",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!hasUsagePermission.value && !isFromOnboardingScreen) {
            Button(
                onClick = {
                    openBatteryOptimizationSettings(context)
                },
            ) {
                Text(text = "Open Settings")
            }
        }
    }
}
