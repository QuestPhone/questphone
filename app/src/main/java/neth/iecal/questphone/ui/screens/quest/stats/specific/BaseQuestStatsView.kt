package neth.iecal.questphone.ui.screens.quest.stats.specific

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toLocalDateTime
import neth.iecal.questphone.R
import neth.iecal.questphone.data.game.InventoryItem
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.getInventoryItemCount
import neth.iecal.questphone.data.game.useInventoryItem
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.stats.StatsDatabaseProvider
import neth.iecal.questphone.data.quest.stats.StatsInfo
import neth.iecal.questphone.utils.daysSince
import neth.iecal.questphone.utils.formatHour
import neth.iecal.questphone.utils.getStartOfWeek
import neth.iecal.questphone.utils.json
import neth.iecal.questphone.utils.toJavaDayOfWeek
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun BaseQuestStatsView(id: String, navController: NavHostController) {
    val context = LocalContext.current
    var successfulDates = remember { mutableStateListOf<kotlinx.datetime.LocalDate>() }

    /*
    Total Amount of quests(including failed and successful) that could be performed since the creation of the quest
     */
    var totalPerformableQuests by remember { mutableIntStateOf(0) }
    var totalSuccessfulQuests by remember { mutableIntStateOf(0) }
    var totalFailedQuests by remember { mutableIntStateOf(0) }
    var currentStreak by remember { mutableIntStateOf(0) }
    var longestStreak by remember { mutableIntStateOf(0) }
    var failureRate by remember { mutableFloatStateOf(0f) }
    var successRate by remember { mutableFloatStateOf(0f) }
    var totalCoins by remember { mutableIntStateOf(0) }
    var weeklyAverageCompletions by remember { mutableDoubleStateOf(0.0) }

    var baseData by remember { mutableStateOf<CommonQuestInfo>(CommonQuestInfo()) }
    LaunchedEffect(Unit) {
        val bdao = QuestDatabaseProvider.getInstance(context).questDao()
        baseData = bdao.getQuestById(id)!!

        val dao = StatsDatabaseProvider.getInstance(context).statsDao()

        var stats = dao.getStatsByQuestId(baseData.id).first()

        successfulDates.addAll(stats.map { it.date })
        val allowedDays = baseData.selected_days.map { it.toJavaDayOfWeek() }.toSet()
        totalPerformableQuests = daysSince(baseData.created_on, allowedDays)
        totalSuccessfulQuests = stats.size
        totalFailedQuests = totalPerformableQuests - totalFailedQuests
        currentStreak = calculateCurrentStreak(stats,allowedDays)
        longestStreak = calculateLongestStreak(stats,allowedDays)
        failureRate = if (totalPerformableQuests > 0) (totalFailedQuests.toFloat() / totalPerformableQuests) * 100 else 0f
        successRate = if (totalPerformableQuests > 0) (totalSuccessfulQuests.toFloat() / totalPerformableQuests) * 100 else 0f
        totalCoins = totalSuccessfulQuests * baseData.reward
        weeklyAverageCompletions = weeklyAverage(stats)
    }


    var showCalendarDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val currentYearMonth = remember { mutableStateOf(YearMonth.now()) }

    val scrollState = rememberScrollState()

    val isQuestEditorInfoDialogVisible = remember { mutableStateOf(false) }
    val isQuestDeleterInfoDialogVisible = remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with quest name and quick stats
            QuestHeader(baseData, currentStreak)

            // Progress overview cards
            ProgressStatsSection(
                successRate = successRate,
                longestStreak = longestStreak,
                weeklyAverage = weeklyAverageCompletions,
                totalCoins = totalCoins,
                totalSuccessful = totalSuccessfulQuests,
                totalPerformable = totalPerformableQuests
            )

            // Calendar preview showing last month's completions
            CalendarSection(
                questStats = successfulDates.toSet(),
                onShowFullCalendar = { showCalendarDialog = true }
            )

            // Quest Details
            QuestDetailsCard(baseData,isQuestEditorInfoDialogVisible,isQuestDeleterInfoDialogVisible)

            UseItemDialog(InventoryItem.QUEST_EDITOR,isQuestEditorInfoDialogVisible){
                navController.navigate(baseData.integration_id.name + "/${baseData.title}")
            }

            UseItemDialog(InventoryItem.QUEST_DELETER,isQuestDeleterInfoDialogVisible){
                val dao = QuestDatabaseProvider.getInstance(context).questDao()
                coroutineScope.launch {
                    val quest = dao.getQuest(baseData.title)
                    quest!!.is_destroyed = true
                    dao.upsertQuest(quest)
                }
                navController.popBackStack()
            }

        }

        // Calendar Dialog
        if (showCalendarDialog) {
            CalendarDialog(
                successfulDates = successfulDates.toSet(),
                currentYearMonth = currentYearMonth,
                onDismiss = { showCalendarDialog = false }
            )
        }


    }
}

