package neth.iecal.questphone.ui.screens.launcher

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import neth.iecal.questphone.R
import neth.iecal.questphone.data.game.StreakCheckReturn
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.checkIfStreakFailed
import neth.iecal.questphone.data.game.continueStreak
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.ui.navigation.Screen
import neth.iecal.questphone.ui.screens.components.NeuralMeshAsymmetrical
import neth.iecal.questphone.ui.screens.components.TopBarActions
import neth.iecal.questphone.ui.screens.launcher.components.AllQuestsDialog
import neth.iecal.questphone.ui.screens.quest.DialogState
import neth.iecal.questphone.ui.screens.quest.RewardDialogInfo
import neth.iecal.questphone.ui.screens.quest.setup.deep_focus.SelectAppsDialog
import neth.iecal.questphone.utils.QuestHelper
import neth.iecal.questphone.utils.getCurrentDate
import neth.iecal.questphone.utils.getCurrentDay
import neth.iecal.questphone.utils.getCurrentTime12Hr
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

    var isAppSelectorVisible by remember { mutableStateOf(false) }
    var shortcuts = remember { mutableStateListOf<String>() }
    var tempShortcuts = remember { mutableStateListOf<String>() } //used by app selector dialog, to store the shortcuts temporarily before saving

    val sidePanelItems = listOf<SidePanelItem>(
        SidePanelItem(R.drawable.profile_d,{navController.navigate(Screen.UserInfo.route)},"Profile"),
        SidePanelItem(R.drawable.notification_up,{ Toast.makeText(context,"Coming soon!", Toast.LENGTH_SHORT).show()},"Notifications"),
        SidePanelItem(R.drawable.store,{navController.navigate(Screen.Store.route)},"Store"),
        SidePanelItem(R.drawable.quest_analytics,{navController.navigate(Screen.ListAllQuest.route)},"Quest Analytics"),
        SidePanelItem(R.drawable.quest_adderpng,{navController.navigate(Screen.SelectTemplates.route)},"Add Quest")
    )

    var isAllQuestsDialogVisible by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val swipeIconAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -10f, // move up 10 dp (negative is up in Compose)
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offsetY"
    )

    val hapticFeedback = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isServiceEnabled = remember(context) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isLockScreenServiceEnabled(context)
    }

    BackHandler { }

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

            var list = questListUnfiltered.filter {
                !it.is_destroyed && it.selected_days.contains(todayDay)
            }


            list.forEach { item ->
                if (item.last_completed_on == getCurrentDate()) {
                    completedQuests.add(item.id)
                }
                if (questHelper.isQuestRunning(item.title)) {
                    navController.navigate(item.integration_id.name + item.id)
                }
            }

            val data = context.getSharedPreferences("onboard", MODE_PRIVATE)
            if (User.userInfo.streak.currentStreak != 0) {
                streakFailResultHandler(User.checkIfStreakFailed())
            }
            if (completedQuests.size == list.size && data.getBoolean("onboard", false)) {
                if (User.continueStreak()) {
                    RewardDialogInfo.currentDialog = DialogState.STREAK_UP
                    RewardDialogInfo.isRewardDialogVisible = true
                }
            }

            // Separate uncompleted and completed quests
            val uncompleted = list.filter { it.id !in completedQuests }
            val completed = list.filter { it.id in completedQuests }

            // Merge and sort, prioritizing uncompleted first
            val merged = (uncompleted + completed).sortedBy { QuestHelper.isInTimeRange(it) }

            // Take up to 4 items from the merged list
            list = if (merged.size >= 4) merged.take(4) else merged

            questList.clear()
            questList.addAll(list)




        }
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar({}, actions = {
                TopBarActions( true, true)
            })

        },

        snackbarHost = { SnackbarHost(hostState = snackbarHostState)}) { innerPadding ->

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
        if(isAllQuestsDialogVisible){
            AllQuestsDialog(
                navController = navController
            ) {
                isAllQuestsDialogVisible = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    var verticalDragOffset = 0f
                    detectVerticalDragGestures(
                        onDragStart = {
                            // Reset offset when drag starts
                            verticalDragOffset = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            verticalDragOffset += dragAmount
                        },
                        onDragEnd = {
                            // Negative value for swipe-up, adjust threshold as needed
                            val swipeThreshold = -100f // Increased for more deliberate swipe
                            if (verticalDragOffset < swipeThreshold) {
                                navController.navigate(Screen.AppList.route)
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
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
        ) {
            Column(
                Modifier.padding(8.dp)
            ) {
                NeuralMeshAsymmetrical(modifier = Modifier.size(200.dp))
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

                if(questList.isEmpty()){
                    TextButton(onClick = {
                        navController.navigate(Screen.SelectTemplates.route)
                    }) {
                        Row {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Quests")
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = "Add Quests",
                                fontWeight = FontWeight.ExtraLight,
                                fontSize = 23.sp)
                        }
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    userScrollEnabled = false,

                    ) {
                    items(questList.size) { index ->
                        val baseQuest = questList[index]
                        val isFailed = questHelper.isOver(baseQuest)

                        val isCompleted = completedQuests.contains(baseQuest.id)
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
                            modifier = Modifier.clickable(onClick = {
                                isAllQuestsDialogVisible  = true
                            },
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false))
                        )

                        if(!isSetToDefaultLauncher(context)) {
                            Spacer(Modifier.size(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = {
                                openDefaultLauncherSettings(context)
                            })) {
                                Icon(
                                    painter = painterResource(R.drawable.baseline_info_24),
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = "Set QuestPhone as your default launcher for the best experience",
                                    fontSize = 15.sp,
                                )
                            }
                        }
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