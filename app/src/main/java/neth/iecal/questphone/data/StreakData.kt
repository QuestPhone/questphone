package nethical.questphone.data.game

import android.util.Log
import kotlinx.serialization.Serializable
import neth.iecal.questphone.data.InventoryItem
import neth.iecal.questphone.data.User
import neth.iecal.questphone.data.addItemsToInventory
import neth.iecal.questphone.data.addXp
import neth.iecal.questphone.data.getInventoryItemCount
import neth.iecal.questphone.data.saveUserInfo
import neth.iecal.questphone.data.useInventoryItem
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

@Serializable
data class StreakData(
    var currentStreak: Int = 0,
    var longestStreak: Int = 0,
    var lastCompletedDate: String = "0001-01-01",
)


fun xpFromStreak(dayStreak: Int): Int {
    return (10 * dayStreak) + (dayStreak * dayStreak / 2)
}

/**
 * Represents the return value of [checkIfStreakFailed]
 * @param streakFreezersUsed The number of streak freezers used if the user failed to complete the streak or null if not used
 * @param streakDaysLost The numbers of streak days lost if the user failed to complete the streak or null if not used
 */
data class StreakCheckReturn(
    val streakFreezersUsed: Int? = null,
    val streakDaysLost: Int? = null
)

/**
 * Checks if the user has failed a streak
 * @return returns null if the user has not failed the streak or [StreakCheckReturn] if user either failed to complete the streak or used streak freezers.
 */
fun User.checkIfStreakFailed(): StreakCheckReturn? {
    val today = LocalDate.now()
    val streakData = userInfo.streak
    val lastCompleted = LocalDate.parse(streakData.lastCompletedDate)
    val daysSince = ChronoUnit.DAYS.between(lastCompleted, today)
    Log.d("streak day since",daysSince.toString())

    // failed to complete streak
    if(daysSince>=2){
        // check if user has available streak freezers
        val requiredStreakFreezers = (daysSince-1).toInt()
        // user has enough streak freezers, hence use them
            if(getInventoryItemCount(InventoryItem.STREAK_FREEZER) >= requiredStreakFreezers){
                useInventoryItem(InventoryItem.STREAK_FREEZER,requiredStreakFreezers)

                val oldStreak = streakData.currentStreak
                streakData.currentStreak += requiredStreakFreezers
                streakData.lastCompletedDate = today.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                // Grant XP for each missed day
                lastXpEarned = (oldStreak until streakData.currentStreak).sumOf { day ->
                    val xp = xpFromStreak(day)
                    User.addXp(xp)
                    xp
                }

                saveUserInfo()
                return StreakCheckReturn(streakFreezersUsed = requiredStreakFreezers, streakDaysLost = null)
            }else{
                // user has no streak freezer
                val oldStreak = streakData.currentStreak
                streakData.longestStreak = maxOf(streakData.currentStreak, streakData.longestStreak)
                streakData.currentStreak = 0
                saveUserInfo()
                return StreakCheckReturn(
                    streakFreezersUsed = null, streakDaysLost = oldStreak
                )
            }
    }

    // neither failed to complete streak nor used streak freezers
    return null
}

fun User.continueStreak(): Boolean {
    val today = LocalDate.now()
    val streakData = userInfo.streak
    val lastCompleted = LocalDate.parse(streakData.lastCompletedDate)
    val daysSince = ChronoUnit.DAYS.between(lastCompleted, today)

    Log.d("daysSince",daysSince.toString())
    if(daysSince!=0L){
        streakData.currentStreak += 1
        streakData.longestStreak = maxOf(streakData.currentStreak, streakData.longestStreak)
        streakData.lastCompletedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        saveUserInfo()
        return true
    }
    return false
}

fun User.addLevelUpRewards(): HashMap<InventoryItem,Int>{
    var levelUpInventoryItem  = hashMapOf<InventoryItem, Int>()
    levelUpInventoryItem.put(InventoryItem.QUEST_SKIPPER, 1)
    if (userInfo.level % 2 == 0) {
        levelUpInventoryItem.put(InventoryItem.XP_BOOSTER, 1)
    }
    if (userInfo.level % 5 == 0) {
        levelUpInventoryItem.put(InventoryItem.STREAK_FREEZER, 1)
    }

    addItemsToInventory(levelUpInventoryItem)
    return levelUpInventoryItem
}


