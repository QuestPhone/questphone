package neth.iecal.questphone.app.screens.quest.view

import android.app.Application
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import neth.iecal.questphone.app.screens.components.TopBarActions
import neth.iecal.questphone.app.screens.quest.view.components.MdPad
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.backend.QuestDatabaseProvider
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.backend.repositories.StatsRepository
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.core.core.utils.VibrationHelper
import nethical.questphone.core.core.utils.formatHour
import nethical.questphone.data.game.InventoryItem
import nethical.questphone.data.game.xpToRewardForQuest
import javax.inject.Inject

@HiltViewModel
class SwiftMarkQuestViewVModel @Inject constructor (questRepository: QuestRepository,
                                                    userRepository: UserRepository, statsRepository: StatsRepository,
                                                    application: Application
) : ViewQuestVM(questRepository, userRepository, statsRepository, application)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwiftMarkQuestView(
    commonQuestInfo: CommonQuestInfo,
    viewModel: SwiftMarkQuestViewVModel = hiltViewModel()
) {

    val context = LocalContext.current

    val isQuestComplete by viewModel.isQuestComplete.collectAsState()
    val isInTimeRange by viewModel.isInTimeRange.collectAsState()
    val progress by viewModel.progress.collectAsState()

    val scope = rememberCoroutineScope()
    val dao = QuestDatabaseProvider.getInstance(context).questDao()

    val hideStartQuestBtn = isQuestComplete || !isInTimeRange
    val coins by viewModel.coins.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.setCommonQuest(commonQuestInfo)
    }
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
                if(!hideStartQuestBtn) {
                    Spacer(modifier = Modifier.width(15.dp))
                    Button(
                        onClick = {
                            VibrationHelper.vibrate(100)
                            viewModel.saveQuestToDb()
                        }
                    ) {
                        Text(text = "Start Quest")
                    }
                }
            }
        }) { innerPadding ->

        Column(
            modifier = Modifier.padding(innerPadding)
                .padding(8.dp)
        ) {
            Text(
                text = commonQuestInfo.title,
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                textDecoration = if(hideStartQuestBtn) TextDecoration.LineThrough else TextDecoration.None
            )

            Text(
                text = (if (isQuestComplete) "Next Reward" else "Reward") + ": ${commonQuestInfo.reward} coins + ${
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

            MdPad(commonQuestInfo)
        }
    }
}