@Composable
fun QuestHeader(baseData: CommonQuestInfo, currentStreak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = baseData.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side - Time window
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${formatHour(baseData.time_range[0])} - ${formatHour(baseData.time_range[1])}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                // Right side - Current streak with fire icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_local_fire_department_24
                        ),
                        contentDescription = "Current Streak",
                        tint = if (currentStreak > 0) Color(0xFFF57C00) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (currentStreak > 0) "$currentStreak days" else "No active streak",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (currentStreak > 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (currentStreak > 0)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun ProgressStatsSection(
    successRate: Float,
    totalSuccessful: Int,
    totalPerformable: Int,
    longestStreak: Int,
    weeklyAverage: Double,
    totalCoins: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Progress Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Circular progress indicator for success rate
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                val animatedSuccessRate by animateFloatAsState(
                    targetValue = successRate / 100f,
                    label = ""
                )

                CircularProgressIndicator(
                    progress = { animatedSuccessRate },
                    modifier = Modifier.size(150.dp),
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = when {
                        successRate >= 80f -> Color(0xFF4CAF50) // Green
                        successRate >= 60f -> Color(0xFF8BC34A) // Light Green
                        successRate >= 40f -> Color(0xFFFFC107) // Amber
                        successRate >= 20f -> Color(0xFFFF9800) // Orange
                        else -> Color(0xFFF44336) // Red
                    }
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${successRate.toInt()}%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Success Rate",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Key stats in a grid
            val items = listOf(
                Triple(
                    "Longest Streak",
                    "$longestStreak days",
                    R.drawable.baseline_local_fire_department_24
                ),
                Triple(
                    "Weekly Average",
                    "${String.format("%.1f", weeklyAverage)}",
                    R.drawable.baseline_view_week_24
                ),
                Triple("Total Earned", "$totalCoins coins", R.drawable.baseline_circle_24),
                Triple(
                    "Days Active",
                    "${totalSuccessful}/${totalPerformable}",
                    R.drawable.baseline_calendar_month_24
                )
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                items.chunked(2).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowItems.forEach { (title, value, icon) ->
                            StatCard(title, value, icon, Modifier.weight(1f))
                        }

                        // Fill empty space if odd number of items
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(40.dp)
                    .padding(bottom = 4.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
@Composable
fun CalendarSection(
    questStats: Set<kotlinx.datetime.LocalDate>,
    onShowFullCalendar: () -> Unit
) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    val pastWeekDates = (0..6).map { offset ->
        today.minus(offset, DateTimeUnit.DAY)
    }.reversed()

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                TextButton(onClick = onShowFullCalendar) {
                    Text("View Full Calendar")
                }
            }

            // Last 7 days calendar preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                pastWeekDates.forEach { date ->
                    val isCompleted = date in questStats
                    val dayInitial = date.dayOfWeek.name.first().toString() // First letter: M, T, W, etc.
                    val isToday = date == today

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Day of week (1st letter)
                        Text(
                            text = dayInitial,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Date circle
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCompleted -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = if (isToday) 2.dp else 1.dp,
                                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    isCompleted -> MaterialTheme.colorScheme.onPrimary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun QuestDetailsCard(baseData: CommonQuestInfo, isQuestEditorInfoDialogVisible: MutableState<Boolean>, isQuestDeleterInfoDialogVisible: MutableState<Boolean>) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            if(!baseData.is_destroyed){
                QuestInfoRow(label = "Expires", value = baseData.auto_destruct)
            }else{
                QuestInfoRow(label = "Current Status", value = "Destroyed")
            }
            QuestInfoRow(label = "Days Active", value = baseData.selected_days.toString())
            QuestInfoRow(
                label = "Time Range",
                value = "${formatHour(baseData.time_range[0])} - ${formatHour(baseData.time_range[1])}"
            )
            QuestInfoRow(label = "Created", value = baseData.created_on)

            QuestInfoRow(label = "Integration", value = baseData.integration_id.name)
            QuestInfoRow(
                label = "Reward",
                value = "${baseData.reward} coins",
                highlight = true
            )

            Spacer(Modifier.size(12.dp))

            if(!baseData.is_destroyed) {
                OutlinedButton(onClick = {
                    isQuestEditorInfoDialogVisible.value = true

                }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.quest_editor),
                            contentDescription = "quest_editor",
                            modifier = Modifier
                                .size(25.dp)
                                .clickable {
                                }
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = "Edit Quest",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.size(4.dp))

                OutlinedButton(onClick = {
                    isQuestDeleterInfoDialogVisible.value = true

                }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.quest_deletor),
                            contentDescription = "quest_editor",
                            modifier = Modifier
                                .size(25.dp)
                                .clickable {
                                }
                        )
                        Text(
                            text = "Delete Quest",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

            }


            Spacer(Modifier.size(4.dp))

            OutlinedButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(baseData.title, json.encodeToString(baseData))
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied to Clipboard", Toast.LENGTH_SHORT).show()
            }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.baseline_extension_24),
                        contentDescription = "export metadata",
                        modifier = Modifier
                            .size(25.dp)
                    )
                    Text(
                        text = "Copy Quest Json",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }


        }
    }
}

@Composable
private fun QuestInfoRow(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}


