@file:OptIn(ExperimentalMaterial3Api::class)

package neth.iecal.questphone.app.screens.quest.templates

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import neth.iecal.questphone.R
import neth.iecal.questphone.app.screens.quest.setup.ai_snap.model.ModelDownloadDialog
import neth.iecal.questphone.app.screens.quest.setup.components.DateSelector
import neth.iecal.questphone.app.screens.quest.setup.components.TimeRangeDialog
import neth.iecal.questphone.app.screens.quest.setup.deep_focus.SelectAppsDialog
import neth.iecal.questphone.data.TemplateVariable
import neth.iecal.questphone.data.VariableType
import nethical.questphone.core.core.utils.getCurrentDate
import nethical.questphone.core.core.utils.managers.formatAppList
import nethical.questphone.core.core.utils.readableTimeRange
import nethical.questphone.data.BaseIntegrationId
import nethical.questphone.data.DayOfWeek
import nethical.questphone.data.json

@SuppressLint("MutableCollectionMutableState")
@Composable
fun SetupTemplate(controller: NavController,viewModel: TemplatesViewModel) {
    val isLoading by viewModel.isLoading.collectAsState()
    val templateContent by viewModel.selectedTemplateContent.collectAsState()
    val variableValues by viewModel.variableValues.collectAsState()

    var showSaveConfirmation by remember { mutableStateOf(false) }
    var currentVariable by remember { mutableStateOf<TemplateVariable?>(null) }
    var isModelDownloadDialogVisible =remember{ mutableStateOf(false)}

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Setup Template",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (!isLoading && templateContent != null) {
                FloatingActionButton(
                    onClick = { showSaveConfirmation = true },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_check_24),
                        contentDescription = "Save"
                    )
                }
            }
        }
    ) { padding ->
        if(templateContent?.basicQuest?.integration_id == BaseIntegrationId.AI_SNAP){
            ModelDownloadDialog(modelDownloadDialogVisible = isModelDownloadDialogVisible)
        }
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading template...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            templateContent?.let { data ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Info banner
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (getCurrentDate() == viewModel.userCreatedOn)"Click on the highlighted items to change values" else "Fake a quest if you want. It'll sit in your history, reminding you you're a fraud. Real ones can ignore this, you’ve got nothing to hide.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 80.dp) // Space for FAB
                    ) {
                        ClickableTemplateText(
                            content = data.content.replace("#{userName}", viewModel.username),
                            variables = data.variableTypes,
                            variableValues = variableValues,
                            onVariableClick = { variable ->
                                currentVariable = variable
                            }
                        )
                    }
                }
            } ?: Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Template not available",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Unable to load template data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Variable edit dialog
    if (currentVariable != null) {
        VariableEditDialog(
            variable = currentVariable!!,
            initialValue = variableValues[currentVariable!!.name] ?: "",
            onDismiss = { currentVariable = null },
            onSave = { name, value ->
                viewModel.setVariable(name,value)
            }
        )
    }

    // Save confirmation dialog
    if (showSaveConfirmation) {
        SaveConfirmationDialog(
            allVariablesFilled = viewModel.areAllVariablesFilled(),
            unfilledCount = templateContent?.questExtraVariableDeclaration?.count { variable ->
                val value = variableValues[variable.name]
                value.isNullOrBlank() || value == "Not set"
            } ?: 0,
            onDismiss = { showSaveConfirmation = false },
            onConfirm = {
                // Handle save logic here
                showSaveConfirmation = false
                viewModel.addToQuests {
                    controller.popBackStack()
                    showSaveConfirmation = false
                }
            }
        )
    }
}

