package nethical.questphone.data.quest.health

import kotlinx.serialization.Serializable

@Serializable
data class HealthGoalConfig(
    var initial: Int,
    var final: Int,
    var increment: Int
)