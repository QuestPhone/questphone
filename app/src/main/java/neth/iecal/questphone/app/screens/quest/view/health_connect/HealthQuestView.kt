package neth.iecal.questphone.app.screens.quest.view.health_connect

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import neth.iecal.questphone.app.screens.components.TopBarActions
import neth.iecal.questphone.app.screens.quest.view.ViewQuestVM
import neth.iecal.questphone.app.screens.quest.view.components.MdPad
import neth.iecal.questphone.app.screens.quest.view.dialogs.QuestSkipperDialog
import neth.iecal.questphone.app.theme.smoothYellow
import neth.iecal.questphone.core.utils.managers.HealthConnectManager
import neth.iecal.questphone.core.utils.managers.HealthConnectManager.Companion.requiredPermissions
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.backend.repositories.StatsRepository
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.core.core.utils.VibrationHelper
import nethical.questphone.data.game.InventoryItem
import nethical.questphone.data.json
import nethical.questphone.data.quest.health.HealthQuest
import nethical.questphone.data.quest.health.HealthTaskType
import nethical.questphone.data.xpToRewardForQuest
import javax.inject.Inject

@HiltViewModel
class HealthQuestViewVM @Inject constructor (questRepository: QuestRepository,
                        userRepository: UserRepository, statsRepository: StatsRepository,
                        application: Application
): ViewQuestVM(questRepository, userRepository, statsRepository, application){
    val healthQuest = MutableStateFlow(HealthQuest())
    val hasRequiredPermissions = MutableStateFlow(false)
    val currentHealthProgress = MutableStateFlow(0.0)

    val healthManager = HealthConnectManager(application)

    /**
     * encode Health quest to common quest json
     */
    fun encodeToCommonQuest(){
        commonQuestInfo.quest_json = json.encodeToString(healthQuest)
    }

    /**
     * Decodes Health quest from common quest
     */
    fun decodeFromCommonQuest(){
        healthQuest.value = json.decodeFromString<HealthQuest>(commonQuestInfo.quest_json)
    }


    fun onHealthQuestDone(){
        healthQuest.value.incrementGoal()
        encodeToCommonQuest()
        saveQuestToDb()
    }
    fun checkPermissionHandlerResult(granted:Set<String>){
        hasRequiredPermissions.value = granted.containsAll(requiredPermissions)
    }
    suspend fun checkIfPermissionGranted(){
        hasRequiredPermissions.value = healthManager.hasAllPermissions()
    }

    fun loadCurrentHealthProgress(){
        if (hasRequiredPermissions.value) {
            viewModelScope.launch {
                fetchHealthData(healthManager, healthQuest.value.type) { data ->
                    currentHealthProgress.value = data
                    // Update progress based on nextGoal
                    progress.value =
                        (data / healthQuest.value.nextGoal).toFloat().coerceIn(0f, 1f)
                    if(data>=healthQuest.value.nextGoal){
                        if(!isQuestComplete.value){
                            onHealthQuestDone()
                        }
                    }
                }
            }
            Log.d("health result", currentHealthProgress.value.toString())
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("DefaultLocale")
@Composable
fun HealthQuestView(commonQuestInfo: CommonQuestInfo, viewModel: HealthQuestViewVM = hiltViewModel()) {

    val healthQuest by viewModel.healthQuest.collectAsState()


    val isQuestComplete by viewModel.isQuestComplete.collectAsState()
    val isInTimeRange by viewModel.isInTimeRange.collectAsState()
    val hasRequiredPermissions by viewModel.hasRequiredPermissions.collectAsState()
    val currentHealthData by viewModel.currentHealthProgress.collectAsState()
    val progressState by viewModel.progress.collectAsState()
    val coins by viewModel.coins.collectAsState()
    val activeBoosts by viewModel.activeBoosts.collectAsState()

    val scrollState = rememberScrollState()
    LaunchedEffect(Unit) {
        viewModel.setCommonQuest(commonQuestInfo)
        viewModel.decodeFromCommonQuest()
    }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
        onResult = { granted ->
            viewModel.checkPermissionHandlerResult(granted)
            viewModel.loadCurrentHealthProgress()
        }
    )


    LaunchedEffect(Unit) {
        viewModel.checkIfPermissionGranted()
        viewModel.loadCurrentHealthProgress()

        Log.d("health connect perm", hasRequiredPermissions.toString())
        val isHealthConnectAvailable = viewModel.healthManager.isAvailable()
        if (!isHealthConnectAvailable) {
            Log.d("HealthConnect", "Health Connect not available")
            return@LaunchedEffect
        }


    }


    if (!hasRequiredPermissions) {
        HealthConnectScreen(
            onGetStarted = {
                permissionLauncher.launch(requiredPermissions)
            },
            onSkip = {
                viewModel.hasRequiredPermissions.value = true
            }
        )
    } else {

        Scaffold( Modifier.safeDrawingPadding(),
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

                }
            }) { innerPadding ->
            QuestSkipperDialog(viewModel)

            Column(modifier = Modifier.padding(innerPadding)
                .padding(8.dp)
                .verticalScroll(scrollState)) {
                Text(
                    text = commonQuestInfo.title,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
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
                    if(!isQuestComplete && viewModel.isBoosterActive(InventoryItem.XP_BOOSTER)) {
                        Text(
                            text = " + ",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Black,
                            color = smoothYellow
                        )
                        Image(painter = painterResource( InventoryItem.XP_BOOSTER.icon),
                            contentDescription = InventoryItem.XP_BOOSTER.simpleName,
                            Modifier.size(20.dp))
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
                    text = "Health Task Type: ${healthQuest.type.label}",
                    style = MaterialTheme.typography.bodyLarge
                )

                if (isQuestComplete) {
                    Text(
                        text = "Today Progress: ${
                            String.format(
                                "%.3f",
                                currentHealthData
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
                                currentHealthData
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