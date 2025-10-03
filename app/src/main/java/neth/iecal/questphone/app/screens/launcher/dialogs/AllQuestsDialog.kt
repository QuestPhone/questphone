package neth.iecal.questphone.app.screens.launcher.dialogs

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import neth.iecal.questphone.R
import neth.iecal.questphone.app.navigation.RootRoute
import neth.iecal.questphone.backed.repositories.QuestRepository
import neth.iecal.questphone.core.utils.managers.QuestHelper
import neth.iecal.questphone.data.CommonQuestInfo
import nethical.questphone.core.core.utils.formatHour
import nethical.questphone.core.core.utils.getCurrentDate
import nethical.questphone.core.core.utils.getCurrentDay
import javax.inject.Inject

@HiltViewModel
class AllQuestDialogViewModel @Inject constructor(
    questRepository: QuestRepository,
    application: Application
) : AndroidViewModel(application){

    val completedQuests = SnapshotStateList<String>()

    private val allQuests = MutableStateFlow<List<CommonQuestInfo>>(emptyList())

    val questList = MutableStateFlow<List<CommonQuestInfo>>(mutableListOf())
    val isLoading = mutableStateOf(true)

    init {
        viewModelScope.launch {
            questRepository.getAllQuests().collectLatest { unfiltered ->
                val todayDay = getCurrentDay()
                val filtered = unfiltered.filter {
                    !it.is_destroyed && it.selected_days.contains(todayDay)
                }

                allQuests.value = filtered
                questList.value = filtered

                completedQuests.clear()
                completedQuests.addAll(
                    filtered.filter { it.last_completed_on == getCurrentDate() }
                        .map { it.id }
                )

                isLoading.value = false
            }
        }
    }

    fun showOnlyHardLocked() {
        questList.value = allQuests.value.filter { it.isHardLock }
    }

    fun showAll() {
        questList.value = allQuests.value
    }
}

@Composable
fun AllQuestsDialog(
    rootNavController: NavController?,
    viewModel: AllQuestDialogViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
    showOnlyHardLockedQuests : Boolean = false
    ) {
    val completedQuests = viewModel.completedQuests
    val questList by viewModel.questList.collectAsState()
    val isLoading = viewModel.isLoading


    val progress =
        (completedQuests.size.toFloat() / questList.size.toFloat()).coerceIn(0f, 1f)

    LaunchedEffect(showOnlyHardLockedQuests) {
        if (showOnlyHardLockedQuests) {
            viewModel.showOnlyHardLocked()
        } else {
            viewModel.showAll()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .wrapContentHeight(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 24.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header with icon and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                ) {
                    Icon(
                        painter = painterResource(neth.iecal.questphone.R.drawable.baseline_gamepad_24),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp),
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if(showOnlyHardLockedQuests) "Please perform all your Hard Locked quests first" else "Today's Quests",
                        style = if(showOnlyHardLockedQuests)MaterialTheme.typography.bodyMedium else MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!isLoading.value && questList.isNotEmpty()) {
                        Text(
                            text = "${completedQuests.size} of ${questList.size} completed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Content
            AnimatedContent(
                targetState = isLoading,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                },
                label = "loading_content"
            ) { loading ->
                if (loading.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Loading your quests...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column {
                        // Progress indicator
                        if (questList.isNotEmpty()) {

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Progress",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${(progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Quest list
                        if (questList.isEmpty()) {
                            EmptyQuestState()
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 400.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(
                                    items = questList,
                                    key = { it.id }
                                ) { baseQuest ->
                                    val timeRange =
                                        "${formatHour(baseQuest.time_range[0])} - ${
                                            formatHour(
                                                baseQuest.time_range[1]
                                            )
                                        } : "
                                    val prefix =
                                        if (baseQuest.time_range[0] == 0 && baseQuest.time_range[1] == 24) ""
                                        else timeRange
                                    val isFailed = QuestHelper.isTimeOver(baseQuest)
                                    val isCompleted = completedQuests.contains(baseQuest.id)

                                    Row (verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (QuestHelper.Companion.isInTimeRange(baseQuest) && isFailed) baseQuest.title else prefix + baseQuest.title,
                                            fontWeight = FontWeight.ExtraLight,
                                            fontSize = 23.sp,
                                            color = if (isFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                            textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                            modifier = Modifier.clickable(
                                                onClick = {
                                                    onDismiss()
                                                    rootNavController?.navigate(RootRoute.ViewQuest.route + baseQuest.id)
                                                },
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = ripple(bounded = false),
                                            ).weight(1f)
                                        )
                                        if (baseQuest.isHardLock) {
                                            Spacer(Modifier.size(4.dp))
                                            Icon(
                                                painter = painterResource(R.drawable.outline_lock_24),
                                                contentDescription = "Locked Quest"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
@Composable
private fun EmptyQuestState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier
                    .padding(20.dp)
                    .size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "No quests for today",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "You're all caught up! Check back tomorrow.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}