package nethical.questphone.backend.repositories

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.auth
import nethical.questphone.backend.Supabase
import nethical.questphone.backend.triggerProfileSync
import nethical.questphone.core.core.utils.isTimeOver
import nethical.questphone.data.game.InventoryItem
import nethical.questphone.data.game.StreakCheckReturn
import nethical.questphone.data.game.UserInfo
import nethical.questphone.data.game.xpToLevelUp
import nethical.questphone.data.json
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class UserRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val statsRepository: StatsRepository,
    private val questRepository: QuestRepository
) {
    var userInfo: UserInfo = loadUserInfo()
    var coins by mutableIntStateOf(userInfo.coins)
    var currentStreak by mutableIntStateOf(userInfo.streak.currentStreak)

    // the below variables act as a trigger for launching the reward dialog declared in the MainActivity from a
    // different SubScreen.
    fun getUserId(): String {
        return if (userInfo.isAnonymous){
            ""
        } else {
            val sp = context.getSharedPreferences("authtoken", Context.MODE_PRIVATE)
            var id = sp.getString("key",null)
            if(id!= null) return id
            id = Supabase.supabase.auth.currentUserOrNull()!!.id
            sp.edit { putString("key",id) }
            Supabase.supabase.auth.currentUserOrNull()!!.id
        }
    }

    fun addXp(xp: Int) {
        removeInactiveBooster()
        val multiplier = if (isBoosterActive(InventoryItem.XP_BOOSTER)) 2 else 1
        userInfo.xp += xp * multiplier
        while (userInfo.xp >= xpToLevelUp(userInfo.level + 1)) {
            userInfo.level++
        }
        saveUserInfo()
    }

    fun removeInactiveBooster() {
        userInfo.active_boosts.entries.removeIf { isTimeOver(it.value) }
        saveUserInfo()
    }

    fun isBoosterActive(reward: InventoryItem): Boolean {
        if (userInfo.active_boosts.contains(reward)) {
            val isActive = !isTimeOver(userInfo.active_boosts.getOrDefault(reward, "9999-09-09-09-09"))
            if (!isActive) removeInactiveBooster()
            return isActive
        }
        return false
    }

    fun addItemsToInventory(items: HashMap<InventoryItem, Int>) {
        items.forEach {
            userInfo.inventory[it.key] = it.value + getInventoryItemCount(it.key)
        }
        saveUserInfo()
    }

    fun saveUserInfo(isSetLastUpdated: Boolean = true) {
        if (isSetLastUpdated && !userInfo.isAnonymous) {
            userInfo.last_updated = System.currentTimeMillis()
            userInfo.needsSync = true
            triggerProfileSync(context)
        }
        context.getSharedPreferences("user_info", Context.MODE_PRIVATE)
            .edit { putString("user_info", json.encodeToString(userInfo)) }
    }

    fun getInventoryItemCount(item: InventoryItem): Int {
        return userInfo.inventory.getOrDefault(item, 0)
    }

    fun deductFromInventory(item: InventoryItem, count: Int = 1) {
        if (getInventoryItemCount(item) > 0) {
            userInfo.inventory[item] = getInventoryItemCount(item) - count
            if (getInventoryItemCount(item) == 0) {
                userInfo.inventory.remove(item)
            }
            saveUserInfo()
        }
    }

    fun useCoins(number: Int) {
        userInfo.coins -= number
        coins -= number
        saveUserInfo()
    }

    fun addCoins(coins: Int) {
        userInfo.coins += coins
        saveUserInfo()
    }

    fun checkIfStreakFailed(): StreakCheckReturn? {
        val today = LocalDate.now()
        val streakData = userInfo.streak
        val lastCompleted = LocalDate.parse(streakData.lastCompletedDate)
        val daysSince = ChronoUnit.DAYS.between(lastCompleted, today) - 1
        Log.d("streak day since", daysSince.toString())

        if (daysSince >= 1) {
            val requiredFreezers = (daysSince).toInt()
            if (getInventoryItemCount(InventoryItem.STREAK_FREEZER) >= requiredFreezers) {
                deductFromInventory(InventoryItem.STREAK_FREEZER, requiredFreezers)

                val oldStreak = streakData.currentStreak
                streakData.currentStreak += requiredFreezers
                streakData.lastCompletedDate = today.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                saveUserInfo()
                return StreakCheckReturn(isOngoing = true,streakFreezersUsed = requiredFreezers, lastStreak = oldStreak)
            } else {
                // User failed streak
                val oldStreak = streakData.currentStreak
                streakData.longestStreak = maxOf(streakData.currentStreak, streakData.longestStreak)
                streakData.currentStreak = 0
                saveUserInfo()
                return StreakCheckReturn(isOngoing = false,streakDaysLost = oldStreak)
            }
        }
        // day not passed, no action
        return null
    }


    fun calculateLevelUpInvRewards(): HashMap<InventoryItem, Int> {
        val rewards = hashMapOf<InventoryItem, Int>()
        rewards[InventoryItem.QUEST_SKIPPER] = 1
        if (userInfo.level % 2 == 0) rewards[InventoryItem.XP_BOOSTER] = 1
        if (userInfo.level % 5 == 0) rewards[InventoryItem.STREAK_FREEZER] = 1
        return rewards
    }
    fun calculateLevelUpCoinsRewards(): Int {
        return maxOf(userInfo.level.times(userInfo.level),50)
    }


    private fun loadUserInfo(): UserInfo {
        val sharedPreferences = context.getSharedPreferences("user_info", Context.MODE_PRIVATE)
        val userInfoJson = sharedPreferences.getString("user_info", null)
        return userInfoJson?.let {
            json.decodeFromString(it)
        } ?: UserInfo()
    }

    private fun deleteLocalUserInfoCache(){
        val sharedPreferences = context.getSharedPreferences("user_info", Context.MODE_PRIVATE)
        sharedPreferences.edit { remove("user_info") }
    }

    suspend fun signOut() {
        val sharedPrefs = context.getSharedPreferences("onboard", MODE_PRIVATE)
        sharedPrefs.edit { putBoolean("onboard", false) }
        deleteLocalUserInfoCache()

        questRepository.deleteAll()
        statsRepository.deleteAll()

        Supabase.supabase.auth.signOut()

    }
}