@Composable
fun CalendarDialog(
    successfulDates: Set<kotlinx.datetime.LocalDate>,
    currentYearMonth: MutableState<YearMonth>,
    onDismiss: () -> Unit
) {

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with month navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        currentYearMonth.value = currentYearMonth.value.minusMonths(1)
                    }) {
                        Text("<", style = MaterialTheme.typography.titleMedium)
                    }

                    Text(
                        text = currentYearMonth.value.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) +
                                " " + currentYearMonth.value.year,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(onClick = {
                        currentYearMonth.value = currentYearMonth.value.plusMonths(1)
                    }) {
                        Text(">", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // Weekday headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (day in DayOfWeek.values()) {
                        Text(
                            text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Calendar grid
                val firstOfMonth = currentYearMonth.value.atDay(1)
                val daysInMonth = currentYearMonth.value.lengthOfMonth()
                val firstDayOfWeekValue = firstOfMonth.dayOfWeek.value % 7
                val today = LocalDate.now()
                val rows = ((firstDayOfWeekValue + daysInMonth + 6) / 7)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (col in 0 until 7) {
                                val dayIndex = row * 7 + col - firstDayOfWeekValue + 1

                                if (dayIndex in 1..daysInMonth) {
                                    val javaDate = currentYearMonth.value.atDay(dayIndex)
                                    val date = javaDate.toKotlinLocalDate()
                                    val isSuccessful = date in successfulDates
                                    val isToday = javaDate == today

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSuccessful -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                    isToday -> MaterialTheme.colorScheme.surfaceVariant
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = if (isToday) 2.dp else 0.dp,
                                                color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayIndex.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = when {
                                                isSuccessful -> MaterialTheme.colorScheme.onPrimary
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                    }
                                } else {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = MaterialTheme.colorScheme.primary, text = "Completed")
                    if (today.monthValue == currentYearMonth.value.monthValue &&
                        today.year == currentYearMonth.value.year) {
                        LegendItem(borderColor = MaterialTheme.colorScheme.primary, text = "Today")
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}



@Composable
fun LegendItem(color: Color? = null, borderColor: Color? = null, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .then(
                    if (color != null)
                        Modifier.background(color)
                    else if (borderColor != null)
                        Modifier.border(1.dp, borderColor, CircleShape)
                    else
                        Modifier
                )
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun UseItemDialog(item: InventoryItem, isDialogVisible: MutableState<Boolean>, onUse: ()-> Unit = {}){
    if(isDialogVisible.value){
        Dialog(onDismissRequest = {
            isDialogVisible.value = false
        }) {
            val doesUserOwnEditor = User.getInventoryItemCount(item)>0
            Surface(
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .padding(24.dp)
                    .wrapContentSize()
            ) {

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(item.icon),
                        contentDescription = InventoryItem.QUEST_EDITOR.simpleName,
                        modifier = Modifier.size(60.dp)
                    )
                    if (doesUserOwnEditor) {
                        Text("Do You Want to Spend 1 ${item.simpleName} to perform this action?")
                    } else {
                        Text("You currently have no ${item.simpleName}. Please buy one from the shop to edit this quest")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isDialogVisible.value = false }) {
                            Text("Close")
                        }

                        if(doesUserOwnEditor){
                            Button(onClick = {
                                User.useInventoryItem(item)
                                onUse()
                                isDialogVisible.value = false
                            }) {
                                Text("Use")
                            }
                        }
                    }

                }
            }
        }
    }
}

fun calculateCurrentStreak(
    completed: Collection<StatsInfo>,
    questDays: Set<DayOfWeek>
): Int {
    val completedDates: HashSet<kotlinx.datetime.LocalDate> = completed.map { it.date }.toHashSet()

    var streak = 0
    var currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    while (true) {
        if (currentDate.dayOfWeek !in questDays) {
            currentDate = currentDate.minus(1, DateTimeUnit.DAY)
            continue
        }

        if (completedDates.contains(currentDate)) {
            streak++
            currentDate = currentDate.minus(1, DateTimeUnit.DAY)
        } else {
            break
        }
    }

    return streak
}



fun calculateLongestStreak(
    successStats: List<StatsInfo>,
    allowedDays: Set<DayOfWeek>
): Int {
    if (successStats.isEmpty()) return 0

    val completedDates = successStats.map { it.date }.toSet()

    val startDate = completedDates.min()
    val endDate = completedDates.max()

    var currentDate = startDate
    var currentStreak = 0
    var longestStreak = 0

    while (currentDate <= endDate) {
        if (currentDate.dayOfWeek in allowedDays) {
            if (currentDate in completedDates) {
                currentStreak++
                longestStreak = maxOf(longestStreak, currentStreak)
            } else {
                currentStreak = 0
            }
        }
        while (currentDate <= endDate) {
            currentDate = currentDate.plus(1, DateTimeUnit.DAY)
        }
    }

    return longestStreak
}

fun weeklyAverage(stats: List<StatsInfo>): Double {
    if (stats.isEmpty()) return 0.0

    // Group by the start of the ISO week
    val groupedByWeek = stats.groupBy { it.date.getStartOfWeek() }

    val totalWeeks = groupedByWeek.size
    val totalCompletions = stats.size

    return totalCompletions.toDouble() / totalWeeks
}
