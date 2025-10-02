package nethical.questphone.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import nethical.questphone.data.game.Achievements
import nethical.questphone.data.game.InventoryItem
import nethical.questphone.data.game.StreakData
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.time.ExperimentalTime

/**
 * Represents the user's information in the game
 * @param active_boosts A map of active boosts in the game. Format <BoostObject,Timestamp>
 *     timeStamp format: yyyy-dd-mm-hh-mm
 */
@Serializable
data class UserInfo constructor(
    var username: String = "",
    var full_name: String = "",
    var has_profile: Boolean = false,
    var xp : Int= 0,
    var coins: Int = 90,
    var level : Int = 1,
    val inventory: HashMap<InventoryItem, Int> = hashMapOf(Pair(InventoryItem.STREAK_FREEZER,2)),
    var customization_info: CustomizationInfo = CustomizationInfo(),
    val achievements: List<Achievements> = listOf(Achievements.THE_EARLY_FEW),
    var active_boosts: HashMap<InventoryItem,String> = hashMapOf(),
    var last_updated: Long = System.currentTimeMillis(),
    @Serializable(with = JavaInstantSerializer::class) var created_on: Instant = Clock.system(ZoneId.systemDefault()).instant(),
    var streak : StreakData = StreakData(),
    var blockedAndroidPackages: Set<String>? = setOf(),
    var unlockedAndroidPackages: MutableMap<String, Long>? = mutableMapOf(),
    var fcm_tokens : List<String> = listOf(),
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

/**
 * format: yyyy-MM-dd
 */
private fun formatInstantToDate(instant: Instant): String {
    val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
    return localDate.toString() // yyyy-MM-dd
}


object JavaInstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString()) // ISO-8601
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}

/**
 * Converts the level to xp required to level up
 */
fun xpToLevelUp(level: Int): Int {
    return (100 * level * level)
}

/**
 * The xp that is rewarded when user completes a quest
 */
fun xpToRewardForQuest(level: Int, multiplier: Int = 1): Int {
    return maxOf((30 * level + 50) * multiplier, 150)
}