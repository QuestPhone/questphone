package neth.iecal.questphone.app.screens.quest.setup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import neth.iecal.questphone.data.QuestInfoState
import neth.iecal.questphone.app.screens.quest.setup.components.AutoDestruct
import neth.iecal.questphone.app.screens.quest.setup.components.SelectDaysOfWeek
import neth.iecal.questphone.app.screens.quest.setup.components.SetTimeRange
import nethical.questphone.core.core.utils.getCurrentDate
import nethical.questphone.core.core.utils.getCurrentDay

@Composable
fun CommonSetBaseQuest(createdOnDate:String,questInfoState: QuestInfoState, isTimeRangeSupported: Boolean = true) {

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

    OutlinedTextField(
        value = questInfoState.instructions,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        onValueChange = { questInfoState.instructions = it }, // Direct update
        label = { Text("Instructions") },
        modifier = Modifier.fillMaxWidth()
            .height(200.dp)
    )
    AutoDestruct(questInfoState)

    if(isTimeRangeSupported){
        SetTimeRange(questInfoState)
    }

}
