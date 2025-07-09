package neth.iecal.questphone.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import neth.iecal.questphone.data.DayOfWeek
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant.ofEpochSecond
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * format: yyyy-MM-dd
 */
fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}


fun getCurrentTime12Hr(): String {
    return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()) // HH:MM AM/PM format
}

/**
 * format: yyyy-MM-dd
 */
fun formatInstantToDate(instant: Instant): String {
    val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    return localDate.toString() // yyyy-MM-dd
}

/**
 * format: yyyy-MM-dd
 */
fun getPreviousDay(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DATE, -1) // Subtract 1 day
    return sdf.format(calendar.time)
}

/**
 * format: yyyy-MM-dd
 */
fun getDateFromString(dateStr: String): Date {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.parse(dateStr) ?: Date()
}

fun getDayName(date: Date): String {
    val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
    return sdf.format(date)
}

/**
 * format: yyyy-MM-dd-HH-mm
 */
fun getFullFormattedTime(): String {
    val current = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern("yyyy-dd-MM-HH-mm")
    return current.format(formatter)
}

/**
 * checks time in format: yyyy-MM-dd-HH-mm
 */
fun isTimeOver(targetTimeStr: String): Boolean {
    val formatter = DateTimeFormatter.ofPattern("yyyy-dd-MM-HH-mm")
    return try {
        val targetTime = LocalDateTime.parse(targetTimeStr, formatter)
        val now = LocalDateTime.now()
        now.isAfter(targetTime)
    } catch (e: Exception) {
        e.printStackTrace()
        false // or throw an error if you prefer
    }
}

/**
 * format: yyyy-MM-dd-HH-mm
 */
fun getFullTimeAfter(hoursToAdd: Long, minutesToAdd: Long): String {
    val current = LocalDateTime.now()
    val futureTime = current.plusHours(hoursToAdd).plusMinutes(minutesToAdd)
    val formatter = DateTimeFormatter.ofPattern("yyyy-dd-MM-HH-mm")
    return futureTime.format(formatter)
}

/**
 * converts the time in format: yyyy-dd-MM-HH-mm to a ux friendly string
 * 
 */
fun formatRemainingTime(timeString: String): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-dd-MM-HH-mm")
        val endTime = LocalDateTime.parse(timeString, formatter)
        val now = LocalDateTime.now()

        if (endTime.isBefore(now)) return "Already ended"

        val duration = Duration.between(now, endTime)
        val totalMinutes = duration.toMinutes()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        when {
            totalMinutes <= 1 -> "Ending now"
            hours == 0L       -> "Ends in ${minutes}m"
            minutes == 0L     -> "Ends in ${hours}h"
            else              -> "Ends in ${hours}h ${minutes}m"
        }
    } catch (e: Exception) {
        "Invalid time"
    }
}
fun getCurrentDay(): DayOfWeek {
    val calendar = Calendar.getInstance()
    return calendar.convertToDayOfWeek()
}

fun Calendar.convertToDayOfWeek(): DayOfWeek {

    return when ((this.get(Calendar.DAY_OF_WEEK))) {
        Calendar.MONDAY -> DayOfWeek.MON
        Calendar.TUESDAY -> DayOfWeek.TUE
        Calendar.WEDNESDAY -> DayOfWeek.WED
        Calendar.THURSDAY -> DayOfWeek.THU
        Calendar.FRIDAY -> DayOfWeek.FRI
        Calendar.SATURDAY -> DayOfWeek.SAT
        Calendar.SUNDAY -> DayOfWeek.SUN
        else -> throw IllegalStateException("Invalid day")
    }
}

fun java.time.DayOfWeek.convertToDayOfWeek(): DayOfWeek {
   return when (this) {
        java.time.DayOfWeek.MONDAY -> DayOfWeek.MON
        java.time.DayOfWeek.TUESDAY -> DayOfWeek.TUE
        java.time.DayOfWeek.WEDNESDAY -> DayOfWeek.WED
        java.time.DayOfWeek.THURSDAY -> DayOfWeek.THU
        java.time.DayOfWeek.FRIDAY -> DayOfWeek.FRI
        java.time.DayOfWeek.SATURDAY -> DayOfWeek.SAT
        java.time.DayOfWeek.SUNDAY -> DayOfWeek.SUN
    }
}

