package neth.iecal.questphone.data

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import nethical.questphone.core.core.utils.formatInstantToDate
import nethical.questphone.data.game.Achievements
import nethical.questphone.data.game.StreakData
import kotlin.time.ExperimentalTime

/**
 * Represents the user's information in the game
 * @param active_boosts A map of active boosts in the game. Format <BoostObject,Timestamp>
 *     timeStamp format: yyyy-dd-mm-hh-mm
 */
@Serializable
data class UserInfo @OptIn(ExperimentalTime::class) constructor(
    var username: String = "",
    var full_name: String = "",
    var has_profile: Boolean = false,
    var xp : Int= 0,
    var coins: Int = 0,
    var level : Int = 1,
    val inventory: HashMap<InventoryItem, Int> = hashMapOf(Pair(InventoryItem.STREAK_FREEZER,2)),
    val achievements: List<Achievements> = listOf(Achievements.THE_EARLY_FEW),
    var active_boosts: HashMap<InventoryItem,String> = hashMapOf(),
    var last_updated: Long = System.currentTimeMillis(),
    var created_on: Instant = Clock.System.now(),
    var streak : StreakData = StreakData(),
    @Transient
    var needsSync: Boolean = true,
    @Transient
    var isAnonymous : Boolean = false,
){
    fun getFirstName(): String {
        return full_name.trim().split(" ").firstOrNull() ?: ""
    }

    @OptIn(ExperimentalTime::class)
    fun getCreatedOnString():String{
        return formatInstantToDate(created_on)
    }
}