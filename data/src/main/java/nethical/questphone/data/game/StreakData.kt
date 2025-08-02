package nethical.questphone.data.game

import kotlinx.serialization.Serializable


@Serializable
data class StreakData(
    var currentStreak: Int = 0,
    var longestStreak: Int = 0,
    var lastCompletedDate: String = "0001-01-01",
)

data class StreakCheckReturn(
    val streakFreezersUsed: Int? = null,
    val streakDaysLost: Int? = null
)

fun xpFromStreak(dayStreak: Int): Int {
    return (10 * dayStreak) + (dayStreak * dayStreak / 2)
}