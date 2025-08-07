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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import neth.iecal.questphone.ui.screens.quest.setup.CommonSetBaseQuest
import neth.iecal.questphone.ui.screens.quest.setup.ReviewDialog
import neth.iecal.questphone.ui.screens.quest.setup.SetupViewModel
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.data.BaseIntegrationId
import nethical.questphone.data.json
import nethical.questphone.data.quest.focus.DeepFocus
import nethical.questphone.data.quest.focus.FocusTimeConfig
import javax.inject.Inject

@HiltViewModel
class SetDeepFocusViewModel @Inject constructor(
    questRepository: QuestRepository, userRepository: UserRepository
) : SetupViewModel(questRepository, userRepository){
    var selectedApps :SnapshotStateList<String> = mutableStateListOf()

    var focusTimeConfig = MutableStateFlow(FocusTimeConfig())
    var showAppSelectionDialog = MutableStateFlow(false)

    fun getDeepFocusQuest(): DeepFocus{
        return DeepFocus(
            focusTimeConfig = focusTimeConfig.value,
            unrestrictedApps = selectedApps.toSet(),
            nextFocusDurationInMillis = focusTimeConfig.value.initialTimeInMs
        )
    }
    fun saveQuest(onSuccess:()-> Unit){
        addQuestToDb(json.encodeToString(getDeepFocusQuest())) { onSuccess() }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnrememberedMutableState")
@Composable
fun SetDeepFocus(editQuestId:String? = null,navController: NavHostController, viewModel: SetDeepFocusViewModel = hiltViewModel()) {

    val haptic = LocalHapticFeedback.current
    val showAppSelectionDialog by viewModel.showAppSelectionDialog.collectAsState()
    val selectedApps = viewModel.selectedApps
    val focusTimeConfig by viewModel.focusTimeConfig.collectAsState()
    val questInfoState by viewModel.questInfoState.collectAsState()
    val isReviewDialogVisible by viewModel.isReviewDialogVisible.collectAsState()

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.loadQuestData(editQuestId, BaseIntegrationId.DEEP_FOCUS) {
            val deepFocus = json.decodeFromString<DeepFocus>(it.quest_json)
            viewModel.focusTimeConfig.value = deepFocus.focusTimeConfig
            selectedApps.addAll(deepFocus.unrestrictedApps)
        }
    }
    if (showAppSelectionDialog) {
        SelectAppsDialog(
            selectedApps = viewModel.selectedApps,
            onDismiss = {
                viewModel.showAppSelectionDialog.value = false
            }
        )
    }
    if (isReviewDialogVisible) {
        val baseQuest = viewModel.getBaseQuestInfo()
        val deepFocus = viewModel.getDeepFocusQuest()
        ReviewDialog(
            items = listOf(
                baseQuest, deepFocus
            ),

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
    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        text = "Deep Focus",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                }
            )
        }
    )
    { paddingValues ->

        Box(Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),

            ) {

                CommonSetBaseQuest(viewModel.userCreatedOn,questInfoState)

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.showAppSelectionDialog.value = true
                    },
                ) {

                    Text(
                        text = "Selected App Exceptions ${selectedApps.size}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                SetFocusTimeUI(focusTimeConfig){
                    viewModel.focusTimeConfig.value = it
                }

                Button(
                    onClick = {
                        viewModel.isReviewDialogVisible.value = true

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