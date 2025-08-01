package nethical.questphone.data.quest.focus

import kotlinx.serialization.Serializable
import nethical.questphone.data.ExcludeFromReviewDialog

/**
 * Stores time related data for quests
 *
 * @property initialTime
 * @property finalTime
 * @property incrementTime
 * @property initialUnit
 * @property finalUnit
 * @property incrementUnit
 */
@Serializable
data class FocusTimeConfig(
    var initialTime: String = "5",
    var finalTime: String = "5",
    var incrementTime: String = "15",
    var initialUnit: String = "m",
    var finalUnit: String = "h",
    var incrementUnit: String = "m"
) {

    @ExcludeFromReviewDialog
    val initialTimeInMs: Long get() = convertToMillis(initialTime, initialUnit)
    @ExcludeFromReviewDialog
    val finalTimeInMs: Long get() = convertToMillis(finalTime, finalUnit)
    @ExcludeFromReviewDialog
    val incrementTimeInMs: Long get() = convertToMillis(incrementTime, incrementUnit)

    private fun convertToMillis(time: String, unit: String): Long {
        val timeValue = time.toLongOrNull() ?: return 0L
        return when (unit) {
            "h" -> timeValue * 60 * 60 * 1000
            "m" -> timeValue * 60 * 1000
            else -> 0L
        }
    }
}
