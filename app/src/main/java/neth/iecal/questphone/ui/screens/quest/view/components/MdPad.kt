package neth.iecal.questphone.ui.screens.quest.view.components

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import dev.jeziellago.compose.markdowntext.MarkdownText
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.core.core.utils.getCurrentDate

@Composable
fun MdPad(commonQuestInfo: CommonQuestInfo){

    val context = LocalContext.current
    var currentText by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val sp = context.getSharedPreferences("temp_instructions", Context.MODE_PRIVATE)
        val cachedDate = sp.getString("cached_date", "")
        if(cachedDate == getCurrentDate()){
            if(sp.contains(commonQuestInfo.id)){
                currentText = sp.getString(commonQuestInfo.id, commonQuestInfo.instructions).toString()
            } else {
                currentText = commonQuestInfo.instructions
            }
        } else {
            sp.edit(commit = true) {
                clear()
                putString("cached_date", getCurrentDate())
            }
            currentText = commonQuestInfo.instructions
        }
    }

    fun saveToSharedPreferences(text: String) {
        val sp = context.getSharedPreferences("temp_instructions", Context.MODE_PRIVATE)
        sp.edit(commit = true) {
            putString(commonQuestInfo.id, text)
            putString("cached_date", getCurrentDate())
        }
    }

    Box() {

        if (isEditing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = {
                        if (isEditing) {
                            saveToSharedPreferences(currentText)
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
                value = currentText,
                onValueChange = { currentText = it },
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
            MarkdownText(
                markdown = currentText,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier
                    .padding(top = 32.dp, bottom = 4.dp)
                    .clickable { isEditing = true }
            )
        }
    }
}