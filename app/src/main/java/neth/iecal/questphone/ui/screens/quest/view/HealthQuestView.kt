package neth.iecal.questphone.ui.screens.quest.view

import android.annotation.SuppressLint
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.getUserInfo
import neth.iecal.questphone.data.game.xpToRewardForQuest
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.health.HealthQuest
import neth.iecal.questphone.data.quest.health.HealthTaskType
import neth.iecal.questphone.data.quest.stats.StatsDatabaseProvider
import neth.iecal.questphone.data.quest.stats.StatsInfo
import neth.iecal.questphone.ui.screens.quest.checkForRewards
import neth.iecal.questphone.ui.screens.quest.view.components.MdPad
import neth.iecal.questphone.ui.screens.tutorial.HealthConnectScreen
import neth.iecal.questphone.ui.theme.JetBrainsMonoFont
import neth.iecal.questphone.utils.HealthConnectManager
import neth.iecal.questphone.utils.HealthConnectPermissionManager
import neth.iecal.questphone.utils.QuestHelper
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.json
import java.util.UUID

@SuppressLint("DefaultLocale")
@Composable
fun HealthQuestView(commonQuestInfo: CommonQuestInfo) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val questHelper = QuestHelper(context)
    val healthQuest by remember { mutableStateOf(json.decodeFromString<HealthQuest>(commonQuestInfo.quest_json)) }
    val dao = QuestDatabaseProvider.getInstance(context).questDao()

    val healthManager = HealthConnectManager(context)
    val permissionManager = HealthConnectPermissionManager(context)

    var isQuestComplete =
        remember { mutableStateOf(commonQuestInfo.last_completed_on == getCurrentDate()) }
    val hasRequiredPermissions = remember { mutableStateOf(false) }
    val currentHealthData = remember { mutableDoubleStateOf(0.0) }
    val progressState = remember { mutableFloatStateOf(if (isQuestComplete.value) 1f else 0f) }
    val userInfo = getUserInfo(LocalContext.current)

    fun onQuestCompleted(){
        healthQuest.incrementGoal()
        commonQuestInfo.quest_json = json.encodeToString(healthQuest)
        commonQuestInfo.last_completed_on = getCurrentDate()
        commonQuestInfo.synced = false
        commonQuestInfo.last_updated = System.currentTimeMillis()
        scope.launch {
            dao.upsertQuest(commonQuestInfo)
            val statsDao = StatsDatabaseProvider.getInstance(context).statsDao()
            statsDao.upsertStats(
                StatsInfo(
                    id = UUID.randomUUID().toString(),
                    quest_id = commonQuestInfo.id,
                    user_id = User.userInfo.userId,
                    )
            )
        }
        checkForRewards(commonQuestInfo)
        isQuestComplete.value = true
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = permissionManager.requestPermissionContract,
        onResult = { granted ->
            hasRequiredPermissions.value = granted.containsAll(permissionManager.permissions)
            if (hasRequiredPermissions.value) {
                scope.launch {
                    fetchHealthData(healthManager, healthQuest.type) { data ->
                        currentHealthData.doubleValue = data
                        // Update progress based on nextGoal
                        progressState.floatValue =
                            (data / healthQuest.nextGoal).toFloat().coerceIn(0f, 1f)
                        if(data>=healthQuest.nextGoal){
                            if(!isQuestComplete.value){
                                onQuestCompleted()
                            }
                        }
                    }
                }
            }
        }
    )


    LaunchedEffect(Unit) {
        val isHealthConnectAvailable = healthManager.isAvailable()
        if (!isHealthConnectAvailable) {
            Log.d("HealthConnect", "Health Connect not available")
            return@LaunchedEffect
        }

        hasRequiredPermissions.value = healthManager.hasAllPermissions()
        if (!hasRequiredPermissions.value) {
//            permissionLauncher.launch(permissionManager.permissions)
        } else {
            fetchHealthData(healthManager, healthQuest.type) { data ->
                currentHealthData.doubleValue = data
                progressState.floatValue =
                    (data / healthQuest.nextGoal).toFloat().coerceIn(0f, 1f)
                if(data>=healthQuest.nextGoal){
                    if(!isQuestComplete.value){
                        onQuestCompleted()
                    }
                }
            }
        }

    }


    if (!hasRequiredPermissions.value) {
        HealthConnectScreen(
            onGetStarted = {
                permissionLauncher.launch(permissionManager.permissions)
            },
            onSkip = {
                hasRequiredPermissions.value = true
            }
        )
    } else {

        BaseQuestView(
            hideStartQuestBtn = true,
            progress = progressState,
            loadingAnimationDuration = 400,
            onQuestStarted = { /* No-op for now, health quests auto-track */ },
            onQuestCompleted = {
                onQuestCompleted()
            },
            isQuestCompleted = isQuestComplete
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = commonQuestInfo.title,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    fontFamily = JetBrainsMonoFont,
                    modifier = Modifier.padding(top = 40.dp)
                )
                Text(
                    text = (if(isQuestComplete.value) "Next Reward" else "Reward") + ": ${commonQuestInfo.reward} coins + ${
                        xpToRewardForQuest(
                            userInfo.level
                        )
                    } xp",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                )

                Text(
                    text = "Health Task Type: ${healthQuest.type.label}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
                )

                if (isQuestComplete.value) {
                    Text(
                        text = "Today Progress: ${
                            String.format(
                                "%.3f",
                                currentHealthData.doubleValue
                            )
                        }",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Next Goal: ${healthQuest.nextGoal} ${healthQuest.type.unit}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                } else {
                    Text(
                        text = "Current Progress: ${
                            String.format(
                                "%.3f",
                                currentHealthData.doubleValue
                            )
                        } / ${healthQuest.nextGoal} ${
                            healthQuest.type.unit
                        }",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                MdPad(commonQuestInfo)

            }
        }

    }
}

private suspend fun fetchHealthData(
    healthManager: HealthConnectManager,
    taskType: HealthTaskType,
    onDataReceived: (Double) -> Unit
) {
    try {
        val data = healthManager.getTodayHealthData(taskType)
        Log.d("HealthConnect", "Fetched data for $taskType: $data")

        onDataReceived(data)
    } catch (e: Exception) {
        Log.e("HealthConnect", "Error fetching health data: ${e.message}", e)
        onDataReceived(0.0) // Fallback to 0 on error
    }
}