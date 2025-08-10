package neth.iecal.questphone.data

import androidx.navigation.NavController
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.backend.repositories.StatsRepository
import nethical.questphone.backend.repositories.UserRepository

data class InventoryExecParams(
    val navController: NavController,
    val userRepository: UserRepository,
    val questRepository: QuestRepository,
    val statsRepository: StatsRepository
)