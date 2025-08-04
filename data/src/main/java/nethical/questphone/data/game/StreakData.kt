package nethical.questphone.data.game

import kotlinx.serialization.Serializable


@Serializable
data class StreakData(
    var currentStreak: Int = 0,
    var longestStreak: Int = 0,
    var lastCompletedDate: String = "0001-01-01",
)

/**
 * @property isOngoing true if streak has not been broken
 * @property streakFreezersUsed null if no streak freezers used
 * @property streakDaysLost null if user didn't lose a streak
 */
data class StreakFreezerReturn(
    val isOngoing: Boolean = false,
    val streakFreezersUsed: Int? = null,
    val streakDaysLost: Int? = null,
    val lastStreak: Int = 0
)

fun xpFromStreak(dayStreak: Int): Int {
    return (10 * dayStreak) + (dayStreak * dayStreak / 2)
}