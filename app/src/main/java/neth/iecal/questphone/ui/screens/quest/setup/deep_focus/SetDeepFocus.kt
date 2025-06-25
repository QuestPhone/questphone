package neth.iecal.questphone.ui.screens.quest.setup.deep_focus

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.AppInfo
import neth.iecal.questphone.data.IntegrationId
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.QuestInfoState
import neth.iecal.questphone.data.quest.focus.DeepFocus
import neth.iecal.questphone.data.quest.focus.FocusTimeConfig
import neth.iecal.questphone.ui.screens.quest.setup.ReviewDialog
import neth.iecal.questphone.ui.screens.quest.setup.SetBaseQuest
import neth.iecal.questphone.ui.screens.quest.setup.components.SetFocusTimeUI
import neth.iecal.questphone.utils.QuestHelper
import neth.iecal.questphone.utils.json
import neth.iecal.questphone.utils.reloadApps

@SuppressLint("UnrememberedMutableState")
@Composable
fun SetDeepFocus(editQuestId:String? = null,navController: NavHostController) {
    val context = LocalContext.current
    val apps = remember { mutableStateOf(emptyList<AppInfo>()) }

    val showDialog = remember { mutableStateOf(false) }
    var selectedApps = remember { mutableStateListOf<String>() }
    val questInfoState = remember { QuestInfoState(initialIntegrationId = IntegrationId.DEEP_FOCUS) }
    val focusTimeConfig = remember { mutableStateOf(FocusTimeConfig()) }

    val scrollState = rememberScrollState()
    val sp = QuestHelper(LocalContext.current)

    val isReviewDialogVisible = remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if(editQuestId!=null){
            val dao = QuestDatabaseProvider.getInstance(context).questDao()
            val quest = dao.getQuest(editQuestId)
            questInfoState.fromBaseQuest(quest!!)
            val deepFocus = json.decodeFromString<DeepFocus>(quest.quest_json)
            focusTimeConfig.value = deepFocus.focusTimeConfig
            selectedApps.addAll(deepFocus.unrestrictedApps)
        }
    }
    LaunchedEffect(apps) {
        apps.value = reloadApps(context.packageManager, context).getOrNull() ?: emptyList()
    }

    if (showDialog.value) {
        SelectAppsDialog(
            apps = apps,
            selectedApps = selectedApps,
            onDismiss = {
                showDialog.value = false
            }
        )
    }
    if (isReviewDialogVisible.value) {
        val deepFocus = DeepFocus(
            focusTimeConfig = focusTimeConfig.value,
            unrestrictedApps = selectedApps.toSet(),
            nextFocusDurationInMillis = focusTimeConfig.value.initialTimeInMs
        )
        val baseQuest =
            questInfoState.toBaseQuest<DeepFocus>(deepFocus)

        ReviewDialog(
            items = listOf(
                baseQuest, deepFocus
            ),

            onConfirm = {
                scope.launch {
                    val dao = QuestDatabaseProvider.getInstance(context).questDao()
                    dao.upsertQuest(baseQuest)
                }
                isReviewDialogVisible.value = false
                navController.popBackStack()
            },
            onDismiss = {
                isReviewDialogVisible.value = false
            }
        )
    }
    Scaffold()
    { paddingValues ->

        Box(Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp)

            ) {

                Column(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(top = 32.dp)
                ) {

                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        text = "Deep Focus ",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    )
                    SetBaseQuest(questInfoState)

                    OutlinedButton(
                        onClick = { showDialog.value = true },
                    ) {

                        Text(
                            text = "Selected App Exceptions ${selectedApps.size}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    SetFocusTimeUI(focusTimeConfig)

                    Button(
                        onClick = {
                            isReviewDialogVisible.value = true

                        },
                        enabled = questInfoState.selectedDays.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Done,
                                contentDescription = "Done"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if(editQuestId==null) "Create Quest" else "Save Changes",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    Spacer(Modifier.size(100.dp))
                }

            }
        }


    }

}