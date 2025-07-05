package neth.iecal.questphone.ui.screens.launcher

import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.coroutineScope
import neth.iecal.questphone.R
import neth.iecal.questphone.data.game.StreakCheckReturn
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.checkIfStreakFailed
import neth.iecal.questphone.data.game.continueStreak
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.ui.navigation.Screen
import neth.iecal.questphone.ui.screens.launcher.components.LiveClock
import neth.iecal.questphone.ui.screens.quest.DialogState
import neth.iecal.questphone.ui.screens.quest.RewardDialogInfo
import neth.iecal.questphone.utils.QuestHelper
import neth.iecal.questphone.utils.VibrationHelper
import neth.iecal.questphone.utils.formatHour
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.getCurrentDay
import neth.iecal.questphone.utils.isSetToDefaultLauncher
import neth.iecal.questphone.utils.openDefaultLauncherSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current

    val dao = QuestDatabaseProvider.getInstance(context).questDao()

    val questHelper = QuestHelper(context)
    val questListUnfiltered by dao.getAllQuests().collectAsState(initial = emptyList())
    val initial = remember { mutableStateOf(true) }
    val questList = remember { mutableStateListOf<CommonQuestInfo>() }

    val completedQuests = remember { SnapshotStateList<String>() }
    val progress = (completedQuests.size.toFloat() / questList.size.toFloat()).coerceIn(0f,1f)


    BackHandler {  }


    fun streakFailResultHandler(streakCheckReturn: StreakCheckReturn?){
        if(streakCheckReturn!=null){
            RewardDialogInfo.streakData = streakCheckReturn
            if(streakCheckReturn.streakFreezersUsed!=null){
                RewardDialogInfo.currentDialog = DialogState.STREAK_UP
            }
            if(streakCheckReturn.streakDaysLost!=null){
                RewardDialogInfo.currentDialog = DialogState.STREAK_FAILED
            }
            RewardDialogInfo.isRewardDialogVisible = true

        }
    }

    LaunchedEffect(questListUnfiltered) {
        if (initial.value) {
            initial.value = false // Ignore the first emission (initial = emptyList())
        } else {
            val todayDay = getCurrentDay()
            val isUserCreatedToday = getCurrentDate() == User.userInfo.getCreatedOnString()

            Log.d("IsUserCreatedToday",isUserCreatedToday.toString())
            val list = questListUnfiltered.filter {
                !it.is_destroyed && it.selected_days.contains(todayDay) &&
                        (isUserCreatedToday || it.created_on != getCurrentDate())
            }
            questList.clear()
            questList.addAll(list)


            questList.forEach { item ->
                if (item.last_completed_on == getCurrentDate()) {
                    completedQuests.add(item.title)
                }
                if (questHelper.isQuestRunning(item.title)) {
                    viewQuest(item, navController)
                }
            }

            val data = context.getSharedPreferences("onboard", MODE_PRIVATE)


            if (User.userInfo.streak.currentStreak != 0) {
                streakFailResultHandler(User.checkIfStreakFailed())
            }
            if (completedQuests.size == questList.size && data.getBoolean("onboard", false)) {
                if (User.continueStreak()) {
                    RewardDialogInfo.currentDialog = DialogState.STREAK_UP
                    RewardDialogInfo.isRewardDialogVisible = true
                }
            }

        }
    }
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        // Wait for the first touch down event
                        awaitFirstDown()
                        var dragAmount = 0f

                        // Track vertical drag events
                        do {
                            val event = awaitPointerEvent()
                            val dragEvent = event.changes.first()
                            val dragChange = dragEvent.positionChange().y
                            dragAmount += dragChange

                            // If the swipe exceeds the threshold, trigger navigation
                            if (dragAmount < -5) { // Swipe-up threshold
                                navController.navigate(Screen.AppList.route)
                                VibrationHelper.vibrate(50)
                                break
                            }
                        } while (dragEvent.pressed)
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Coins display on the left
            Image(
                painter = painterResource(R.drawable.coin_icon),
                contentDescription = "Streak",
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp)
                    .clickable{
                        navController.navigate(Screen.Store.route)
                    }
            )
            Text(
                text = "${User.userInfo.coins}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.size(8.dp))

            Image(
                painter = painterResource(R.drawable.streak),
                contentDescription = "Streak",
                modifier = Modifier
                    .padding(8.dp)
                    .size(30.dp)
                    .clickable{
                        navController.navigate(Screen.Store.route)
                    }
            )
            Text(
                text = "${User.userInfo.streak.currentStreak}D",
                style = MaterialTheme.typography.bodyLarge,
            )


            Spacer(modifier = Modifier.weight(1f)) // Pushes the Icon to the right

            Icon(
                painter = painterResource(R.drawable.outline_store_24),
                contentDescription = "Shop",
                modifier = Modifier
                    .padding(8.dp)
                    .size(30.dp)
                    .clickable{
                    navController.navigate(Screen.Store.route)
                }
            )

            // Profile icon on the right
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "user info and stats",
                modifier = Modifier
                    .padding(8.dp)
                    .size(30.dp)
                    .clickable {
                        navController.navigate(Screen.UserInfo.route)
                    }
            )
        }


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ) {

            LiveClock(
                Modifier
                    .padding(bottom = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            // QUESTS header
//            Text(
//                text = "QUESTS",
//                style = MaterialTheme.typography.headlineLarge,
//                fontFamily = JetBrainsMonoFont,
//                fontWeight = FontWeight.Bold,
//                modifier = Modifier.padding(bottom = 16.dp)
//                    .align(Alignment.CenterHorizontally)
//            )



            if(questList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(50.dp)
                        .padding(bottom = 32.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                items(questList.size){ index ->
                    val baseQuest = questList[index]
                    val timeRange = "${formatHour(baseQuest.time_range[0])} - ${formatHour(baseQuest.time_range[1])} : "
                    val prefix = if(baseQuest.time_range[0]==0&&baseQuest.time_range[1]==24) "" else timeRange
                    val isOver = questHelper.isOver(baseQuest)
                    QuestItem(
                        text =  if(QuestHelper.Companion.isInTimeRange(baseQuest) && isOver) baseQuest.title else  prefix +  baseQuest.title,
                        isCompleted = completedQuests.contains(baseQuest.title),
                        isFailed = isOver,
                        modifier = Modifier.clickable {
                            viewQuest(baseQuest,navController)
                    })
                }
                item {
                    QuestItem(
                        text = "Manage Quests",
                        modifier = Modifier.clickable {
                            navController.navigate(Screen.ListAllQuest.route)
                        }
                    )
                }
                if(!isSetToDefaultLauncher(context)){
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openDefaultLauncherSettings(context) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_info_24),
                                    contentDescription = "Information",
                                    modifier = Modifier.size(30.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                Text(
                                    text = "Set QuestPhone as your default launcher for the best experience",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.weight(1f)
                                )

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward, // or chevron_right
                                    contentDescription = "Open settings",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                    }
                }
            }

        }
    }}
}

fun viewQuest(baseQuest: CommonQuestInfo, navController: NavController) {
    navController.navigate(Screen.ViewQuest.route + baseQuest.id)
}
@Composable
fun QuestItem(
    text: String,
    isCompleted: Boolean = false,
    isFailed: Boolean = false,
    modifier: Modifier
) {
    Text(
        text = text,
        style = if (isCompleted || isFailed) {
            MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough)
        } else {
            MaterialTheme.typography.bodyLarge
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        color = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
}
