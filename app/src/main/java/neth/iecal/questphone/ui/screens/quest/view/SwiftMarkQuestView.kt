package neth.iecal.questphone.ui.screens.quest.view

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
import neth.iecal.questphone.ui.screens.quest.view.components.MdPad
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.backend.QuestDatabaseProvider
import nethical.questphone.backend.StatsDatabaseProvider
import nethical.questphone.backend.StatsInfo
import nethical.questphone.core.core.utils.formatHour
import nethical.questphone.core.core.utils.getCurrentDate
import nethical.questphone.data.game.xpToRewardForQuest
import java.util.UUID

@Composable
fun SwiftMarkQuestView(
    commonQuestInfo: CommonQuestInfo
) {
    val context = LocalContext.current
    val questHelper = QuestHelper(context)
    val isQuestComplete = remember {
        mutableStateOf(
            commonQuestInfo.last_completed_on == getCurrentDate()
        )
    }
    val scope = rememberCoroutineScope()
    val dao = QuestDatabaseProvider.getInstance(context).questDao()

    val isInTimeRange = remember { mutableStateOf(QuestHelper.Companion.isInTimeRange(commonQuestInfo)) }
    val isFailed = remember { mutableStateOf(QuestHelper.isOver(commonQuestInfo)) }

    val progress = remember {
        mutableFloatStateOf(if (isQuestComplete.value || isFailed.value ) 1f else 0f)
    }


    fun onQuestCompleted(){
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
                    user_id = User?.getUserId() ?: "",

                    )
            )
        }
        rewardUserForQuestCompl(commonQuestInfo)
        isQuestComplete.value = true
    }

    BaseQuestView(
        hideStartQuestBtn = isQuestComplete.value || !isInTimeRange.value || isFailed.value,
        onQuestStarted = {
            onQuestCompleted()
        },
        progress = progress,
        loadingAnimationDuration = 400,
        startButtonTitle = "Mark as complete",
        isFailed = isFailed,
        onQuestCompleted = {
            onQuestCompleted()
        },
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
                text = (if(isQuestComplete.value) "Next Reward" else "Reward") + ": ${commonQuestInfo.reward} coins + ${
                    xpToRewardForQuest(
                        User!!.userInfo.level
                    )
                } xp",
                style = MaterialTheme.typography.bodyLarge
            )


            if(!isInTimeRange.value){
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