@Composable
private fun ClickableTemplateText(
    content: String,
    variables: List<TemplateVariable>,
    variableValues: Map<String, String>,
    onVariableClick: (TemplateVariable) -> Unit
) {
    val variableRegex = Regex("#\\{([^}]+)\\}")

    val annotatedString = buildAnnotatedString {
        var lastIndex = 0
        for (match in variableRegex.findAll(content)) {
            if (match.range.first > lastIndex) {
                append(content.substring(lastIndex, match.range.first))
            }
            val varName = match.groupValues[1]
            val variable = variables.find { it.name == varName }
            val value = variableValues[varName]
            var displayText = if (!value.isNullOrBlank() && value != "Not set") {
                value
            } else {
                variable?.getDefaultValue() ?: ""
            }

            if (variable != null) {
                pushStringAnnotation(tag = "VARIABLE", annotation = varName)
                withStyle(
                    style = SpanStyle(
                        color = if (!value.isNullOrBlank() && value != "Not set")
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                        background = if (!value.isNullOrBlank() && value != "Not set")
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    when(variable.type){
                        VariableType.daysOfWeek -> {
                            val v = json.decodeFromString<Set<DayOfWeek>>(displayText)
                            displayText = if(v.size == 7) "Everyday" else v.joinToString(", ") { it.name }
                        }
                        VariableType.date -> {
                            if(displayText=="9999-06-21") {
                                displayText = "forever"
                            }
                        }
                        VariableType.timeRange -> {
                            val v = json.decodeFromString<List<Int>>(displayText)
                            displayText = readableTimeRange(v)
                        }
                        VariableType.text -> {}
                        VariableType.number -> {}
                        VariableType.appSelector -> {
                            displayText = formatAppList(json.decodeFromString(displayText),LocalContext.current)
                        }
                    }
                    displayText = variable.label + ": " + displayText
                    append(displayText)

                }
                pop()
            } else {
                append(match.value)
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < content.length) {
            append(content.substring(lastIndex))
        }
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3
        ),
        modifier = Modifier.fillMaxWidth(),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "VARIABLE", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    val variable = variables.find { it.name == annotation.item }
                    if (variable != null) {
                        onVariableClick(variable)
                    }
                }
        }
    )
}

@Composable
private fun VariableEditDialog(
    variable: TemplateVariable,
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var textValue by remember { mutableStateOf(initialValue) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Edit ${variable.label}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                when (variable.type) {

                    VariableType.daysOfWeek -> {
                        Text(
                            text = "Select days:",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        DaysOfWeekSelectorDialog(
                            initialSelected = if(textValue.isEmpty()) emptySet() else json.decodeFromString<Set<DayOfWeek>>(textValue) ,
                            onSelectionChanged = { selected ->
                                textValue = json.encodeToString(selected)
                            }
                        )
                    }
                    VariableType.date -> {
                        DateSelector("Date") {
                            textValue = it
                            onSave(variable.name, textValue.ifBlank { "9999-12-31" })
                        }
                    }
                    VariableType.timeRange -> {
                        var v = (if(textValue.isEmpty()){
                            listOf(0,24)
                        } else {
                            json.decodeFromString(textValue)
                        })
                        TimeRangeDialog(
                            v[0], v[1],
                            onDismiss = onDismiss

                        ) { i,e ->
                            v = listOf(i,e)
                            textValue = json.encodeToString(v)
                            onSave(variable.name, textValue.ifBlank { variable.getDefaultValue().toString() })

                            Log.d("time",v.toString())
                            onDismiss()
                        }
                    }
                    VariableType.text -> {
                        OutlinedTextField(
                            value = textValue,
                            onValueChange = { textValue = it },
                            label = { Text(variable.label) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            maxLines = 5,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    VariableType.number ->  OutlinedTextField(
                        value = textValue,
                        onValueChange = {
                            try{
                                it.toInt()
                                textValue = it
                            }catch (_: Exception){} },
                        label = { Text(variable.label) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = false,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp)
                    )

                    VariableType.appSelector -> {
                        var selectedApps = remember { mutableStateListOf<String>() }
                        SelectAppsDialog(selectedApps,onDismiss, onConfirm = {
                            textValue = json.encodeToString(selectedApps.toSet())
                            onSave(variable.name, textValue.ifBlank { variable.getDefaultValue().toString() })
                            onDismiss()
                        })
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            onDismiss()
                            onSave(variable.name, textValue.ifBlank { "Not set" }) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun DaysOfWeekSelectorDialog(
    initialSelected: Set<DayOfWeek>,
    onSelectionChanged: (Set<DayOfWeek>) -> Unit
) {
    var selectedDays by remember { mutableStateOf(initialSelected) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        LazyColumn(
            modifier = Modifier
                .heightIn(max = 200.dp)
                .padding(8.dp)
        ) {
            items(DayOfWeek.entries) { day ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedDays =
                                if (selectedDays.contains(day)) selectedDays - day else selectedDays + day
                            onSelectionChanged(selectedDays)
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    Checkbox(
                        checked = selectedDays.contains(day),
                        onCheckedChange = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = day.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun SaveConfirmationDialog(
    allVariablesFilled: Boolean,
    unfilledCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.baseline_check_24),
                contentDescription = null,
                tint = if (allVariablesFilled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = if (allVariablesFilled) "Save Quest?" else "Incomplete Quest",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = if (allVariablesFilled) {
                    "All variables have been configured. Save this Quest configuration?"
                } else {
                    "You have $unfilledCount unfilled variable${if (unfilledCount != 1) "s" else ""}. "
                },
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = allVariablesFilled,
                colors = if (allVariablesFilled) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}