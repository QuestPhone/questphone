package neth.iecal.questphone.ui.screens.quest.stats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import nethical.questphone.backend.QuestDatabaseProvider

// --- Data class to hold daily quest information ---
data class DailyQuestInfo(
    val date: LocalDate,
    val quests: List<String>,
    val isPlaceholder: Boolean = false
)

// --- Constants for easy adjustments ---
private val CELL_SIZE = 12.dp
private val CELL_SPACING = 3.dp
private val MONTH_SPACING = 8.dp // Space between months in the grid
private val DAY_LABEL_WIDTH = 32.dp
private val MONTH_LABEL_HEIGHT = 20.dp

@Composable
fun HeatMapChart(
    questMap: Map<LocalDate, List<String>>, // Using kotlinx.datetime.LocalDate
    modifier: Modifier = Modifier
) {
    if (questMap.isEmpty()) {
        Text("No Data available.", modifier = modifier.padding(16.dp))
        return
    }

    val currentSystemTimeZone = remember { TimeZone.currentSystemDefault() }

    // Determine the date range and pad with empty days
    val dateRange = remember(questMap) {
        val today = Clock.System.todayIn(currentSystemTimeZone)

        // The start date is still calculated from the first data point or today.
        val minDate = questMap.keys.minOrNull() ?: today
        val daysFromMondayStart = minDate.dayOfWeek.value - DayOfWeek.MONDAY.value
        val startDate = minDate.minus(daysFromMondayStart, DateTimeUnit.DAY)

        // The end date is now forced to extend to the start of the next year.
        val nextYear = today.year + 1
        val endOfYearTarget = LocalDate(nextYear, Month.JANUARY, 1)

        // We still pad the final week to the following Sunday for a complete grid.
        val daysToSundayEnd = DayOfWeek.SUNDAY.value - endOfYearTarget.dayOfWeek.value
        val endDate = endOfYearTarget.plus(daysToSundayEnd, DateTimeUnit.DAY)

        startDate to endDate
    }

    val allDaysData = remember(dateRange, questMap) {
        val (startDate, endDate) = dateRange
        val days = mutableListOf<DailyQuestInfo>()
        var currentDate = startDate
        while (currentDate <= endDate) {
            val questsForDate = questMap[currentDate] ?: emptyList()
            days.add(
                DailyQuestInfo(date = currentDate, quests = questsForDate)
            )
            currentDate = currentDate.plus(1, DateTimeUnit.DAY)
        }
        days
    }

    // Group padded days by week (starting Monday)
    val weeksData = remember(allDaysData) {
        allDaysData.groupBy {
            val daysFromMonday = it.date.dayOfWeek.value - DayOfWeek.MONDAY.value
            it.date.minus(daysFromMonday, DateTimeUnit.DAY)
        }
            .toSortedMap() // Ensure weeks are ordered
    }

    // Group weeks by the month they primarily belong to for label positioning
    val monthLabelsData = remember(weeksData) {
        weeksData.keys.groupBy { it.month }
            .mapValues { entry -> entry.value.size } // Month -> Number of weeks starting in it
            .toList()
            .sortedBy { it.first } // Sort months chronologically
    }

    val horizontalScrollState = rememberScrollState()
    var selectedDayInfo = remember { mutableStateOf<DailyQuestInfo?>(null) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            DayLabelsColumn()

            Column {
                MonthLabelsRow(monthLabelsData = monthLabelsData)
                Spacer(modifier = Modifier.height(4.dp))
                ContributionGrid(
                    weeksData = weeksData,
                    onCellClick = { selectedDayInfo.value = it }
                )
            }
        }

        QuestTooltip(
            dailyInfo = selectedDayInfo
        )

        Spacer(modifier = Modifier.height(8.dp))

        ContributionLegend(modifier = Modifier.padding(horizontal = 16.dp))
    }
}


@Composable
private fun DayLabelsColumn(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(DAY_LABEL_WIDTH)
            .padding(top = MONTH_LABEL_HEIGHT + 4.dp), // Align below month labels
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CELL_SPACING) // Match grid spacing
    ) {
        val daysToShow =
            listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY)
        // DayOfWeek.entries is deprecated, use standard list
        listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        ).forEach { day ->
            if (day in daysToShow) {
                Text(
                    text = day.name.take(3)
                        .toLowerCase(Locale.current)
                        .replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.height(CELL_SIZE) // Match cell height
                )
            } else {
                Spacer(modifier = Modifier.height(CELL_SIZE))
            }
        }
    }
}

