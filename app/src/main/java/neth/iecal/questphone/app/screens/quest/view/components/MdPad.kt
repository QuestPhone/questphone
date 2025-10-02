package neth.iecal.questphone.app.screens.quest.view.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import dev.jeziellago.compose.markdowntext.MarkdownText
import neth.iecal.questphone.app.screens.etc.ComponentType
import neth.iecal.questphone.app.screens.etc.MdComponent
import neth.iecal.questphone.app.screens.etc.generateComponentMarkdown
import neth.iecal.questphone.app.screens.etc.generateMarkdown
import neth.iecal.questphone.app.screens.etc.parseMarkdown
import neth.iecal.questphone.data.CommonQuestInfo
import nethical.questphone.core.core.utils.getCurrentDate

@Composable
fun MdPad(commonQuestInfo: CommonQuestInfo) {

    val components = remember { mutableStateOf(listOf<MdComponent>()) }
    val componentPreview = remember { mutableStateListOf<Pair<ComponentType, String>>() }

    val context = LocalContext.current
    var currentRawMarkdown by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    fun parseCheckboxContent(content: String): Pair<Boolean, String> {
        val parts = content.split("|", limit = 2)
        if (parts.size < 2) return false to content  // fallback if malformed

        val isChecked = parts[0].equals("done", ignoreCase = true)
        val text = parts[1]
        return isChecked to text
    }

    fun reFetchComponents() {

        components.value = parseMarkdown(currentRawMarkdown)
        componentPreview.clear()
        components.value.forEach {
            componentPreview.add(Pair(it.type, generateComponentMarkdown(it)))
        }
        Log.d("components", components.value.toString())
        Log.d("components", componentPreview.toString())
    }
    LaunchedEffect(Unit) {
        val sp = context.getSharedPreferences("temp_instructions", Context.MODE_PRIVATE)
        val cachedDate = sp.getString("cached_date", "")
        if (cachedDate == getCurrentDate()) {
            if (sp.contains(commonQuestInfo.id)) {
                currentRawMarkdown =
                    sp.getString(commonQuestInfo.id, commonQuestInfo.instructions).toString()
            } else {
                currentRawMarkdown = commonQuestInfo.instructions
            }
        } else {
            sp.edit(commit = true) {
                clear()
                putString("cached_date", getCurrentDate())
            }
            currentRawMarkdown = commonQuestInfo.instructions
        }
        reFetchComponents()
    }


    fun saveToSharedPreferences(text: String) {
        val sp = context.getSharedPreferences("temp_instructions", Context.MODE_PRIVATE)
        sp.edit(commit = true) {
            putString(commonQuestInfo.id, text)
            putString("cached_date", getCurrentDate())
        }
    }


    Spacer(Modifier.size(32.dp))
    if (isEditing) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = {
                    if (isEditing) {
                        saveToSharedPreferences(currentRawMarkdown)
                        reFetchComponents()
                    }
                    isEditing = !isEditing
                }) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = if (isEditing) "Save" else "Edit"
                )
            }
        }
        TextField(
            value = currentRawMarkdown,
            onValueChange = { currentRawMarkdown = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 4.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    } else {
        componentPreview.forEachIndexed { i, it ->
            when (it.first) {
                ComponentType.CHECKBOX -> {
                    val (initialChecked, label) = parseCheckboxContent(components.value[i].content)
                    var checked by remember { mutableStateOf(initialChecked) }

                    fun handleCheckBoxPress(newValue: Boolean) {
                        checked = newValue
                        components.value[i].content = if (checked) "done|$label" else "undone|$label"
                        saveToSharedPreferences(generateMarkdown(components.value))
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { handleCheckBoxPress(!checked) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { handleCheckBoxPress(it) },
                            modifier = Modifier
                                .size(20.dp) // shrink the actual checkbox to remove built-in spacing
                                .padding(0.dp) // ensure no extra padding is applied
                        )
                        Spacer(Modifier.size(6.dp)) // small gap you control
                        Text(
                            text = label,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.size(16.dp))
                }


                else -> {
                    MarkdownText(
                        markdown = it.second,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEditing = true }
                    )
                    Spacer(Modifier.size(16.dp))
                }
            }
        }
    }

    Spacer(modifier = Modifier.size(1.dp).padding(WindowInsets.navigationBars.asPaddingValues()))
}