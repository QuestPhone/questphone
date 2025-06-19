package launcher.launcher.ui.screens.quest.view

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
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import launcher.launcher.data.game.getUserInfo
import launcher.launcher.data.game.xpToRewardForQuest
import launcher.launcher.data.quest.CommonQuestInfo
import launcher.launcher.data.quest.QuestDatabaseProvider
import launcher.launcher.data.quest.stats.StatsDatabaseProvider
import launcher.launcher.data.quest.stats.StatsInfo
import launcher.launcher.ui.screens.quest.checkForRewards
import launcher.launcher.ui.screens.quest.view.components.MdPad
import launcher.launcher.ui.theme.JetBrainsMonoFont
import launcher.launcher.utils.QuestHelper
import launcher.launcher.utils.Supabase
import launcher.launcher.utils.formatHour
import launcher.launcher.utils.getCurrentDate
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

    val isInTimeRange = remember { mutableStateOf(QuestHelper.isInTimeRange(commonQuestInfo)) }
    val isFailed = remember { mutableStateOf(questHelper.isOver(commonQuestInfo)) }

    val progress = remember {
        mutableFloatStateOf(if (isQuestComplete.value || isFailed.value ) 1f else 0f)
    }


    val userInfo = getUserInfo(LocalContext.current)

    fun onQuestCompleted(){
        progress.floatValue = 1f
        commonQuestInfo.last_completed_on = getCurrentDate()
        commonQuestInfo.synced = false
        commonQuestInfo.last_updated = System.currentTimeMillis()
        scope.launch {
            dao.upsertQuest(commonQuestInfo)

            val userid = Supabase.supabase.auth.currentUserOrNull()!!.id
            val statsDao = StatsDatabaseProvider.getInstance(context).statsDao()
            statsDao.upsertStats(StatsInfo(
                id =  UUID.randomUUID().toString(),
                quest_id = commonQuestInfo.id,
                user_id = userid,
            ))
        }
        checkForRewards(commonQuestInfo)
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
                fontFamily = JetBrainsMonoFont,
                modifier = Modifier.padding(top = 40.dp)
            )

            Text(
                text = (if(isQuestComplete.value) "Next Reward" else "Reward") + ": ${commonQuestInfo.reward} coins + ${xpToRewardForQuest(userInfo.level)} xp",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
            )


            if(!isInTimeRange.value){
                Text(
                    text = "Time: ${formatHour(commonQuestInfo.time_range[0])} to ${formatHour(commonQuestInfo.time_range[1])}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Thin)
                )
            }

            MdPad(commonQuestInfo)
        }
    }
}