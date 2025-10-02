package neth.iecal.questphone.app.screens.quest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import neth.iecal.questphone.app.navigation.RootRoute
import neth.iecal.questphone.backed.repositories.QuestRepository
import neth.iecal.questphone.core.utils.managers.QuestHelper
import neth.iecal.questphone.data.CommonQuestInfo
import neth.iecal.questphone.data.toAdv

@Composable
fun ViewQuest(
    navController: NavHostController,
    questRepository: QuestRepository,
    id: String
) {
    val context = LocalContext.current

    val showDestroyQuestDialog = remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    var commonQuestInfo by remember { mutableStateOf<CommonQuestInfo?>(null) }

    LaunchedEffect(Unit) {
        commonQuestInfo = questRepository.getQuestById(id)
    }

    Surface {
        if(commonQuestInfo!=null) {
            if (QuestHelper.Companion.isNeedAutoDestruction(commonQuestInfo!!)) {
                showDestroyQuestDialog.value = true
            } else {
                commonQuestInfo!!.integration_id.toAdv().viewScreen.invoke(commonQuestInfo!!)
            }
            if (showDestroyQuestDialog.value)
                DestroyQuestDialog {
                commonQuestInfo!!.is_destroyed = true
                commonQuestInfo!!.synced = false
                commonQuestInfo!!.last_updated = System.currentTimeMillis()
                scope.launch {
                    questRepository.upsertQuest(commonQuestInfo!!)
                }
                    navController.navigate(RootRoute.HomeScreen.route) {
                        popUpTo(RootRoute.ViewQuest.route) { inclusive = true }
                    }
            }
        }
    }

}

@Composable
private fun DestroyQuestDialog(onDismiss: () -> Unit) {

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "This Quest has been destroyed.....",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    }

}