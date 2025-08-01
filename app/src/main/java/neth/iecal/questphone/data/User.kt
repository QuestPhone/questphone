package neth.iecal.questphone.data

import android.content.Context
import androidx.core.content.edit
import io.github.jan.supabase.auth.auth
import neth.iecal.questphone.data.User.lastRewards
import neth.iecal.questphone.data.User.lastXpEarned
import nethical.questphone.backend.Supabase
import nethical.questphone.backend.triggerProfileSync
import nethical.questphone.core.core.utils.isTimeOver
import nethical.questphone.data.json
import kotlin.time.ExperimentalTime


/**
 * Represents the user in the game
 * @param lastXpEarned The amount of xp that user earned the last time through means like streak, quests etc. Used by dialogs and stuff to display information
 * @param lastRewards same as [lastXpEarned] but instead for rewards
 */
object User {
    lateinit var appContext: Context
    lateinit var userInfo: UserInfo

    var lastXpEarned: Int? = null
    var lastRewards: List<InventoryItem>? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        userInfo = getUserInfo(appContext)
    }

    fun getUserId(): String {
        return if (userInfo.isAnonymous){
            ""
        } else {
            val sp = appContext.getSharedPreferences("authtoke", Context.MODE_PRIVATE)
            var id = sp.getString("key",null)
            if(id!= null) return id
            id = Supabase.supabase.auth.currentUserOrNull()!!.id
            sp.edit { putString("key",id) }
            Supabase.supabase.auth.currentUserOrNull()!!.id
        }
    }
}


/**
 * Converts the level to xp required to level up
 */
fun xpToLevelUp(level: Int): Int {
    return (50 * level * level - 50 * level)
}

/**
 * The xp that is rewarded when user completes a quest
 */
fun xpToRewardForQuest(level: Int, multiplier: Int = 1): Int {
    return (20 * level + 30) * multiplier
}

/**
 * Adds xp to the user and checks if the user has leveled up
 * @param xp The xp to add
 * @return userinfo with the new xp and level
 */
fun User.addXp(xp: Int){
    removeInactiveBooster()
    val multiplier = if(isBoosterActive(InventoryItem.XP_BOOSTER)) 2 else 1
    userInfo.xp += xp * multiplier
    while(userInfo.xp >= xpToLevelUp(userInfo.level+1)){
        userInfo.level++
    }
    saveUserInfo()
}

fun User.removeInactiveBooster() {
    userInfo.active_boosts.forEach {
        if(isTimeOver(it.value)){
            userInfo.active_boosts.remove(it.key)
        }
    }
    saveUserInfo()
}

fun User.isBoosterActive(reward: InventoryItem): Boolean {
    if (userInfo.active_boosts.contains(reward)) {
        val isBoosterActive =
            !isTimeOver(userInfo.active_boosts.getOrDefault(reward, "9999-09-09-09-09"))
        if (isBoosterActive) removeInactiveBooster()
        return isBoosterActive
    }
    return false

}

fun User.addItemsToInventory(items: HashMap<InventoryItem, Int>){
    items.forEach {
        userInfo.inventory.put(it.key,it.value+getInventoryItemCount(it.key))
    }
    saveUserInfo()
}


fun User.saveUserInfo(isSetLastUpdated: Boolean = true){
    val sharedPreferences = appContext.getSharedPreferences("user_info", Context.MODE_PRIVATE)
    if(isSetLastUpdated && !userInfo.isAnonymous){
        userInfo.last_updated = System.currentTimeMillis()
        userInfo.needsSync = true
        triggerProfileSync(appContext)
    }
    sharedPreferences.edit { putString("user_info", json.encodeToString(userInfo)) }
}


fun User.getInventoryItemCount(item: InventoryItem): Int{
    return userInfo.inventory.getOrDefault(item,0)
}

fun User.useInventoryItem(item: InventoryItem, count:Int = 1){
    if(userInfo.inventory.getOrDefault(item,0) > 0){
        userInfo.inventory.put(item,getInventoryItemCount(item)-count)
        if(getInventoryItemCount(item) == 0){
            userInfo.inventory.remove(item)
        }
        saveUserInfo()
    }
}

@OptIn(ExperimentalTime::class)
fun getUserInfo(context: Context): UserInfo {
    val sharedPreferences = context.getSharedPreferences("user_info", Context.MODE_PRIVATE)
    val userInfoJson = sharedPreferences.getString("user_info", null)
    return if (userInfoJson != null) {
        json.decodeFromString(userInfoJson)
    } else {
        UserInfo("")
    }
}
fun User.useCoins(coins: Int){
    userInfo.coins-=coins
    saveUserInfo()
}
fun User.addCoins(coins:Int){
    userInfo.coins+=coins
    saveUserInfo()
}