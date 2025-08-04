package neth.iecal.questphone.ui.screens.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import neth.iecal.questphone.ui.screens.game.RewardDialogInfo.coinsEarned
import neth.iecal.questphone.ui.screens.game.RewardDialogInfo.currentDialog
import neth.iecal.questphone.ui.screens.game.RewardDialogInfo.streakCheckReturn
import neth.iecal.questphone.ui.screens.game.dialogs.LevelUpDialog
import neth.iecal.questphone.ui.screens.game.dialogs.QuestCompletionDialog
import neth.iecal.questphone.ui.screens.game.dialogs.StreakFailedDialog
import neth.iecal.questphone.ui.screens.game.dialogs.StreakUpDialog
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.data.game.InventoryItem
import nethical.questphone.data.game.StreakCheckReturn
import nethical.questphone.data.game.xpFromStreak
import nethical.questphone.data.game.xpToRewardForQuest

enum class DialogState { QUEST_COMPLETED, LEVEL_UP, STREAK_UP, STREAK_FAILED, NONE }

/**
 * This values in here must be set to true in order to show the dialog [RewardDialogMaker]
 * from the [neth.iecal.questphone.MainActivity]
 */
object RewardDialogInfo{
    var currentDialog by mutableStateOf<DialogState>(DialogState.NONE)
    var coinsEarned : Int = 0
    var streakCheckReturn : StreakCheckReturn? = null
}

/**
 * Handles both showing the rewards dialog as well as rewarding user with xp, coins and bs.
 */
@Composable
fun RewardDialogMaker(userRepository: UserRepository) {
    // Track current dialog state
    var currentDialog = remember { derivedStateOf { currentDialog } }

    // store the last level so later when user earns xp, we compare it to find if they levelled up
    var oldLevel = remember { userRepository.userInfo.level }
    var levelledUpUserRewards = remember { hashMapOf<InventoryItem, Int>() }
    val xpEarned = remember { mutableIntStateOf(0) }

    fun didUserLevelUp(): Boolean {
        return oldLevel != userRepository.userInfo.level
    }
    LaunchedEffect(currentDialog.value) {
        when (currentDialog.value) {
            DialogState.QUEST_COMPLETED -> {
                val xp = xpToRewardForQuest(userRepository.userInfo.level)
                userRepository.addXp(xp)
                xpEarned.intValue = xp
            }

            DialogState.LEVEL_UP -> {
                levelledUpUserRewards = userRepository.calculateLevelUpInvRewards()
                coinsEarned = userRepository.calculateLevelUpCoinsRewards()
                userRepository.addItemsToInventory(levelledUpUserRewards)
            }

            DialogState.STREAK_UP -> {
                xpEarned.intValue = (streakCheckReturn!!.lastStreak until userRepository.currentStreak).sumOf { day ->
                    val xp = xpFromStreak(day)
                    userRepository.addXp(xp)
                    xp
                }

            }

            DialogState.STREAK_FAILED -> {}
            DialogState.NONE -> {
                streakCheckReturn = null
                coinsEarned = 0
                xpEarned.intValue = 0
                levelledUpUserRewards.clear()
                oldLevel = userRepository.userInfo.level
            }

        }

        userRepository.addCoins(coinsEarned)

    }


    // Show the appropriate dialog based on the current state
    when (currentDialog.value) {
        DialogState.QUEST_COMPLETED -> {
            QuestCompletionDialog(
                coinReward = coinsEarned,
                xpReward = xpEarned.value,
                onDismiss = {
                    // If user leveled up, show level up dialog next, otherwise end
                    RewardDialogInfo.currentDialog = if (didUserLevelUp()) {
                        DialogState.LEVEL_UP
                    } else {
                        DialogState.NONE
                    }
                }
            )
        }

        DialogState.LEVEL_UP -> {
            LevelUpDialog(
                oldLevel = oldLevel,
                lvUpRew = levelledUpUserRewards,
                newLevel = userRepository.userInfo.level,
                onDismiss = {
                    RewardDialogInfo.currentDialog = DialogState.NONE
                }
            )
        }

        DialogState.STREAK_UP -> {
            StreakUpDialog(
                streakCheckReturn = streakCheckReturn!!,
                streakData = userRepository.userInfo.streak,
                xpEarned = xpEarned.intValue
            ) {
                RewardDialogInfo.currentDialog =
                    if (didUserLevelUp()) DialogState.LEVEL_UP else DialogState.NONE
            }
        }

        DialogState.STREAK_FAILED -> {
            StreakFailedDialog(
                streakCheckReturn = streakCheckReturn!!
            ) {
                RewardDialogInfo.currentDialog = DialogState.NONE
            }
        }

        DialogState.NONE -> {}

    }
}

/**
 * Calculates what to reward user as well as trigger the reward dialog to be shown to the user when user
 * completes a quest
 */
fun rewardUserForQuestCompl(commonQuestInfo: CommonQuestInfo){
    coinsEarned =  commonQuestInfo.reward
    currentDialog = DialogState.QUEST_COMPLETED
}

/**
 * rewards user for streak completion and or trigger dialog for streak success/ streak failure
 */
fun rewardUserForStreak(streakReturn: StreakCheckReturn?){
    if(streakReturn!=null){
        streakCheckReturn = streakReturn
        if (streakCheckReturn!!.isOngoing) {
            currentDialog = DialogState.STREAK_UP
        }else{
            currentDialog = DialogState.STREAK_FAILED
        }
    }
}
