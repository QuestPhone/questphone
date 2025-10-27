
package neth.iecal.questphone.app.screens.quest.setup.ai_snap

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import neth.iecal.questphone.AiSnapQuestViewVM
import neth.iecal.questphone.BuildConfig
import neth.iecal.questphone.R
import neth.iecal.questphone.app.navigation.RootRoute
import neth.iecal.questphone.app.screens.etc.MarkdownComposer
import neth.iecal.questphone.app.screens.etc.MdComponent
import neth.iecal.questphone.app.screens.etc.parseMarkdown
import neth.iecal.questphone.app.screens.quest.setup.CommonSetBaseQuest
import neth.iecal.questphone.app.screens.quest.setup.QuestSetupViewModel
import neth.iecal.questphone.app.screens.quest.setup.ReviewDialog
import neth.iecal.questphone.app.screens.quest.setup.ai_snap.model.ModelDownloadDialog
import neth.iecal.questphone.app.screens.quest.view.ai_snap.AiEvaluationScreen
import neth.iecal.questphone.app.screens.quest.view.ai_snap.CameraScreen
import neth.iecal.questphone.backed.repositories.QuestRepository
import neth.iecal.questphone.backed.repositories.UserRepository
import neth.iecal.questphone.data.CommonQuestInfo
import neth.iecal.questphone.data.IntegrationId
import nethical.questphone.data.BaseIntegrationId
import nethical.questphone.data.json
import nethical.questphone.data.quest.ai.snap.AiSnap
import javax.inject.Inject

@HiltViewModel
class SetAiSnapViewModelQuest @Inject constructor (questRepository: QuestRepository,
                                                   userRepository: UserRepository
) : QuestSetupViewModel(questRepository, userRepository){
    val taskDescription = MutableStateFlow("")
    val features : SnapshotStateList<String> = mutableStateListOf()

    fun getAiQuest(): AiSnap{
        return AiSnap(
            taskDescription = taskDescription.value,
            features = features.toMutableList()
        )
    }

    fun saveQuest(onSuccess:()-> Unit){
        addQuestToDb(json.encodeToString(getAiQuest())) { onSuccess() }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnrememberedMutableState")
@Composable
fun SetAiSnap(editQuestId:String? = null,navController: NavHostController, viewModel: SetAiSnapViewModelQuest = hiltViewModel(),tester : AiSnapQuestViewVM = hiltViewModel()) {
    val scrollState = rememberScrollState()
    val questInfoState by viewModel.questInfoState.collectAsState()
    val taskDescription by viewModel.taskDescription.collectAsState()
    var features = viewModel.features
    val context = LocalContext.current
    val isReviewDialogVisible by viewModel.isReviewDialogVisible.collectAsState()
    var isModelDownloadDialogVisible = remember { mutableStateOf(false) }

    ModelDownloadDialog(modelDownloadDialogVisible = isModelDownloadDialogVisible)

    val parsedMarkdown = remember { mutableStateOf(listOf<MdComponent>()) }
    var isMdEditorVisible by remember { mutableStateOf(false) }


    val isCameraScreen by tester.isCameraScreen.collectAsState()
    val isAiEvaluating by tester.isAiEvaluating.collectAsState()
    if (isAiEvaluating) {
        AiEvaluationScreen(
            {
                tester.isCameraScreen.value = false
                tester.isAiEvaluating.value = false
            },
            tester
        )
    }
    BackHandler(isMdEditorVisible || isCameraScreen || isAiEvaluating) {
        isMdEditorVisible = false
        tester.isCameraScreen.value = false
        tester.isAiEvaluating.value = false
    }

    LaunchedEffect(Unit) {

        viewModel.loadQuestUpperData(editQuestId, BaseIntegrationId.AI_SNAP) {
            val aiSnap = json.decodeFromString<AiSnap>(it.quest_json)
            viewModel.taskDescription.value = aiSnap.taskDescription
            viewModel.features.addAll(aiSnap.features)
        }
    }


    // Review dialog before creating the quest
    if (isReviewDialogVisible) {
        val aiSnapQuest = viewModel.getAiQuest()
        val baseQuest = viewModel.getBaseQuestInfo()

        ReviewDialog(
            items = listOf(baseQuest, aiSnapQuest),
            onConfirm = {
                viewModel.saveQuest {
                    navController.popBackStack()
                }

            },
            onDismiss = {
                viewModel.isReviewDialogVisible.value = false
            }
        )
    }

    if (isMdEditorVisible) {
        MarkdownComposer(
            list = parsedMarkdown.value,
            generatedMarkdown = questInfoState
        )
        return
    }

    if (isAiEvaluating) {
        AiEvaluationScreen({
            tester.isCameraScreen.value = false
            tester.isAiEvaluating.value = false
        }, tester)
        return
    } else if (isCameraScreen) {
        CameraScreen({
            tester.isAiEvaluating.value = true
            tester.evaluateQuest {
            }
        })
        return
    } else {
        Scaffold(
            modifier = Modifier.safeDrawingPadding(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            text = "AI Snap",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                    },
                    actions = {
                        Icon(
                            painter = painterResource(R.drawable.outline_help_24),
                            contentDescription = "Help",
                            modifier = Modifier.clickable {
                                navController.navigate("${RootRoute.IntegrationDocs.route}${IntegrationId.AI_SNAP.name}")
                            }.size(30.dp)
                        )
                    }
                )
            }
        ) { paddingValues ->
            Box(Modifier.padding(paddingValues)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp)
                ) {


                    Column(
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                    ) {

                        // Base quest configuration
                        CommonSetBaseQuest(
                            viewModel.userCreatedOn,
                            questInfoState,
                            onAdvancedMdEditor = {
                                parsedMarkdown.value = parseMarkdown(questInfoState.instructions)

                                isMdEditorVisible = true
                            })

                        // Task description
                        OutlinedTextField(
                            value = taskDescription,
                            onValueChange = { viewModel.taskDescription.value = it },
                            label = { Text("Task Description in extreme details. Explain it as if explaining to a 5yo") },
                            placeholder = { Text("e.g., A Clean Bedroom, An organized desk") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4
                        )

                        if (BuildConfig.IS_FDROID) {
                            Text(text = "This uses a non-finetuned variant of siglip2 that works well for object detection but is not really impressive for contextual and real world tasks. Please write an appropriate prompt, test and explore before adding quest. ")
                        }

                        Button(onClick = {
                            if(taskDescription.isEmpty()) return@Button
                            tester.isCameraScreen.value = true

                            val aiSnap =
                                AiSnap(taskDescription = taskDescription, features.toMutableList())
                            tester.setCommonQuest(
                                CommonQuestInfo(
                                    integration_id = BaseIntegrationId.AI_SNAP,
                                    quest_json = json.encodeToString(aiSnap)
                                )
                            )
                            tester.setAiSnap()
                        }) {
                            Text("Test Prompt")
                        }

                        if (!BuildConfig.IS_FDROID) {
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

                            AddRemoveListWithDialog(items = features)
                        }
                    }

                    Button(
                        enabled = questInfoState.selectedDays.isNotEmpty() && taskDescription.isNotBlank(),
                        onClick = {
                            if (taskDescription.isNotBlank()) {
                                viewModel.isReviewDialogVisible.value = true
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
                                text = if (editQuestId == null) "Create Quest" else "Save Changes",

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
@Composable
private fun AddRemoveListWithDialog(
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
