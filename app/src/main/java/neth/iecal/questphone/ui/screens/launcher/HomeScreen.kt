package neth.iecal.questphone.ui.screens.launcher

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ripple
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
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
import neth.iecal.questphone.ui.screens.components.NeuralNetworkCanvas
import neth.iecal.questphone.ui.screens.components.TopBarActions
import neth.iecal.questphone.ui.screens.quest.DialogState
import neth.iecal.questphone.ui.screens.quest.RewardDialogInfo
import neth.iecal.questphone.ui.screens.quest.setup.deep_focus.SelectAppsDialog
import neth.iecal.questphone.utils.QuestHelper
import neth.iecal.questphone.utils.VibrationHelper
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.getCurrentDay
import neth.iecal.questphone.utils.isLockScreenServiceEnabled
import neth.iecal.questphone.utils.isSetToDefaultLauncher
import neth.iecal.questphone.utils.openDefaultLauncherSettings
import neth.iecal.questphone.utils.performLockScreenAction

data class SidePanelItem(
    val icon: Int,
    val onClick: () -> Unit,
    val contentDesc: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
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
        val shortcutsSp = context.getSharedPreferences("shortcuts", MODE_PRIVATE)
        val tshortcuts =  shortcutsSp.getStringSet("shortcuts", setOf())?.toList<String>() ?: listOf()
        shortcuts.addAll(tshortcuts)
        tempShortcuts.addAll(tshortcuts)
        while (true) {
            time = getCurrentTime12Hr()
            val delayMillis =
                60_000 - (System.currentTimeMillis() % 60_000) // Delay until next minute
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
                    navController.navigate(item.integration_id.name + item.id)
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

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar({}, actions = {
                TopBarActions( true, true)
            })

        }) { innerPadding ->

        if (isAppSelectorVisible) {
            SelectAppsDialog(
                tempShortcuts,
                onDismiss = { isAppSelectorVisible = false },
                onConfirm = {
                    val shortcutsSp = context.getSharedPreferences("shortcuts", MODE_PRIVATE)
                    shortcutsSp.edit(commit = true) {
                        putStringSet(
                            "shortcuts",
                            tempShortcuts.toSet()
                        )
                    }
                    shortcuts.clear()
                    shortcuts.addAll(tempShortcuts)
                    isAppSelectorVisible = false
                })
        }

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
            Column(
                Modifier.padding(8.dp)
            ) {
                Spacer(Modifier.size(12.dp))
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
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    userScrollEnabled = false,

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
                            },
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false)
                            )
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

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp,bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                LazyColumn(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF2A2A2A),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(15.dp),
                    userScrollEnabled = false,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {


                    items(sidePanelItems) {
                        Box(
                            modifier = Modifier
                                .size(35.dp)
                                .clickable(
                                    onClick = {it.onClick() },
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = ripple(bounded = false)

                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(it.icon),
                                contentDescription = it.contentDesc,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp,bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                if(shortcuts.isEmpty()){
                    TextButton(onClick = {
                        isAppSelectorVisible = true
                    }) {
                    Row {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Shortcuts")
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = "Add Shortcuts",
                            fontWeight = FontWeight.ExtraLight,
                            fontSize = 23.sp)
                    }
                    }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    itemsIndexed(shortcuts) { index, it ->
                        val name = try {
                            val appInfo = context.packageManager.getApplicationInfo(it, 0)
                            appInfo.loadLabel(context.packageManager).toString()
                        } catch (_: Exception) {
                            it
                        }

                        Text(
                            text = name,
                            fontWeight = FontWeight.ExtraLight,
                            fontSize = 23.sp,
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .wrapContentWidth()
                                .combinedClickable(onClick = {
                                    val intent =
                                        context.packageManager.getLaunchIntentForPackage(
                                            it
                                        )
                                    intent?.let { context.startActivity(it) }
                                }, onLongClick = {
                                    isAppSelectorVisible = true
                                })
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Swipe up",
                modifier = Modifier.align(Alignment.BottomCenter)
                    .offset(y = swipeIconAnimation.dp)
                    .padding(bottom = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()*2)
            )
        }
    }
}