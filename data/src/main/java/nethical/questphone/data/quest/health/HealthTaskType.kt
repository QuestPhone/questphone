package nethical.questphone.data.quest.health

enum class HealthTaskType(val label: String, val unit: String,) {
    STEPS("Steps","steps"), CALORIES("Calories Burned","cal"), DISTANCE("Distance Covered","m"), SLEEP("Sleep","hrs"), WATER_INTAKE("Water Intake","l")
}
