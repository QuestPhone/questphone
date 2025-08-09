package neth.iecal.questphone.app.screens.quest.view

import android.R
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.application
import androidx.lifecycle.compose.LocalLifecycleOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import neth.iecal.questphone.app.screens.components.TopBarActions
import neth.iecal.questphone.app.screens.quest.view.components.MdPad
import neth.iecal.questphone.core.services.AppBlockerService
import neth.iecal.questphone.core.services.AppBlockerServiceInfo
import neth.iecal.questphone.core.services.INTENT_ACTION_START_DEEP_FOCUS
import neth.iecal.questphone.core.services.INTENT_ACTION_STOP_DEEP_FOCUS
import neth.iecal.questphone.core.utils.managers.QuestHelper
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.backend.repositories.StatsRepository
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.core.core.utils.VibrationHelper
import nethical.questphone.core.core.utils.formatHour
import nethical.questphone.core.core.utils.managers.sendRefreshRequest
import nethical.questphone.data.game.InventoryItem
import nethical.questphone.data.game.xpToRewardForQuest
import nethical.questphone.data.json
import nethical.questphone.data.quest.focus.DeepFocus
import javax.inject.Inject

private const val PREF_NAME = "deep_focus_prefs"
private const val KEY_START_TIME = "start_time_"
private const val KEY_PAUSED_ELAPSED = "paused_elapsed_"
private const val NOTIFICATION_CHANNEL_ID = "focus_timer_channel"
private const val NOTIFICATION_ID = 1001

