package neth.iecal.questphone.app.screens.quest.stats.specific

// BaseQuestStatsViewVM.kt
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toLocalDateTime
import neth.iecal.questphone.R
import neth.iecal.questphone.backed.repositories.QuestRepository
import neth.iecal.questphone.backed.repositories.StatsRepository
import neth.iecal.questphone.backed.repositories.UserRepository
import neth.iecal.questphone.data.CommonQuestInfo
import neth.iecal.questphone.data.StatsInfo
import nethical.questphone.core.core.utils.daysSince
import nethical.questphone.core.core.utils.formatHour
import nethical.questphone.core.core.utils.getStartOfWeek
import nethical.questphone.core.core.utils.toJavaDayOfWeek
import nethical.questphone.data.game.InventoryItem
import nethical.questphone.data.json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.time.ExperimentalTime

@HiltViewModel
class BaseQuestStatsViewVM @Inject constructor(
    private val userRepository: UserRepository,
    private val questRepository: QuestRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {

    // Loading state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Quest data
    private val _baseData = MutableStateFlow(CommonQuestInfo())
    val baseData: StateFlow<CommonQuestInfo> = _baseData.asStateFlow()

    private val _successfulDates = MutableStateFlow<List<kotlinx.datetime.LocalDate>>(emptyList())
    val successfulDates: StateFlow<List<kotlinx.datetime.LocalDate>> = _successfulDates.asStateFlow()

    // Quest statistics
    private val _totalPerformableQuests = MutableStateFlow(0)
    val totalPerformableQuests: StateFlow<Int> = _totalPerformableQuests.asStateFlow()

    private val _totalSuccessfulQuests = MutableStateFlow(0)
    val totalSuccessfulQuests: StateFlow<Int> = _totalSuccessfulQuests.asStateFlow()

    private val _totalFailedQuests = MutableStateFlow(0)
    val totalFailedQuests: StateFlow<Int> = _totalFailedQuests.asStateFlow()

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _longestStreak = MutableStateFlow(0)
    val longestStreak: StateFlow<Int> = _longestStreak.asStateFlow()

    private val _failureRate = MutableStateFlow(0f)
    val failureRate: StateFlow<Float> = _failureRate.asStateFlow()

    private val _successRate = MutableStateFlow(0f)
    val successRate: StateFlow<Float> = _successRate.asStateFlow()

    private val _totalCoins = MutableStateFlow(0)
    val totalCoins: StateFlow<Int> = _totalCoins.asStateFlow()

    private val _weeklyAverageCompletions = MutableStateFlow(0.0)
    val weeklyAverageCompletions: StateFlow<Double> = _weeklyAverageCompletions.asStateFlow()

    // Dialog states
    private val _showCalendarDialog = MutableStateFlow(false)
    val showCalendarDialog: StateFlow<Boolean> = _showCalendarDialog.asStateFlow()

    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    private val _currentYearMonth = MutableStateFlow(YearMonth.now())
    val currentYearMonth: StateFlow<YearMonth> = _currentYearMonth.asStateFlow()

    private val _isQuestEditorInfoDialogVisible = MutableStateFlow(false)
    val isQuestEditorInfoDialogVisible: StateFlow<Boolean> = _isQuestEditorInfoDialogVisible.asStateFlow()

    private val _isQuestDeleterInfoDialogVisible = MutableStateFlow(false)
    val isQuestDeleterInfoDialogVisible: StateFlow<Boolean> = _isQuestDeleterInfoDialogVisible.asStateFlow()

    fun loadQuestStats(questId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val baseData = questRepository.getQuestById(questId) ?: return@launch
                _baseData.value = baseData

                val stats = statsRepository.getStatsByQuestId(baseData.id).first()

                val successfulDates = stats.map { it.date }
                _successfulDates.value = successfulDates

                val allowedDays = baseData.selected_days.map { it.toJavaDayOfWeek() }.toSet()
                val totalPerformableQuests = daysSince(baseData.created_on, allowedDays)
                val totalSuccessfulQuests = stats.size
                val totalFailedQuests = totalPerformableQuests - totalSuccessfulQuests
                val currentStreak = calculateCurrentStreak(stats, allowedDays)
                val longestStreak = calculateLongestStreak(stats, allowedDays)
                val failureRate = if (totalPerformableQuests > 0)
                    (totalFailedQuests.toFloat() / totalPerformableQuests) * 100 else 0f
                val successRate = if (totalPerformableQuests > 0)
                    (totalSuccessfulQuests.toFloat() / totalPerformableQuests) * 100 else 0f
                val totalCoins = totalSuccessfulQuests * baseData.reward
                val weeklyAverageCompletions = weeklyAverage(stats)

                _totalPerformableQuests.value = totalPerformableQuests
                _totalSuccessfulQuests.value = totalSuccessfulQuests
                _totalFailedQuests.value = totalFailedQuests
                _currentStreak.value = currentStreak
                _longestStreak.value = longestStreak
                _failureRate.value = failureRate
                _successRate.value = successRate
                _totalCoins.value = totalCoins
                _weeklyAverageCompletions.value = weeklyAverageCompletions

            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun showCalendarDialog() {
        _showCalendarDialog.value = true
    }

    fun hideCalendarDialog() {
        _showCalendarDialog.value = false
    }

    fun updateCurrentYearMonth(yearMonth: YearMonth) {
        _currentYearMonth.value = yearMonth
    }

    fun updateSelectedDate(date: LocalDate?) {
        _selectedDate.value = date
    }

    fun showQuestEditorDialog() {
        _isQuestEditorInfoDialogVisible.value = true
    }

    fun hideQuestEditorDialog() {
        _isQuestEditorInfoDialogVisible.value = false
    }

    fun showQuestDeleterDialog() {
        _isQuestDeleterInfoDialogVisible.value = true
    }

    fun hideQuestDeleterDialog() {
        _isQuestDeleterInfoDialogVisible.value = false
    }

    fun deductFromInventory(item: InventoryItem){
        userRepository.deductFromInventory(item)
    }
    fun doesUserHaveItem(item: InventoryItem): Boolean {
        return userRepository.getInventoryItemCount(item)>0
    }

    fun deleteQuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val quest = questRepository.getQuest(_baseData.value.title)
                quest?.let {
                    it.is_destroyed = true
                    questRepository.upsertQuest(it)
                    onSuccess()
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

// Updated Composable
@OptIn(ExperimentalTime::class)
@Composable
fun BaseQuestStatsView(
    id: String,
    navController: NavHostController,
    viewModel: BaseQuestStatsViewVM = hiltViewModel()
) {
    val scrollState = rememberScrollState()

    // Collect states
    val isLoading by viewModel.isLoading.collectAsState()
    val baseData by viewModel.baseData.collectAsState()
    val successfulDates by viewModel.successfulDates.collectAsState()
    val totalPerformableQuests by viewModel.totalPerformableQuests.collectAsState()
    val totalSuccessfulQuests by viewModel.totalSuccessfulQuests.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val longestStreak by viewModel.longestStreak.collectAsState()
    val successRate by viewModel.successRate.collectAsState()
    val weeklyAverageCompletions by viewModel.weeklyAverageCompletions.collectAsState()
    val totalCoins by viewModel.totalCoins.collectAsState()
    val showCalendarDialog by viewModel.showCalendarDialog.collectAsState()
    val currentYearMonth by viewModel.currentYearMonth.collectAsState()
    val isQuestEditorInfoDialogVisible by viewModel.isQuestEditorInfoDialogVisible.collectAsState()
    val isQuestDeleterInfoDialogVisible by viewModel.isQuestDeleterInfoDialogVisible.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadQuestStats(id)
    }

    if (isLoading) {
        // Show loading state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold() { padding ->
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
                onShowFullCalendar = { viewModel.showCalendarDialog() }
            )

            // Quest Details
            QuestDetailsCard(
                baseData,
                { viewModel.showQuestEditorDialog() },
                { viewModel.showQuestDeleterDialog() }
            )

            if (isQuestEditorInfoDialogVisible) {
                UseItemDialog(
                    InventoryItem.QUEST_EDITOR,
                    viewModel.doesUserHaveItem(InventoryItem.QUEST_EDITOR),
                    {
                        viewModel.hideQuestEditorDialog()
                    }
                ) {
                    viewModel.deductFromInventory(InventoryItem.QUEST_EDITOR)
                    navController.navigate(baseData.integration_id.name + "/${baseData.id}")
                }
            }

            if (isQuestDeleterInfoDialogVisible) {
                UseItemDialog(
                    InventoryItem.QUEST_DELETER,
                    viewModel.doesUserHaveItem(InventoryItem.QUEST_DELETER),
                    {
                        viewModel.hideQuestDeleterDialog()
                    }
                ) {
                    viewModel.deductFromInventory(InventoryItem.QUEST_DELETER)
                    viewModel.deleteQuest {
                        navController.popBackStack()
                    }
                }
            }
        }

        // Calendar Dialog
        if (showCalendarDialog) {
            CalendarDialog(
                successfulDates = successfulDates.toSet(),
                currentYearMonth = remember { mutableStateOf(currentYearMonth) },
                onDismiss = { viewModel.hideCalendarDialog() }
            )
        }
    }
}
@Composable
fun QuestHeader(baseData: CommonQuestInfo, currentStreak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${formatHour(baseData.time_range[0])} - ${formatHour(baseData.time_range[1])}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                // Right side - Current streak with fire icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_local_fire_department_24
                        ),
                        contentDescription = "Current Streak",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (currentStreak > 0) "$currentStreak days" else "No active streak",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (currentStreak > 0) FontWeight.Bold else FontWeight.Normal,
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
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                val animatedSuccessRate by animateFloatAsState(
                    targetValue = successRate / 100f,
                    label = ""
                )

                CircularProgressIndicator(
                    progress = { animatedSuccessRate },
                    modifier = Modifier.size(200.dp),
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                items.chunked(2).forEach { rowItems ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
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
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
@ExperimentalTime
@Composable
fun CalendarSection(
    questStats: Set<kotlinx.datetime.LocalDate>,
    onShowFullCalendar: () -> Unit
) {
    val today = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    val pastWeekDates = (0..6).map { offset ->
        today.minus(offset, DateTimeUnit.DAY)
    }.reversed()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
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
                    Text(
                        "View Full Calendar",
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Date circle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
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
fun QuestDetailsCard(baseData: CommonQuestInfo, onQuestEditorClicked: () -> Unit, onQuestDeleterClicked: ()-> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    onQuestEditorClicked()

                }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(nethical.questphone.data.R.drawable.quest_editor),
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
                    onQuestDeleterClicked()

                }, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(nethical.questphone.data.R.drawable.quest_deletor),
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
            style = MaterialTheme.typography.titleSmall,
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
                    for (day in DayOfWeek.entries) {
                        Text(
                            text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
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
                                                    isSuccessful -> MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.8f
                                                    )

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
                                    Box(modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f))
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UseItemDialog(item: InventoryItem,doesUserOwnEditor:Boolean, onDismiss: ()-> Unit, onUse: ()-> Unit = {}){
    Dialog(onDismissRequest = onDismiss) {
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
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }

                    if(doesUserOwnEditor){
                        Button(onClick = {
                            onUse()
                            onDismiss()
                        }) {
                            Text("Use")
                        }
                    }
                }

            }
        }
    }
}

@OptIn(ExperimentalTime::class)
fun calculateCurrentStreak(
    completed: Collection<StatsInfo>,
    questDays: Set<DayOfWeek>
): Int {
    val completedDates: HashSet<kotlinx.datetime.LocalDate> = completed.map { it.date }.toHashSet()

    var streak = 0
    var currentDate = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    while (true) {
        val javaDay = DayOfWeek.valueOf(currentDate.dayOfWeek.name)
        if (javaDay !in questDays) {
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
        val javaDay = DayOfWeek.valueOf(currentDate.dayOfWeek.name)
        if (javaDay in allowedDays) {
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
