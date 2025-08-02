package neth.iecal.questphone.ui.screens.quest.view.health_connect

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
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.launch
import neth.iecal.questphone.core.utils.managers.HealthConnectManager
import neth.iecal.questphone.core.utils.managers.HealthConnectManager.Companion.requiredPermissions
import neth.iecal.questphone.core.utils.managers.QuestHelper
import neth.iecal.questphone.core.utils.managers.User
import neth.iecal.questphone.ui.screens.quest.checkForRewards
import neth.iecal.questphone.ui.screens.quest.view.BaseQuestView
import neth.iecal.questphone.ui.screens.quest.view.components.MdPad
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.backend.QuestDatabaseProvider
import nethical.questphone.backend.StatsDatabaseProvider
import nethical.questphone.backend.StatsInfo
import nethical.questphone.core.core.utils.getCurrentDate
import nethical.questphone.data.game.xpToRewardForQuest
import nethical.questphone.data.json
import nethical.questphone.data.quest.health.HealthQuest
import nethical.questphone.data.quest.health.HealthTaskType
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

    var isQuestComplete =
        remember { mutableStateOf(commonQuestInfo.last_completed_on == getCurrentDate()) }
    val hasRequiredPermissions = remember { mutableStateOf(false) }
    val currentHealthData = remember { mutableDoubleStateOf(0.0) }
    val progressState = remember { mutableFloatStateOf(if (isQuestComplete.value) 1f else 0f) }

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
                    user_id = User!!.getUserId(),
                    )
            )
        }
        checkForRewards(commonQuestInfo)
        isQuestComplete.value = true
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
        onResult = { granted ->
            hasRequiredPermissions.value = granted.containsAll(requiredPermissions)
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
                permissionLauncher.launch(requiredPermissions)
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
                )
                Text(
                    text = (if (isQuestComplete.value) "Next Reward" else "Reward") + ": ${commonQuestInfo.reward} coins + ${
                        xpToRewardForQuest(
                            User!!.userInfo.level
                        )
                    } xp",
                    style = MaterialTheme.typography.bodyLarge,
                )

                Text(
                    text = "Health Task Type: ${healthQuest.type.label}",
                    style = MaterialTheme.typography.bodyLarge
                )

                if (isQuestComplete.value) {
                    Text(
                        text = "Today Progress: ${
                            String.format(
                                "%.3f",
                                currentHealthData.doubleValue
                            )
                        }",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
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
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
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