fun DayOfWeek.toJavaDayOfWeek(): java.time.DayOfWeek {
    return when (this) {
        DayOfWeek.MON -> java.time.DayOfWeek.MONDAY
        DayOfWeek.TUE -> java.time.DayOfWeek.TUESDAY
        DayOfWeek.WED -> java.time.DayOfWeek.WEDNESDAY
        DayOfWeek.THU -> java.time.DayOfWeek.THURSDAY
        DayOfWeek.FRI -> java.time.DayOfWeek.FRIDAY
        DayOfWeek.SAT -> java.time.DayOfWeek.SATURDAY
        DayOfWeek.SUN -> java.time.DayOfWeek.SUNDAY
    }
}


fun formatHour(hour: Int): String {
    return when (hour) {
        0, 24 -> "12 AM" // Midnight fix
        12 -> "12 PM"
        in 1..11 -> "$hour AM"
        else -> "${hour - 12} PM"
    }
}
fun getAllDatesBetween(startDate: Date, endDate: Date): List<Date> {
    val dates = mutableListOf<Date>()
    val cal = Calendar.getInstance()
    cal.time = startDate

    while (!cal.time.after(endDate)) {
        dates.add(cal.time)
        cal.add(Calendar.DATE, 1)
    }
    return dates
}


fun daysSince(
    dateString: String,
    allowedDays: Set<java.time.DayOfWeek>
): Int {
    val inputDate = LocalDate.parse(dateString)
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    val start = minOf(inputDate, today)
    val end = maxOf(inputDate, today)

    var count = 0
    var current = start

    while (current <= end) {
        if (current.dayOfWeek in allowedDays) {
            count++
        }
        current = current.plus(1, DateTimeUnit.DAY)
    }

    return count
}
fun LocalDate.getStartOfWeek(): LocalDate {
    // ISO 8601 week starts on Monday
    val dayOfWeek = this.dayOfWeek.isoDayNumber // Monday = 1, Sunday = 7
    return this.minus(dayOfWeek - 1, DateTimeUnit.DAY)
}

fun calculateMonthsPassedAndRoundedStart(input: Instant): LocalDate?{
    val now = Clock.System.now()

    // Convert both to LocalDate in UTC
    val today = now.toLocalDateTime(TimeZone.UTC).date
    val startDate = input.toLocalDateTime(TimeZone.UTC).date

    // Calculate how many months have passed
    val monthsPassed = ((today.year - startDate.year) * 12) + (today.monthNumber - startDate.monthNumber)

    // If more than 12 months, return rounded start date (next Jan 1)
    val roundedStart = if (monthsPassed > 12) {
        LocalDate(startDate.year + 1, 1, 1)
    } else {
        null // No need to round
    }

    return roundedStart
}

fun getTimeRemainingDescription(endHour: Int): String {
    val calendar = Calendar.getInstance()
    val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
    val currentMinute = calendar.get(Calendar.MINUTE)

    // End time in total minutes (e.g., 18:00 = 1080 mins)
    val endTimeInMinutes = endHour * 60
    val currentTimeInMinutes = currentHour * 60 + currentMinute

    val minutesRemaining = endTimeInMinutes - currentTimeInMinutes

    return when {
        minutesRemaining < 1 -> "Time is over"
        else -> {
            val hours = minutesRemaining / 60
            val minutes = minutesRemaining % 60

            buildString {
                if (hours > 0) append("$hours hour${if (hours > 1) "s" else ""} ")
                if (minutes > 0) append("$minutes minute${if (minutes > 1) "s" else ""}")
            }.trim()
        }
    }
}

fun unixToReadable(unixTime: Long, inMillis: Boolean = false, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
    val instant = if (inMillis) java.time.Instant.ofEpochMilli(unixTime) else ofEpochSecond(unixTime)
    val dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern(pattern)
    return dateTime.format(formatter)
}
fun readableTimeRange(range: List<Int>): String {
    val (start, end) = range
    fun formatHour(h: Int) = when (h % 24) {
        0 -> "12 am"
        12 -> "12 pm"
        in 1..11 -> "$h am"
        else -> "${h - 12} pm"
    }

    return when {
        start == 0 && end == 24 -> "entire day"
        start == end -> "at ${formatHour(start)}"
        else -> "${formatHour(start)} to ${formatHour(end)}"
    }
}
