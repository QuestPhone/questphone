package neth.iecal.questphone.backed.repositories

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import neth.iecal.questphone.backed.triggerProfileSync
import neth.iecal.questphone.core.Supabase
import nethical.questphone.core.core.utils.getCurrentDate
import nethical.questphone.core.core.utils.getFullTimeAfter
import nethical.questphone.core.core.utils.isTimeOver
import nethical.questphone.data.UserInfo
import nethical.questphone.data.game.InventoryItem
import nethical.questphone.data.game.StreakFreezerReturn
import nethical.questphone.data.json
import nethical.questphone.data.xpToLevelUp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.ExperimentalTime

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UserRepositoryEntryPoint {
    fun userRepository(): UserRepository
}

@Singleton
class UserRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val statsRepository: StatsRepository,
    private val questRepository: QuestRepository
) {
    var userInfo: UserInfo = loadUserInfo()
    var coinsState = MutableStateFlow(userInfo.coins)
    var currentStreakState = MutableStateFlow(userInfo.streak.currentStreak)

    var activeBoostsState = MutableStateFlow(userInfo.active_boosts)
    
    // the below variables act as a trigger for launching the reward dialog declared in the MainActivity from a
    // different SubScreen.
    fun getUserId(): String {
        return if (userInfo.isAnonymous){
            ""
        } else {
            val sp = context.getSharedPreferences("authtoken", Context.MODE_PRIVATE)
            var id = sp.getString("key",null)
            if(id!= null) return id
            id = Supabase.supabase.auth.currentUserOrNull()?.id ?: return ""
            sp.edit { putString("key",id) }
            Supabase.supabase.auth.currentUserOrNull()!!.id
        }
    }

    fun addXp(xp: Int) {
        removeInactiveBooster()
        val multiplier = if (isBoosterActive(InventoryItem.XP_BOOSTER)) 2 else 1
        userInfo.xp += xp * multiplier
        while (userInfo.xp >= xpToLevelUp(userInfo.level )) {
            userInfo.xp -= xpToLevelUp(userInfo.level)
            userInfo.level++
        }
        saveUserInfo()
    }

    fun removeInactiveBooster() {
        userInfo.active_boosts.entries.removeIf { isTimeOver(it.value) }
        activeBoostsState.value = userInfo.active_boosts
        saveUserInfo()
    }

    fun activateBoost(item: InventoryItem, hoursToAdd: Long, minsToAdd: Long){
        userInfo.active_boosts.put(InventoryItem.XP_BOOSTER, getFullTimeAfter(hoursToAdd, minsToAdd))
        saveUserInfo()
        //update state
        activeBoostsState.value = userInfo.active_boosts
    }
    fun isBoosterActive(reward: InventoryItem): Boolean {
        if (userInfo.active_boosts.contains(reward)) {
            val isActive = !isTimeOver(userInfo.active_boosts.getOrDefault(reward, "0069-69-69-69-69"))
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
        coinsState.value = userInfo.coins
    }

    fun getInventoryItemCount(item: InventoryItem): Int {
        return userInfo.inventory.getOrDefault(item, 0)
    }

    fun updateBlockedAppsSet(set: Set<String>){
        userInfo.blockedAndroidPackages = set
        saveUserInfo()
    }

    fun updateUnlockedAppsSet(set: Map<String,Long>){
        userInfo.unlockedAndroidPackages = set.toMutableMap()
        saveUserInfo()
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
    fun getBlockedPackages():Set<String>{
        return userInfo.blockedAndroidPackages?:emptySet()
    }
    fun getUnlockedPackages(): MutableMap<String, Long>{
        return userInfo.unlockedAndroidPackages ?: mutableMapOf()
    }

    fun useCoins(number: Int) {
        userInfo.coins -= number
        coinsState.value -= number
        saveUserInfo()
    }

    fun addCoins(addedCoins: Int) {
        userInfo.coins += addedCoins
        coinsState.value+=addedCoins
        saveUserInfo()
    }

    /**
     * @return failing for how many days or null if not failing
     */
    fun checkIfStreakFailed(): Int? {
        val today = LocalDate.now()
        val streakData = userInfo.streak
        val lastCompleted = LocalDate.parse(streakData.lastCompletedDate)
        val daysSince = ChronoUnit.DAYS.between(lastCompleted, today)
        Log.d("streak day since", daysSince.toString())

        if (daysSince > 1) {
            return daysSince.toInt()
        }
            return null

    }



    fun tryUsingStreakFreezers(daysSince:Int): StreakFreezerReturn {
        val requiredFreezers = (daysSince -1).toInt()
        val today = LocalDate.now()
        if (getInventoryItemCount(InventoryItem.STREAK_FREEZER) >= requiredFreezers) {
            deductFromInventory(InventoryItem.STREAK_FREEZER, requiredFreezers)

            val oldStreak = userInfo.streak.currentStreak
            userInfo.streak.currentStreak += requiredFreezers
            userInfo.streak.lastCompletedDate = today.minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            currentStreakState.value = userInfo.streak.currentStreak
            saveUserInfo()
            return StreakFreezerReturn(isOngoing = true,streakFreezersUsed = requiredFreezers, lastStreak = oldStreak)
        } else {
            // User failed streak
            val oldStreak = userInfo.streak.currentStreak
            userInfo.streak.longestStreak = maxOf(userInfo.streak.currentStreak, userInfo.streak.longestStreak)
            userInfo.streak.currentStreak = 0
            currentStreakState.value = userInfo.streak.currentStreak
            updateStreakHistory(oldStreak)
            saveUserInfo()
            return StreakFreezerReturn(isOngoing = false,streakDaysLost = oldStreak)
        }
    }

    private fun updateStreakHistory(oldStreak: Int){
        val streakHistory = userInfo.streak.streakFailureHistory.toMutableMap()
        streakHistory[getCurrentDate()] = oldStreak
        userInfo.streak.streakFailureHistory = streakHistory
    }
    fun continueStreak(): Boolean {
        val today = LocalDate.now()
        val lastCompleted = LocalDate.parse(userInfo.streak.lastCompletedDate)
        val daysSince = ChronoUnit.DAYS.between(lastCompleted, today)

        Log.d("daysSince",daysSince.toString())
        if(daysSince!=0L){
            userInfo.streak.currentStreak += 1
            userInfo.streak.longestStreak = maxOf(userInfo.streak.currentStreak, userInfo.streak.longestStreak)
            userInfo.streak.lastCompletedDate = getCurrentDate()
            currentStreakState.value = userInfo.streak.currentStreak

            saveUserInfo()
            return true
        }
        return false
    }


    fun calculateLevelUpInvRewards(): HashMap<InventoryItem, Int> {
        val rewards = hashMapOf<InventoryItem, Int>()
        rewards[InventoryItem.QUEST_SKIPPER] = 1
        if(userInfo.level == 2) rewards[InventoryItem.REWARD_TIME_EDITOR] = 1
        if (userInfo.level % 2 == 0) rewards[InventoryItem.XP_BOOSTER] = 1
        if (userInfo.level % 5 == 0) rewards[InventoryItem.STREAK_FREEZER] = 1
        return rewards
    }
    fun calculateLevelUpCoinsRewards(): Int {
        return maxOf(userInfo.level.times(userInfo.level),50)
    }


    @OptIn(ExperimentalTime::class)
    fun loadUserInfo(): UserInfo {
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
        context.deleteSharedPreferences("crnt_pg_onboard")
        context.deleteSharedPreferences("onboard")
        deleteLocalUserInfoCache()

        questRepository.deleteAll()
        statsRepository.deleteAll()

        Supabase.supabase.auth.signOut()

    }

    fun saveFcmToken(string: String) {
        val tokens = userInfo.fcm_tokens.toMutableList()
        tokens.add(string)
        userInfo.fcm_tokens = tokens
        saveUserInfo()
        Log.d("saved Fcm token", tokens.toString())
    }
}
