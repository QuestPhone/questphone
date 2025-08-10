package nethical.questphone.data.quest.health

import kotlinx.serialization.Serializable

/**
 * TODO
 *
 * @property type
 * @property healthGoalConfig
 * @property nextGoal stores the goal for the current date in case wherein the quest is not completed.or stores the next goal when complete
 */
@Serializable
data class HealthQuest(
    val type: HealthTaskType = HealthTaskType.STEPS,
    val healthGoalConfig: HealthGoalConfig = HealthGoalConfig(1000,1000,0),
    var nextGoal: Int = healthGoalConfig.initial
){
    fun incrementGoal() {
        if (nextGoal < healthGoalConfig.final) {
            nextGoal = minOf(
                nextGoal + healthGoalConfig.increment,
                healthGoalConfig.final
            )
        }
    }
}