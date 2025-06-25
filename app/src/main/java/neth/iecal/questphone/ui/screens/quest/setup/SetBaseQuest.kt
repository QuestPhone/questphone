package neth.iecal.questphone.ui.screens.quest.setup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.QuestInfoState
import neth.iecal.questphone.ui.screens.quest.setup.components.AutoDestruct
import neth.iecal.questphone.ui.screens.quest.setup.components.SelectDaysOfWeek
import neth.iecal.questphone.ui.screens.quest.setup.components.SetTimeRange
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.getCurrentDay

@Composable
fun SetBaseQuest(questInfoState: QuestInfoState, isTimeRangeSupported: Boolean = true) {

    val allQuestTitles = mutableSetOf<String>()

    var isTitleDuplicate by remember { mutableStateOf(false) }
    val dao = QuestDatabaseProvider.getInstance(LocalContext.current).questDao()

    LaunchedEffect(Unit) {
        allQuestTitles.addAll(
            dao.getAllQuests().first().map { it.title }
        )

    }

    OutlinedTextField(
        value = questInfoState.title,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        onValueChange = {
            isTitleDuplicate = allQuestTitles.contains(it)
            questInfoState.title = it
                        },
        label = { Text("Quest Title") },
        modifier = Modifier.fillMaxWidth(),
        isError = isTitleDuplicate
    )
    if(isTitleDuplicate){
        Text(text = "Title already exists", color = MaterialTheme.colorScheme.error)
    }

    if(questInfoState.selectedDays.contains(getCurrentDay()) && User.userInfo.getCreatedOnString() != getCurrentDate()){
        Text("To prevent abuse, you can't perform this quest today. You'll be able to do it starting from the next time it occurs.")
    }

//    OutlinedTextField(
//        value = questInfoState.reward.toString(),
//        singleLine = true,
//        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
//        onValueChange = {
//            val count = it.toIntOrNull() ?: 0
//            if(count<5){
//                questInfoState.reward = count
//            }
//        },
//        label = { Text("Rewarded Coins") },
//        modifier = Modifier.fillMaxWidth(),
//        isError = isTitleDuplicate
//    )
//
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
