package neth.iecal.questphone.app.screens.quest.setup.health_connect

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import neth.iecal.questphone.R
import neth.iecal.questphone.app.navigation.RootRoute
import neth.iecal.questphone.app.screens.quest.setup.CommonSetBaseQuest
import neth.iecal.questphone.app.screens.quest.setup.QuestSetupViewModel
import neth.iecal.questphone.app.screens.quest.setup.ReviewDialog
import neth.iecal.questphone.data.IntegrationId
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.data.BaseIntegrationId
import nethical.questphone.data.json
import nethical.questphone.data.quest.health.HealthQuest
import nethical.questphone.data.quest.health.HealthTaskType
import javax.inject.Inject

@HiltViewModel
class SetHealthConnectViewModelQuest @Inject constructor (questRepository: QuestRepository,
                                                          userRepository: UserRepository
): QuestSetupViewModel(questRepository, userRepository){
    val healthQuest = MutableStateFlow(HealthQuest())

    fun saveQuest(onSuccess: ()-> Unit){
        healthQuest.value.nextGoal = healthQuest.value.healthGoalConfig.initial
        addQuestToDb(json.encodeToString(healthQuest.value)) { onSuccess() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnrememberedMutableState")
@Composable
fun SetHealthConnect(editQuestId:String? = null,navController: NavHostController,viewModel: SetHealthConnectViewModelQuest = hiltViewModel()) {
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current

    val questInfoState by viewModel.questInfoState.collectAsState()
    val healthQuest by viewModel.healthQuest.collectAsState()
    val isReviewDialogVisible by viewModel.isReviewDialogVisible.collectAsState()


    LaunchedEffect(Unit) {
        viewModel.loadQuestData(editQuestId, BaseIntegrationId.HEALTH_CONNECT) {
            viewModel.healthQuest.value = json.decodeFromString<HealthQuest>(it.quest_json)
        }
    }

    if (isReviewDialogVisible) {
        val baseQuest = viewModel.getBaseQuestInfo()
        ReviewDialog(
            items = listOf(
                baseQuest, healthQuest
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
                        text = "Health Connect",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                actions = {
                    Icon(
                        painter = painterResource(R.drawable.outline_help_24),
                        contentDescription = "Help",
                        modifier = Modifier.clickable{
                            navController.navigate("${RootRoute.IntegrationTutorial.route}${IntegrationId.HEALTH_CONNECT.name}")
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
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),


                ) {

                CommonSetBaseQuest(
                    viewModel.userCreatedOn,
                    questInfoState,
                    isTimeRangeSupported = false
                )

                Text(
                    text = "Health Goal Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Task Type Dropdown
                HealthTaskTypeSelector(
                    selectedType = healthQuest.type,
                    onTypeSelected = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.healthQuest.value = healthQuest.copy(type = it)
                    }
                )

                // Goal Config Inputs
                GoalConfigInput(
                    label = "Initial Count",
                    value = healthQuest.healthGoalConfig.initial.toString(),
                    onValueChange = {
                        val newValue = it.toIntOrNull() ?: 0
                        viewModel.healthQuest.value = healthQuest.copy(
                            healthGoalConfig = healthQuest.healthGoalConfig.copy(initial = newValue)
                        )
                    },
                    unit = healthQuest.type.unit
                )

                GoalConfigInput(
                    label = "Increment Daily By",
                    value = healthQuest.healthGoalConfig.increment.toString(),
                    onValueChange = {
                        val newValue = it.toIntOrNull() ?: 0
                        viewModel.healthQuest.value = healthQuest.copy(
                            healthGoalConfig = healthQuest.healthGoalConfig.copy(increment = newValue)
                        )
                    },
                    unit = healthQuest.type.unit
                )
                GoalConfigInput(
                    label = "Final Count",
                    value = healthQuest.healthGoalConfig.final.toString(),
                    onValueChange = {
                        val newValue = it.toIntOrNull() ?: 0
                        viewModel.healthQuest.value = healthQuest.copy(
                            healthGoalConfig = healthQuest.healthGoalConfig.copy(final = newValue)
                        )
                    },
                    unit = healthQuest.type.unit
                )

                Button(
                    enabled = questInfoState.selectedDays.isNotEmpty(),
                    onClick = { viewModel.isReviewDialogVisible.value = true },
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthTaskTypeSelector(
    selectedType: HealthTaskType,
    onTypeSelected: (HealthTaskType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedType.name.lowercase().replaceFirstChar { it.uppercase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Activity Type") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select activity type"
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable, true),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize()
        ) {
            HealthTaskType.entries.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(type.label)
                    },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun GoalConfigInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { char -> char.isDigit() } || it.isEmpty()) onValueChange(it) },
        label = { Text(label) },
        trailingIcon = {
            Text(
                text = unit,
                modifier = Modifier.padding(end = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

