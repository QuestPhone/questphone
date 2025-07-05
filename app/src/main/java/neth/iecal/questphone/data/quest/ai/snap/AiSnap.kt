package neth.iecal.questphone.data.quest.ai.snap

import kotlinx.serialization.Serializable

@Serializable
data class AiSnap(
    val taskDescription: String = "",
    var features: MutableList<String> = mutableListOf()
)