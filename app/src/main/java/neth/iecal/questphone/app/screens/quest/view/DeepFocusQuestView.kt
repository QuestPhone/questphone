package neth.iecal.questphone.app.screens.quest.view

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.application
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import neth.iecal.questphone.app.screens.components.TopBarActions
import neth.iecal.questphone.app.screens.quest.view.components.MdPad
import neth.iecal.questphone.app.screens.quest.view.dialogs.QuestSkipperDialog
import neth.iecal.questphone.app.theme.LocalCustomTheme
import neth.iecal.questphone.app.theme.smoothYellow
import neth.iecal.questphone.backed.repositories.QuestRepository
import neth.iecal.questphone.backed.repositories.StatsRepository
import neth.iecal.questphone.backed.repositories.UserRepository
import neth.iecal.questphone.core.services.AppBlockerService
import neth.iecal.questphone.core.services.AppBlockerServiceInfo
import neth.iecal.questphone.core.services.INTENT_ACTION_START_DEEP_FOCUS
import neth.iecal.questphone.core.services.INTENT_ACTION_STOP_DEEP_FOCUS
import neth.iecal.questphone.core.utils.managers.QuestHelper
import neth.iecal.questphone.data.CommonQuestInfo
import nethical.questphone.core.core.utils.VibrationHelper
import nethical.questphone.core.core.utils.formatHour
import nethical.questphone.core.core.utils.getCurrentDate
import nethical.questphone.core.core.utils.managers.sendRefreshRequest
import nethical.questphone.data.game.InventoryItem
import nethical.questphone.data.json
import nethical.questphone.data.quest.focus.DeepFocus
import nethical.questphone.data.xpToRewardForQuest
import javax.inject.Inject

private const val PREF_NAME = "deep_focus_prefs"
private const val KEY_START_TIME = "start_time_"
private const val KEY_PAUSED_ELAPSED = "paused_elapsed_"

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

        val intent = Intent(INTENT_ACTION_START_DEEP_FOCUS)
        intent.putStringArrayListExtra("exception", deepFocus.unrestrictedApps.toCollection(ArrayList()))
        intent.putExtra("duration", deepFocus.nextFocusDurationInMillis)
        application.sendBroadcast(intent)
    }

    fun onDeepFocusComplete(){
        questHelper.setQuestRunning(commonQuestInfo.id, false)
        deepFocus.incrementTime()
        focusDuration.value = deepFocus.nextFocusDurationInMillis
        encodeToCommonQuest()
        saveMarkedQuestToDb()

        isQuestRunning.value = false
        isTimerActive.value = false

        progress.value = 1f
        // Clear saved times
        prefs.edit {
            remove(startTimeKey)
                .remove(pausedElapsedKey)
        }

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
    val progress by viewModel.progress.collectAsState()
    val coins by viewModel.coins.collectAsState()
    val activeBoosts by viewModel.activeBoosts.collectAsState()

    val questKey = commonQuestInfo.title.replace(" ", "_").lowercase()
    val startTimeKey = KEY_START_TIME + questKey
    val pausedElapsedKey = KEY_PAUSED_ELAPSED + questKey

    val duration by viewModel.focusDuration.collectAsState()

    val isHideStartButton = isQuestComplete || isQuestRunning || !isInTimeRange

    val scrollState = rememberScrollState()
    // Observe app lifecycle for notification management

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
        contentWindowInsets = WindowInsets(0),
        containerColor = LocalCustomTheme.current.getRootColorScheme().surface,
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 8.dp
                )
            ) {
                if (!isQuestComplete && viewModel.getInventoryItemCount(InventoryItem.QUEST_SKIPPER) > 0) {
                    Image(
                        painter = painterResource(nethical.questphone.data.R.drawable.quest_skipper),
                        contentDescription = "use quest skipper",
                        modifier = Modifier.size(30.dp)
                            .clickable {
                                VibrationHelper.vibrate(50)
                                viewModel.isQuestSkippedDialogVisible.value = true
                            }
                    )

                }
                if (!isHideStartButton) {
                    Spacer(modifier = Modifier.width(15.dp))
                    Button(
                        onClick = {
                            VibrationHelper.vibrate(100)
                            viewModel.startQuest()
                        }
                    ) {
                        Text(text = "Start Focusing")
                    }
                }
            }
        }) { innerPadding ->

        if (isQuestRunning && progress < 1f || isQuestComplete) {
            LocalCustomTheme.current.DeepFocusThemeObjects(
                innerPadding,
                progress,
                commonQuestInfo.id + getCurrentDate()
            )
        }

        QuestSkipperDialog(viewModel)

        Box(Modifier.fillMaxSize()) {
            Column(Modifier.padding(WindowInsets.statusBarsIgnoringVisibility.asPaddingValues())) {
                TopBarActions(coins, 0, isCoinsVisible = true)
            Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .verticalScroll(scrollState)

                ) {

                    Text(
                        text = commonQuestInfo.title,
                        textDecoration = if (!isInTimeRange) TextDecoration.LineThrough else TextDecoration.None,
                        style = MaterialTheme.typography.headlineLarge.copy(),
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = (if (!isQuestComplete) "Reward" else "Next Reward") + ": ${commonQuestInfo.reward} coins + ${
                                xpToRewardForQuest(
                                    viewModel.level
                                )
                            } xp",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (!isQuestComplete && viewModel.isBoosterActive(InventoryItem.XP_BOOSTER)) {
                            Text(
                                text = " + ",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Black,
                                color = smoothYellow
                            )
                            Image(
                                painter = painterResource(InventoryItem.XP_BOOSTER.icon),
                                contentDescription = InventoryItem.XP_BOOSTER.simpleName,
                                Modifier.size(20.dp)
                            )
                            Text(
                                text = " ${
                                    xpToRewardForQuest(
                                        viewModel.level
                                    )
                                } xp",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Black,
                                color = smoothYellow
                            )
                        }
                    }


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
                            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0))
                                .toString() to packageName
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
                                        val intent = pm.getLaunchIntentForPackage(packageName)
                                        intent?.let { context.startActivity(it) }
                                    }
                            )
                        }
                    }


                    MdPad(commonQuestInfo)
                    Spacer(Modifier.size(1.dp).padding(
                            bottom = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues()
                                .calculateBottomPadding() * 2
                            ))
                }
            }
        }

    }

}