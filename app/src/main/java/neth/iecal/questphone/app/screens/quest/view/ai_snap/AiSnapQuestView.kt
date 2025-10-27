package neth.iecal.questphone.app.screens.quest.view.ai_snap

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import neth.iecal.questphone.AiSnapQuestViewVM
import neth.iecal.questphone.app.screens.components.TopBarActions
import neth.iecal.questphone.app.screens.quest.view.components.MdPad
import neth.iecal.questphone.app.screens.quest.view.dialogs.QuestSkipperDialog
import neth.iecal.questphone.app.theme.LocalCustomTheme
import neth.iecal.questphone.app.theme.smoothYellow
import neth.iecal.questphone.data.CommonQuestInfo
import nethical.questphone.core.core.utils.VibrationHelper
import nethical.questphone.core.core.utils.formatHour
import nethical.questphone.data.game.InventoryItem
import nethical.questphone.data.xpToRewardForQuest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSnapQuestView(
    commonQuestInfo: CommonQuestInfo,
    viewModel: AiSnapQuestViewVM = hiltViewModel()
) {
    val isQuestComplete by viewModel.isQuestComplete.collectAsState()
    val isCameraScreen by viewModel.isCameraScreen.collectAsState()
    val isAiEvaluating by viewModel.isAiEvaluating.collectAsState()


    val isInTimeRange by viewModel.isInTimeRange.collectAsState()
    val coins by viewModel.coins.collectAsState()

    val isHideStartButton = isQuestComplete || !isInTimeRange

    val scrollState = rememberScrollState()
    BackHandler(isCameraScreen || isAiEvaluating) {
        viewModel.isCameraScreen.value = false
        viewModel.isAiEvaluating.value = false
    }

    LaunchedEffect(Unit) {
        viewModel.setCommonQuest(commonQuestInfo)
        viewModel.setAiSnap()
    }

    if(isAiEvaluating) {
        AiEvaluationScreen({
            viewModel.isCameraScreen.value = false
            viewModel.isAiEvaluating.value = false}, viewModel)
    } else if (isCameraScreen) {
        CameraScreen({
            viewModel.isAiEvaluating.value = true
            viewModel.evaluateQuest {
                viewModel.onAiSnapQuestDone()
            }
        })
    }
    else {

        Scaffold(
            Modifier.safeDrawingPadding(),
            containerColor = LocalCustomTheme.current.getRootColorScheme().surface,
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
                                viewModel.isCameraScreen.value = true
                            }
                        ) {
                            Text(text = "Snap Pic")
                        }
                    }
                }
            }) { innerPadding ->
            QuestSkipperDialog(viewModel)

            Column(
                modifier = Modifier.
                padding(innerPadding)
                    .padding(8.dp)
                    .verticalScroll(scrollState)
            ) {

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


                if (!isInTimeRange) {
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