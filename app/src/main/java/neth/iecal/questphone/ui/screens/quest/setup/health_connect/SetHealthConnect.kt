package neth.iecal.questphone.ui.screens.quest.setup.health_connect

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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import neth.iecal.questphone.data.quest.health.HealthQuest
import neth.iecal.questphone.data.quest.health.HealthTaskType
import neth.iecal.questphone.ui.screens.quest.setup.ReviewDialog
import neth.iecal.questphone.ui.screens.quest.setup.SetBaseQuest
import neth.iecal.questphone.utils.json

@SuppressLint("UnrememberedMutableState")
@Composable
fun SetHealthConnect(editQuestId:String? = null,navController: NavHostController) {
    val scrollState = rememberScrollState()
    val haptic = LocalHapticFeedback.current
    val questInfoState = remember { QuestInfoState(initialIntegrationId = IntegrationId.HEALTH_CONNECT) }
    val healthQuest = remember { mutableStateOf(HealthQuest()) }
    val isReviewDialogVisible = remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if(editQuestId!=null){
            val dao = QuestDatabaseProvider.getInstance(context).questDao()
            val quest = dao.getQuest(editQuestId)
            questInfoState.fromBaseQuest(quest!!)
            healthQuest.value = json.decodeFromString<HealthQuest>(quest.quest_json)
        }
    }

    if (isReviewDialogVisible.value) {
        healthQuest.value.nextGoal = healthQuest.value.healthGoalConfig.initial
        val baseQuest = questInfoState.toBaseQuest(healthQuest.value)
        ReviewDialog(
            items = listOf(
                baseQuest, healthQuest.value
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

    Scaffold() { paddingValues ->
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
                        text = "Health Connect",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    )

                    SetBaseQuest(questInfoState, isTimeRangeSupported = false)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Health Goal Settings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Task Type Dropdown
                            HealthTaskTypeSelector(
                                selectedType = healthQuest.value.type,
                                onTypeSelected = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    healthQuest.value = healthQuest.value.copy(type = it) }
                            )

                            // Goal Config Inputs
                            GoalConfigInput(
                                label = "Initial Count",
                                value = healthQuest.value.healthGoalConfig.initial.toString(),
                                onValueChange = {
                                    val newValue = it.toIntOrNull() ?: 0
                                    healthQuest.value = healthQuest.value.copy(
                                        healthGoalConfig = healthQuest.value.healthGoalConfig.copy(initial = newValue)
                                    )
                                },
                                unit = healthQuest.value.type.unit
                            )

                            GoalConfigInput(
                                label = "Increment Daily By",
                                value = healthQuest.value.healthGoalConfig.increment.toString(),
                                onValueChange = {
                                    val newValue = it.toIntOrNull() ?: 0
                                    healthQuest.value = healthQuest.value.copy(
                                        healthGoalConfig = healthQuest.value.healthGoalConfig.copy(increment = newValue)
                                    )
                                },
                                unit = healthQuest.value.type.unit
                            )
                            GoalConfigInput(
                                label = "Final Count",
                                value = healthQuest.value.healthGoalConfig.final.toString(),
                                onValueChange = {
                                    val newValue = it.toIntOrNull() ?: 0
                                    healthQuest.value = healthQuest.value.copy(
                                        healthGoalConfig = healthQuest.value.healthGoalConfig.copy(final = newValue)
                                    )
                                },
                                unit = healthQuest.value.type.unit
                            )

                            }

                    Button(
                        enabled = questInfoState.selectedDays.isNotEmpty(),
                        onClick = { isReviewDialogVisible.value = true },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthTaskTypeSelector(
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
                .menuAnchor(),
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
fun GoalConfigInput(
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

