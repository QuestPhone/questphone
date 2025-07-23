package neth.iecal.questphone.ui.screens.quest.setup.ai_snap

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import neth.iecal.questphone.data.IntegrationId
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.QuestInfoState
import neth.iecal.questphone.data.quest.ai.snap.AiSnap
import neth.iecal.questphone.ui.screens.quest.setup.ReviewDialog
import neth.iecal.questphone.ui.screens.quest.setup.SetBaseQuest
import neth.iecal.questphone.ui.screens.quest.setup.ai_snap.model.ModelDownloadDialog
import neth.iecal.questphone.utils.json


@SuppressLint("UnrememberedMutableState")
@Composable
fun SetAiSnap(editQuestId:String? = null,navController: NavHostController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // State for the quest
    val questInfoState = remember { QuestInfoState(initialIntegrationId = IntegrationId.AI_SNAP) }
    val taskDescription = remember { mutableStateOf("") }
    var features = remember { mutableStateListOf<String>() }

    val scope = rememberCoroutineScope()

    val isReviewDialogVisible = remember { mutableStateOf(false) }
    var isModelDownloadDialogVisible =remember{ mutableStateOf(false)}

    ModelDownloadDialog(modelDownloadDialogVisible = isModelDownloadDialogVisible)

    LaunchedEffect(Unit) {
        if(editQuestId!=null){
            val dao = QuestDatabaseProvider.getInstance(context).questDao()
            val quest = dao.getQuest(editQuestId)
            questInfoState.fromBaseQuest(quest!!)
            val aiSnap = json.decodeFromString<AiSnap>(quest.quest_json)
            taskDescription.value = aiSnap.taskDescription
           features.addAll(aiSnap.features)
//            spatialImageUri.value = aiSnap.spatialImageUrl
        }
    }

    // Review dialog before creating the quest
    if (isReviewDialogVisible.value) {
        val aiSnapQuest = AiSnap(
            taskDescription = taskDescription.value,
            features = features.toMutableList()
        )
        val baseQuest = questInfoState.toBaseQuest(aiSnapQuest)

        ReviewDialog(
            items = listOf(baseQuest, aiSnapQuest),
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

    Scaffold { paddingValues ->
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "AI Snap",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        )

                    }

                    // Base quest configuration
                    SetBaseQuest(questInfoState)

                    // Task description
                    OutlinedTextField(
                        value = taskDescription.value,
                        onValueChange = { taskDescription.value = it },
                        label = { Text("Task Description") },
                        placeholder = { Text("e.g., Clean the bedroom, Organize desk") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )


                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Image Features (Optional)",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            "Enter all the features that must be present in all snaps. Examples: a green wall, a green watch on hand etc",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AddRemoveListWithDialog(items =features)
                }

                // Create Quest button
                Button(
                    enabled = questInfoState.selectedDays.isNotEmpty(),
                    onClick = {
                        if (taskDescription.value.isNotBlank()) {
                            isReviewDialogVisible.value = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
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

@Composable
fun AddRemoveListWithDialog(
    modifier: Modifier = Modifier,
    items: SnapshotStateList<String>
) {
    var showDialog by remember { mutableStateOf(false) }
    var dialogInput by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.padding(16.dp)) {
        Button(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                showDialog = true }, Modifier.fillMaxWidth()) {
            Text("Add Item")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column {
            items.forEachIndexed { i,item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(item, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            items.removeAt(i) }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }


        // Dialog to add item
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Add New Feature") },
                text = {
                    TextField(
                        value = dialogInput,
                        onValueChange = { dialogInput = it },
                        placeholder = { Text("Enter feature description") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (dialogInput.isNotBlank()) {
                                items.add(dialogInput)
                                dialogInput = ""
                                showDialog = false
                            }
                        }
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            dialogInput = ""
                            showDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
