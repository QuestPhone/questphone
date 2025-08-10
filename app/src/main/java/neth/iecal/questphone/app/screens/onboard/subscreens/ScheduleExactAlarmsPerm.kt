package neth.iecal.questphone.app.screens.onboard.subscreens

import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import neth.iecal.questphone.core.utils.reminder.NotificationScheduler

@Composable
fun ScheduleExactAlarmPerm(isOnBoardingScreen : Boolean= true) {
    val context = LocalContext.current
    val notificationScheduler = NotificationScheduler(LocalContext.current)
    val hasPermission = remember {
        mutableStateOf(
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                notificationScheduler.alarmManager.canScheduleExactAlarms()
            }else{
                true
            }
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            hasPermission.value = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                notificationScheduler.alarmManager.canScheduleExactAlarms()
            }else{
                true
            }
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
            text = "Schedule Alarms",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = if (!hasPermission.value)
                "QuestPhone needs permission to schedule alarms to notify you about your quests. "
            else
                "Schedule exact alarms permission granted!",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (!hasPermission.value && !isOnBoardingScreen) {
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        context.startActivity(intent)

                    }
                },
            ) {
                Text(text = "Grant Overlay Permission")
            }
        }
    }
}
