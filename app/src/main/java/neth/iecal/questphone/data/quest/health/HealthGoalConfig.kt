package neth.iecal.questphone.data.quest.health

import kotlinx.serialization.Serializable

@Serializable
data class HealthGoalConfig(
    val initial: Int,
    val final: Int,
    val increment: Int
)