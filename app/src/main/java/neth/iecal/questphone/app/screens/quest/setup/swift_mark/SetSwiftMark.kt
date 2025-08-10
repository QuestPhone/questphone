package neth.iecal.questphone.app.screens.quest.setup.swift_mark

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import neth.iecal.questphone.app.screens.quest.setup.CommonSetBaseQuest
import neth.iecal.questphone.app.screens.quest.setup.ReviewDialog
import neth.iecal.questphone.app.screens.quest.setup.QuestSetupViewModel
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.data.BaseIntegrationId
import javax.inject.Inject

@HiltViewModel
class SetSwiftMarkViewModelQuest @Inject constructor (questRepository: QuestRepository,
                                                      userRepository: UserRepository
): QuestSetupViewModel(questRepository, userRepository)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetSwiftMark(editQuestId:String? = null,navController: NavHostController, viewModel: SetSwiftMarkViewModelQuest = hiltViewModel()) {
    val questInfoState by viewModel.questInfoState.collectAsState()

    val scrollState = rememberScrollState()
    val isReviewDialogVisible by viewModel.isReviewDialogVisible.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadQuestData(editQuestId, BaseIntegrationId.SWIFT_MARK)
    }

    if (isReviewDialogVisible) {
        val baseQuest = viewModel.getBaseQuestInfo()
        ReviewDialog(
            items = listOf(
                baseQuest
            ),
            onConfirm = {
                viewModel.addQuestToDb("",1) {
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
                        text = "Swift Quest",
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
                Button(
                    enabled = questInfoState.selectedDays.isNotEmpty(),
                    onClick = {
                        viewModel.isReviewDialogVisible.value = true
                    },
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