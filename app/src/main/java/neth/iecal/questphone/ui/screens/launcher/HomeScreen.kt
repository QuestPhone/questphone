package neth.iecal.questphone.ui.screens.launcher

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import neth.iecal.questphone.R
import neth.iecal.questphone.data.game.StreakCheckReturn
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.checkIfStreakFailed
import neth.iecal.questphone.data.game.continueStreak
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.ui.navigation.Screen
import neth.iecal.questphone.ui.screens.quest.DialogState
import neth.iecal.questphone.ui.screens.quest.RewardDialogInfo
import neth.iecal.questphone.utils.QuestHelper
import neth.iecal.questphone.utils.VibrationHelper
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.getCurrentDay
import neth.iecal.questphone.utils.isLockScreenServiceEnabled
import neth.iecal.questphone.utils.isSetToDefaultLauncher
import neth.iecal.questphone.utils.openDefaultLauncherSettings
import neth.iecal.questphone.utils.performLockScreenAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current

    val dao = QuestDatabaseProvider.getInstance(context).questDao()

    val questHelper = QuestHelper(context)
    val questListUnfiltered by dao.getAllQuests().collectAsState(initial = emptyList())

    var isFirstRender by remember { mutableStateOf(true) }
    val questList = remember { mutableStateListOf<CommonQuestInfo>() }
    var time by remember { mutableStateOf(getCurrentTime12Hr()) }

    val completedQuests = remember { SnapshotStateList<String>() }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isServiceEnabled = remember(context) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isLockScreenServiceEnabled(context)
    }

    LaunchedEffect(Unit) {
        while (true) {
            time = getCurrentTime12Hr()
            val delayMillis = 60_000 - (System.currentTimeMillis() % 60_000) // Delay until next minute
            delay(delayMillis)
        }
    }

    fun streakFailResultHandler(streakCheckReturn: StreakCheckReturn?) {
        if (streakCheckReturn != null) {
            RewardDialogInfo.streakData = streakCheckReturn
            if (streakCheckReturn.streakFreezersUsed != null) {
                RewardDialogInfo.currentDialog = DialogState.STREAK_UP
            }
            if (streakCheckReturn.streakDaysLost != null) {
                RewardDialogInfo.currentDialog = DialogState.STREAK_FAILED
            }
            RewardDialogInfo.isRewardDialogVisible = true

        }
    }

    LaunchedEffect(questListUnfiltered) {
        if (isFirstRender) {
            isFirstRender = false // Ignore the first emission (initial = emptyList())
        } else {
            val todayDay = getCurrentDay()
            val isUserCreatedToday = getCurrentDate() == User.userInfo.getCreatedOnString()

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
//                    viewQuest(item, navController)
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
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .pointerInput(Unit) {
                coroutineScope {
                    launch {
                        var verticalDragOffset = 0f
                        detectDragGestures(
                            onDragStart = {
                                verticalDragOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                verticalDragOffset += dragAmount.y
                            },
                            onDragEnd = {
                                // less value implies less swipe required
                                val swipeThreshold = -100

                                if (verticalDragOffset < swipeThreshold) {
                                    navController.navigate(Screen.AppList.route)
                                    VibrationHelper.vibrate(50)
                                }
                            }
                        )
                    }

                    launch {
                        detectTapGestures(
                            onDoubleTap = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isServiceEnabled) {
                                    performLockScreenAction()
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Enable Accessibility Service to use double-tap to sleep.",
                                            actionLabel = "Open",
                                            duration = SnackbarDuration.Short
                                        ).also { result ->
                                            if (result == SnackbarResult.ActionPerformed) {
                                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            }
                                        }
                                    }
                                }
                            }
                        )
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


    Scaffold(modifier = Modifier.safeDrawingPadding(), topBar = {
        TopAppBar(
            title = {},
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = Color(0xFF2A2A2A),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.coin_icon),
                        contentDescription = "Coins",
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "32",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            })
    }) { innerPadding ->
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
            // Quests
            Column(
                Modifier.padding(8.dp)
            ) {
                Text(
                    time,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Today's Quests",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(questList.size) { index ->
                        val baseQuest = questList[index]
                        val isFailed = questHelper.isOver(baseQuest)

                        val isCompleted = completedQuests.contains(baseQuest.title)

                        Text(
                            text = baseQuest.title,
                            fontWeight = FontWeight.ExtraLight,
                            fontSize = 23.sp,
                            color = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier.clickable(onClick = {
                                navController.navigate(Screen.ViewQuest.route + baseQuest.id)
                            })
                        )
                    }
                    item {
                        Text(
                            text = "✦✦✦✦✦",
                            fontWeight = FontWeight.ExtraLight,
                            fontSize = 15.sp,
                        )
                    }
                }
            }

            // Bottom links
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                horizontalAlignment = Alignment.End
            ) {
                for(i in 1..4){
                    Text(
                        text = "Shortcut $i",
                        fontWeight = FontWeight.ExtraLight,
                        fontSize = 23.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