@Composable
private fun MonthLabelsRow(
    monthLabelsData: List<Pair<Month, Int>> // List of (Month, numberOfWeeks)
) {
    Row(
        modifier = Modifier.height(MONTH_LABEL_HEIGHT),
        verticalAlignment = Alignment.CenterVertically
    ) {
        monthLabelsData.forEachIndexed { index, (month, weekCount) ->
            val totalWidth = (CELL_SIZE * weekCount) + (CELL_SPACING * (weekCount - 1))
            if (index > 0) {
                Spacer(modifier = Modifier.width(MONTH_SPACING))
            }
            Text(
                text = month.name.take(3)
                    .toLowerCase(Locale.current)
                    .replaceFirstChar { it.titlecase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.width(totalWidth) // Assign width to push next label
            )
        }
    }
}

@Composable
private fun ContributionGrid(
    weeksData: Map<LocalDate, List<DailyQuestInfo>>,
    onCellClick: (DailyQuestInfo) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CELL_SPACING)
    ) {
        val sortedWeeks = weeksData.keys.toList()
        sortedWeeks.forEach { weekStartDate ->
            val weekDays = weeksData[weekStartDate] ?: emptyList()
            val daysInWeekMap = weekDays.associateBy { it.date.dayOfWeek }

            Column(
                verticalArrangement = Arrangement.spacedBy(CELL_SPACING)
            ) {
                // Iterate through Monday to Sunday
                listOf(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY
                ).forEach { dayOfWeek ->
                    // Use data if present, otherwise create a placeholder
                    val dayData = daysInWeekMap[dayOfWeek] ?: DailyQuestInfo(
                        // Calculate the correct date for the placeholder cell
                        date = weekStartDate.plus(dayOfWeek.value - DayOfWeek.MONDAY.value, DateTimeUnit.DAY),
                        quests = emptyList(),
                        isPlaceholder = true // Mark as placeholder
                    )
                    ContributionCell(day = dayData, onClick = onCellClick)
                }
            }

        }
    }
}


@Composable
fun ContributionCell(
    day: DailyQuestInfo,
    onClick: ((DailyQuestInfo) -> Unit)
) {
    // Determine contribution level based on the number of quests
    val contributionLevel = when (day.quests.size) {
        0 -> 0
        1 -> 1
        in 2..3 -> 2
        in 4..5 -> 3
        else -> 4 // 6 or more quests
    }

    // Placeholders that are not in the original map should be visually distinct
    val cellColor = if (day.isPlaceholder) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    } else {
        getContributionColor(contributionLevel)
    }

    Box(
        modifier = Modifier
            .size(CELL_SIZE)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(cellColor)
            .clickable(enabled = !day.isPlaceholder) { // Only allow clicking actual data
                onClick(day)
            }
    )
}

@Composable
fun getContributionColor(level: Int): Color {
    return when (level) {
        0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) // No quests
        1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        4 -> MaterialTheme.colorScheme.primary // Max contribution
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
}

@Composable
fun ContributionLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Less",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        (0..4).forEach { level ->
            Box(
                modifier = Modifier
                    .size(CELL_SIZE)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(getContributionColor(level))
            )
            Spacer(modifier = Modifier.width(CELL_SPACING))
        }
        Text(
            text = "More",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}


@Composable
fun QuestTooltip(dailyInfo: MutableState<DailyQuestInfo?>) {
    val context = LocalContext.current
    val questList = remember { mutableMapOf<String, String>()}

    LaunchedEffect(dailyInfo.value) {
        if(dailyInfo.value!=null){
            val dao = QuestDatabaseProvider.getInstance(context).questDao()
            dailyInfo.value!!.quests.forEach {
                questList[it] = dao.getQuestById(it)?.title ?: it
            }
        }
    }

    if (dailyInfo.value != null) {

        Dialog(onDismissRequest = {
            dailyInfo.value = null
        }) {
            Card(
                modifier = Modifier.padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Format date string manually
                    val dayName =
                        dailyInfo.value!!.date.dayOfWeek.name.toLowerCase(Locale.current)
                            .replaceFirstChar { it.titlecase() }
                    val monthName =
                        dailyInfo.value!!.date.month.name.toLowerCase(Locale.current)
                            .replaceFirstChar { it.titlecase() }
                    val dateText =
                        "$dayName, $monthName ${dailyInfo.value!!.date.dayOfMonth}, ${dailyInfo.value!!.date.year}"

                    val questCount = dailyInfo.value!!.quests.size

                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    val questText = if (questCount == 0) {
                        "No quests attempted."
                    } else {
                        "$questCount ${if (questCount == 1) "Quest" else "Quests"} Attempted"
                    }

                    Text(
                        text = questText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    questList.forEach { questName ->
                        Text(
                            text = "• ${questName.value}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

    }
}