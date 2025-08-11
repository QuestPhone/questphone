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
    val base = 20 * dayStreak                           // slightly stronger linear growth
    val bonus = (dayStreak * dayStreak) / 3             // gentler quadratic growth
    val milestoneBonus = when {
        dayStreak % 30 == 0 -> 500                      // monthly bonus
        dayStreak % 14 == 0 -> 200                      // 2-week bonus
        dayStreak % 7 == 0  -> 100                      // weekly bonus
        else -> 0
    }
    return base + bonus + milestoneBonus
}