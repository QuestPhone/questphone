package nethical.questphone.data.game

import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime


/**
 * format: yyyy-MM-dd
 */
@OptIn(ExperimentalTime::class)
private fun formatInstantToDate(instant: Instant): String {
    val localDate = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date
    return localDate.toString() // yyyy-MM-dd
}