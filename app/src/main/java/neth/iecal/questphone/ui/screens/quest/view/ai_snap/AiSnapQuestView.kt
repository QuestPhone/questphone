package neth.iecal.questphone.ui.screens.quest.view.ai_snap

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import neth.iecal.questphone.core.utils.managers.QuestHelper
import neth.iecal.questphone.core.utils.managers.User
import neth.iecal.questphone.ui.screens.game.rewardUserForQuestCompl
import neth.iecal.questphone.ui.screens.quest.view.BaseQuestView
import neth.iecal.questphone.ui.screens.quest.view.components.MdPad
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.backend.QuestDatabaseProvider
import nethical.questphone.backend.StatsDatabaseProvider
import nethical.questphone.backend.StatsInfo
import nethical.questphone.core.core.utils.formatHour
import nethical.questphone.core.core.utils.getCurrentDate
import nethical.questphone.data.game.xpToRewardForQuest
import nethical.questphone.data.json
import nethical.questphone.data.quest.ai.snap.AiSnap
import java.util.UUID

@Composable
fun AiSnapQuestView(
    commonQuestInfo: CommonQuestInfo
) {
    val context = LocalContext.current
    val questHelper = QuestHelper(context)
    val aiQuest = json.decodeFromString<AiSnap>(commonQuestInfo.quest_json)
    val isQuestComplete = remember {
        mutableStateOf(
            commonQuestInfo.last_completed_on == getCurrentDate()
        )
    }
    var isCameraScreen = remember { mutableStateOf(false) }
    var isAiEvaluating = remember { mutableStateOf(false) }


    val dao = QuestDatabaseProvider.getInstance(context).questDao()
    val scope = rememberCoroutineScope()

    val isInTimeRange = remember { mutableStateOf(QuestHelper.Companion.isInTimeRange(commonQuestInfo)) }
    val isFailed = remember { mutableStateOf(QuestHelper.isOver(commonQuestInfo)) }
    var progress = remember {
        mutableFloatStateOf(if (isQuestComplete.value || isFailed.value ) 1f else 0f)
    }
    BackHandler(isCameraScreen.value || isAiEvaluating.value) {
        isCameraScreen.value = false
        isAiEvaluating.value = false
    }

    fun onQuestComplete(){
        progress.floatValue = 1f
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
        isCameraScreen.value = false
        rewardUserForQuestCompl(commonQuestInfo)
        isQuestComplete.value = true
    }

    if(isAiEvaluating.value) {
        Log.d("aiQuest",aiQuest.toString())
        AiEvaluationScreen(isAiEvaluating,commonQuestInfo.id ?: "return error") {
            onQuestComplete()
        }
    } else if (isCameraScreen.value) {
        CameraScreen(isAiEvaluating)
    }
    else {
        BaseQuestView(
            hideStartQuestBtn = isQuestComplete.value || isFailed.value || !isInTimeRange.value,
            onQuestStarted = {
                scope.launch {
                    isCameraScreen.value = true
                }

            },
            progress = progress,
            loadingAnimationDuration = 400,
            startButtonTitle = "Click Image",
            isFailed = isFailed,
            onQuestCompleted = { onQuestComplete() },
            isQuestCompleted = isQuestComplete
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = commonQuestInfo.title,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                )

                Text(
                    text = (if (!isQuestComplete.value) "Reward" else "Next Reward") + ": ${commonQuestInfo.reward} coins + ${
                        xpToRewardForQuest(
                            User!!.userInfo.level
                        )
                    } xp",
                    style = MaterialTheme.typography.bodyLarge
                )

                if (!isInTimeRange.value) {
                    Text(
                        text = "Time: ${formatHour(commonQuestInfo.time_range[0])} to ${
                            formatHour(
                                commonQuestInfo.time_range[1]
                            )
                        }",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                MdPad(commonQuestInfo)

            }
        }
    }
}