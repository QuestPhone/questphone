package neth.iecal.questphone.app.screens.quest.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import neth.iecal.questphone.app.screens.quest.setup.components.AutoDestruct
import neth.iecal.questphone.app.screens.quest.setup.components.SelectDaysOfWeek
import neth.iecal.questphone.app.screens.quest.setup.components.SetTimeRange
import neth.iecal.questphone.data.QuestInfoState
import nethical.questphone.core.core.utils.formatHour
import nethical.questphone.core.core.utils.getCurrentDate
import nethical.questphone.core.core.utils.getCurrentDay

@Composable
fun CommonSetBaseQuest(createdOnDate:String,questInfoState: QuestInfoState, isTimeRangeSupported: Boolean = true, onAdvancedMdEditor:()->Unit = {}) {

    OutlinedTextField(
        value = questInfoState.title,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        onValueChange = {
            questInfoState.title = it
                        },
        label = { Text("Quest Title") },
        modifier = Modifier.fillMaxWidth(),
    )


    if(questInfoState.selectedDays.contains(getCurrentDay()) && createdOnDate != getCurrentDate()){
        Text("Fake a quest if you want. It'll sit in your history, reminding you you're a fraud. Real ones can ignore this, you’ve got nothing to hide.")
    }
    SelectDaysOfWeek(questInfoState)


    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = questInfoState.instructions,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            onValueChange = { questInfoState.instructions = it }, // Direct update
            label = { Text("Instructions") },
            modifier = Modifier.weight(1f)
                .height(200.dp)
        )
        Spacer(Modifier.size(8.dp))
        IconButton(
            modifier = Modifier.size(30.dp),
            onClick = {
                onAdvancedMdEditor()
        }) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Markdown Editor"
            )
        }
        Spacer(Modifier.size(4.dp))

    }

    AutoDestruct(questInfoState)

    if(isTimeRangeSupported){
        SetTimeRange(questInfoState)
    }


    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable {
                questInfoState.isHardLock = !questInfoState.isHardLock
            }
    ) {
        Text(
            text = "Turn on HardLock? distracting apps stay locked for the full time " +
                    "(${formatHour(questInfoState.initialTimeRange[0])} to " +
                    "${formatHour(questInfoState.initialTimeRange[1])}) until this quest is complete. " +
                    "Coins won’t unlock apps during this time.",
            modifier = Modifier.weight(1f) // ✅ allow wrapping inside row
        )

        Spacer(Modifier.width(8.dp))

        Switch(
            checked = questInfoState.isHardLock,
            onCheckedChange = {
                questInfoState.isHardLock = it
            }
        )
    }

}
