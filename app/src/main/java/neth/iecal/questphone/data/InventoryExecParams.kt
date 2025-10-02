package neth.iecal.questphone.data

import androidx.navigation.NavController
import neth.iecal.questphone.backed.repositories.QuestRepository
import neth.iecal.questphone.backed.repositories.StatsRepository
import neth.iecal.questphone.backed.repositories.UserRepository

data class InventoryExecParams(
    val navController: NavController,
    val userRepository: UserRepository,
    val questRepository: QuestRepository,
    val statsRepository: StatsRepository
)