@HiltViewModel
class DeepFocusQuestViewVM @Inject constructor(questRepository: QuestRepository,
                                               userRepository: UserRepository,
                                               statsRepository: StatsRepository,
                                               application: Application
) : ViewQuestVM(
    questRepository, userRepository, statsRepository, application
){
    val isQuestRunning = MutableStateFlow(false)
    val isTimerActive = MutableStateFlow(false)
    val isAppInForeground = MutableStateFlow(false)
    val questHelper = QuestHelper(application)

    var deepFocus = DeepFocus()
    val focusDuration = MutableStateFlow(0L)

    var questKey = ""
    val startTimeKey = KEY_START_TIME + questKey
    val pausedElapsedKey = KEY_PAUSED_ELAPSED + questKey
    val prefs = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    init {
        createNotificationChannel(application)
    }

    fun setDeepFocus(){
        deepFocus = json.decodeFromString<DeepFocus>(commonQuestInfo.quest_json)
        questKey =  commonQuestInfo.title.replace(" ", "_").lowercase()

        isQuestRunning.value = questHelper.isQuestRunning(commonQuestInfo.id)
        if (isQuestRunning.value && !isQuestComplete.value) {
            isTimerActive.value = true
        }
        focusDuration.value = deepFocus.nextFocusDurationInMillis
        Log.d("Deep Focus Length",focusDuration.value.toString())
    }

    fun encodeToCommonQuest(){
        commonQuestInfo.quest_json = json.encodeToString(deepFocus)
    }

    fun startQuest(){
        questHelper.setQuestRunning(commonQuestInfo.id, true)
        isQuestRunning.value = true
        isTimerActive.value = true

        if(!AppBlockerServiceInfo.isUsingAccessibilityService && AppBlockerServiceInfo.appBlockerService==null){
            startForegroundService(application,Intent(application, AppBlockerService::class.java))
        }
        // Clear any existing data and set fresh start time
        prefs.edit {
            putLong(KEY_START_TIME + questKey, System.currentTimeMillis())
                .putLong(KEY_PAUSED_ELAPSED + questKey, 0L)
        }
        AppBlockerServiceInfo.deepFocus.isRunning = true
        AppBlockerServiceInfo.deepFocus.exceptionApps = deepFocus.unrestrictedApps.toHashSet()
        val intent = Intent(INTENT_ACTION_START_DEEP_FOCUS)
        intent.putStringArrayListExtra("exception", deepFocus.unrestrictedApps.toCollection(ArrayList()))
        application.sendBroadcast(intent)
    }

    fun onDeepFocusComplete(){
        questHelper.setQuestRunning(commonQuestInfo.id, false)
        deepFocus.incrementTime()
        focusDuration.value = deepFocus.nextFocusDurationInMillis
        encodeToCommonQuest()
        saveQuestToDb()

        isQuestRunning.value = false
        isTimerActive.value = false

        progress.value = 1f
        // Clear saved times
        prefs.edit {
            remove(startTimeKey)
                .remove(pausedElapsedKey)
        }

        // Cancel notification when complete
        cancelTimerNotification(application)
        sendRefreshRequest(application, INTENT_ACTION_STOP_DEEP_FOCUS)

        AppBlockerServiceInfo.deepFocus.isRunning = false
    }

    fun saveState(){
        if (isQuestRunning.value) {
            val prefs = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val startTimeKey = KEY_START_TIME + questKey
            val savedStartTime = prefs.getLong(startTimeKey, 0L)

            if (savedStartTime > 0) {
                val pausedElapsedKey = KEY_PAUSED_ELAPSED + questKey
                val elapsedTime = System.currentTimeMillis() - savedStartTime
                prefs.edit { putLong(pausedElapsedKey, elapsedTime) }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DeepFocusQuestView(
    commonQuestInfo: CommonQuestInfo,
    viewModel: DeepFocusQuestViewVM = hiltViewModel()
) {
    val context = LocalContext.current
    val isInTimeRange by viewModel.isInTimeRange.collectAsState()

    val isQuestComplete by viewModel.isQuestComplete.collectAsState()
    val isQuestRunning by viewModel.isQuestRunning.collectAsState()
    val timerActive by viewModel.isTimerActive.collectAsState()
    val isAppInForeground by viewModel.isAppInForeground.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val coins by viewModel.coins.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    val questKey = commonQuestInfo.title.replace(" ", "_").lowercase()
    val startTimeKey = KEY_START_TIME + questKey
    val pausedElapsedKey = KEY_PAUSED_ELAPSED + questKey

    val duration by viewModel.focusDuration.collectAsState()

    val isHideStartButton =  isQuestComplete || isQuestRunning || !isInTimeRange

    // Observe app lifecycle for notification management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.isAppInForeground.value = true
                    cancelTimerNotification(context)
                }
                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.isAppInForeground.value = false
                    // Show notification if quest is running and app goes to background
                    if (isQuestRunning && !isQuestComplete) {
                        updateTimerNotification(context, commonQuestInfo.title, progress, duration)
                    }
                }
                else -> { /* Ignore other events */ }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    LaunchedEffect(Unit) {
        viewModel.setCommonQuest(commonQuestInfo)
        viewModel.setDeepFocus()
    }

    // Handle the timer - use timerActive state to trigger/stop
    LaunchedEffect(timerActive) {
        if (timerActive) {
            // Get the start time from SharedPreferences or use current time if not found
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

            // Get saved values or use defaults
            val savedStartTime = prefs.getLong(startTimeKey, 0L)
            val pausedElapsed = prefs.getLong(pausedElapsedKey, 0L)

            val startTime = if (savedStartTime == 0L) {
                // First time starting the timer
                val newStartTime = System.currentTimeMillis() - pausedElapsed
                prefs.edit { putLong(startTimeKey, newStartTime) }
                newStartTime
            } else {
                // Resuming existing timer
                savedStartTime
            }

            // Update progress continually
            while (progress < 1f) {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - startTime
                viewModel.progress.value = (elapsedTime / duration.toFloat()).coerceIn(0f, 1f)

                // Update notification if app is in background
                if (!isAppInForeground && isQuestRunning) {
                    updateTimerNotification(context, commonQuestInfo.title, progress, duration)
                }

                delay(1000) // Update every second instead of 100ms to reduce battery usage
            }
        }
    }
    LaunchedEffect(progress) {
        if (progress >= 1f && isQuestRunning) {
            viewModel.onDeepFocusComplete()
        }
    }

    // Save state when leaving the composition
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveState()
        }
    }

    // Prevent back navigation when quest is running
    BackHandler(isQuestRunning) {}

    Scaffold(
        Modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    TopBarActions(coins, 0, isCoinsVisible = true)
                }
            )
        },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if(!isQuestComplete && viewModel.getInventoryItemCount(InventoryItem.QUEST_SKIPPER) > 0){
                    Image(
                        painter = painterResource(nethical.questphone.data.R.drawable.quest_skipper),
                        contentDescription = "use quest skipper",
                        modifier = Modifier.size(30.dp)
                            .clickable{
                                VibrationHelper.vibrate(50)
                                viewModel.isQuestSkippedDialogVisible.value = true
                            }
                    )

                }
                if(!isHideStartButton) {
                    Spacer(modifier = Modifier.width(15.dp))
                    Button(
                        onClick = {
                            VibrationHelper.vibrate(100)
                            viewModel.startQuest()
                        }
                    ) {
                        Text(text = "Start Quest")
                    }
                }
            }
        }) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(8.dp)
        ) {
            Text(
                text =  commonQuestInfo.title,
                textDecoration = if(!isInTimeRange) TextDecoration.LineThrough else TextDecoration.None,
                style = MaterialTheme.typography.headlineLarge.copy(),
            )

            Text(
                text = (if(!isQuestComplete) "Reward" else "Next Reward") + ": ${commonQuestInfo.reward} coins + ${
                    xpToRewardForQuest(
                        viewModel.level
                    )
                } xp",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Time: ${formatHour(commonQuestInfo.time_range[0])} to ${
                    formatHour(
                        commonQuestInfo.time_range[1]
                    )
                }",
                style = MaterialTheme.typography.bodyLarge
            )
            // Show remaining time
            if (isQuestRunning && progress < 1f) {
                val remainingSeconds = ((duration * (1 - progress)) / 1000).toInt()
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60

                Text(
                    text = "Remaining: $minutes:${seconds.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Text(
                text = if (!isQuestComplete) "Duration: ${duration / 60_000}m" else "Next Duration: ${duration / 60_000}m",
                style = MaterialTheme.typography.bodyLarge
            )

            val pm = context.packageManager
            val apps = viewModel.deepFocus.unrestrictedApps.mapNotNull { packageName ->
                try {
                    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString() to packageName
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                Text(
                    text = "Unrestricted Apps: ",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                )
                apps.forEach { (appName, packageName) ->
                    Text(
                        text = "$appName, ",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .clickable {
                                // Show notification before launching other app
                                if (isQuestRunning && !isQuestComplete) {
                                    updateTimerNotification(context, commonQuestInfo.title, progress, duration)
                                }
                                val intent = pm.getLaunchIntentForPackage(packageName)
                                intent?.let { context.startActivity(it) }
                            }
                    )
                }
            }
            MdPad(commonQuestInfo)

        }
    }

}

private fun createNotificationChannel(context: Context) {
    val name = "Focus Timer"
    val descriptionText = "Shows the remaining time for focus quests"
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
        description = descriptionText
        setShowBadge(true)
    }
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}

private fun updateTimerNotification(context: Context, questTitle: String, progress: Float, duration: Long) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val remainingSeconds = ((duration * (1 - progress)) / 1000).toInt()
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeText = "$minutes:${seconds.toString().padStart(2, '0')}"

    val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_lock_idle_alarm) // Use your app's icon here
        .setContentTitle("Focus Quest: $questTitle")
        .setContentText("Remaining time: $timeText")
        .setProgress(100, (progress * 100).toInt(), false)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setOngoing(true) // Make it persistent
        .setContentIntent(pendingIntent)
        .setSilent(true)
        .setAutoCancel(false)
        .build()

    notificationManager.notify(NOTIFICATION_ID, notification)
}

private fun cancelTimerNotification(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(NOTIFICATION_ID)
}