package neth.iecal.questphone.app.screens.quest.view.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import neth.iecal.questphone.app.screens.quest.view.ViewQuestVM
import nethical.questphone.core.core.utils.VibrationHelper
import nethical.questphone.data.game.InventoryItem

@Composable
fun QuestSkipperDialog(viewModel: ViewQuestVM) {
    val isDialogVisible by viewModel.isQuestSkippedDialogVisible.collectAsState()
    if (isDialogVisible) {
        Dialog(onDismissRequest = { viewModel.isQuestSkippedDialogVisible.value = false }) {
            Surface(modifier = Modifier.padding(8.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Do you want to use a QUEST SKIPPER to skip this quest for today?",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Available: ${viewModel.getInventoryItemCount(InventoryItem.QUEST_SKIPPER)}",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row {
                        Button(
                            onClick = {
                                viewModel.isQuestSkippedDialogVisible.value = false
                                VibrationHelper.vibrate(200)
                                viewModel.useItem(InventoryItem.QUEST_SKIPPER) {
                                    viewModel.saveQuestToDb()
                                }
                            },
                        ) {
                            Text("Yes")
                        }
                        Spacer(modifier = Modifier.size(16.dp))

                        Button(
                            onClick = {
                                viewModel.isQuestSkippedDialogVisible.value = false
                            },
                        ) {
                            Text("No")
                        }
                    }
                }
            }

        }
    }
}