package nethical.questphone.data.game

import kotlinx.serialization.Serializable

@Serializable
enum class Achievements(val xp:Int, val message: String) {
    THE_EARLY_FEW(1000,"For the people using questphone from day 1")
}