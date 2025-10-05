package neth.iecal.questphone.app.screens.game

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import neth.iecal.questphone.app.screens.game.RewardDialogInfo.coinsEarned
import neth.iecal.questphone.app.screens.game.RewardDialogInfo.currentDialog
import neth.iecal.questphone.app.screens.game.RewardDialogInfo.streakFreezerReturn
import neth.iecal.questphone.app.screens.game.dialogs.LevelUpDialog
import neth.iecal.questphone.app.screens.game.dialogs.QuestCompletionDialog
import neth.iecal.questphone.app.screens.game.dialogs.QuickRewardDialog
import neth.iecal.questphone.app.screens.game.dialogs.StreakFailedDialog
import neth.iecal.questphone.app.screens.game.dialogs.StreakFreezersUsedDialog
import neth.iecal.questphone.app.screens.game.dialogs.StreakUpDialog
import neth.iecal.questphone.backed.repositories.UserRepository
import neth.iecal.questphone.data.CommonQuestInfo
import nethical.questphone.data.game.InventoryItem
import nethical.questphone.data.game.StreakFreezerReturn
import nethical.questphone.data.game.xpFromStreak
import nethical.questphone.data.xpToRewardForQuest

enum class DialogState { QUEST_COMPLETED,QUICK_QUEST_REWARD, LEVEL_UP, STREAK_UP,STREAK_FREEZER_USED, STREAK_FAILED, NONE }

/**
 * This values in here must be set to true in order to show the dialog [RewardDialogMaker]
 * from the [neth.iecal.questphone.MainActivity]
 */
object RewardDialogInfo{
    var currentDialog by mutableStateOf<DialogState>(DialogState.NONE)
    var coinsEarned by mutableIntStateOf(0)
    var streakFreezerReturn : StreakFreezerReturn? = null
}

/**
 * Handles both showing the rewards dialog as well as rewarding user with xp, coins and bs.
 */
@SuppressLint("MutableCollectionMutableState")
@Composable
fun RewardDialogMaker(userRepository: UserRepository) {
    // Track current dialog state
    val currentDialog = currentDialog

    // store the last level so later when user earns xp, we compare it to find if they levelled up
    var oldLevel by remember { mutableIntStateOf( userRepository.userInfo.level) }
    var levelledUpUserRewards by remember { mutableStateOf( hashMapOf<InventoryItem, Int>()) }
    val xpEarned = remember { mutableIntStateOf(0) }

    fun didUserLevelUp(): Boolean {
        Log.d("LevelUp","old level $oldLevel, new Level ${userRepository.userInfo.level}")
        return oldLevel != userRepository.userInfo.level
    }
    LaunchedEffect(currentDialog) {
        when (currentDialog) {
            DialogState.QUEST_COMPLETED -> {
                var xp = xpToRewardForQuest(userRepository.userInfo.level)
                userRepository.addXp(xp)
                xpEarned.intValue = xp
            }
            DialogState.QUICK_QUEST_REWARD -> {}

            DialogState.LEVEL_UP -> {
                Log.d("Level Up","User levelled up")
                oldLevel = userRepository.userInfo.level
                levelledUpUserRewards = userRepository.calculateLevelUpInvRewards()
                coinsEarned = userRepository.calculateLevelUpCoinsRewards()
                userRepository.addItemsToInventory(levelledUpUserRewards)
            }

            DialogState.STREAK_FREEZER_USED -> {
                xpEarned.intValue = (streakFreezerReturn!!.lastStreak until userRepository.currentStreakState.value).sumOf { day ->
                    val xp = xpFromStreak(day)
                    userRepository.addXp(xp)
                    xp
                }
            }
            DialogState.STREAK_UP -> {
                xpEarned.intValue = xpFromStreak(userRepository.currentStreakState.value)
                userRepository.addXp(xpEarned.intValue)

            }
            DialogState.STREAK_FAILED -> {}
            DialogState.NONE -> {
                streakFreezerReturn = null
                coinsEarned = 0
                xpEarned.intValue = 0
                levelledUpUserRewards.clear()
                oldLevel = userRepository.userInfo.level
            }

        }

        userRepository.addCoins(coinsEarned)

    }


    // Show the appropriate dialog based on the current state
    when (currentDialog) {
        DialogState.QUEST_COMPLETED -> {
            QuestCompletionDialog(
                coinReward = coinsEarned,
                xpReward = xpEarned.intValue,
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
                coinReward = coinsEarned,
                lvUpRew = levelledUpUserRewards,
                newLevel = userRepository.userInfo.level,
                onDismiss = {
                    RewardDialogInfo.currentDialog = DialogState.NONE
                }
            )
        }

        DialogState.STREAK_UP -> {
            StreakUpDialog(
                streakData = userRepository.userInfo.streak,
                xpEarned = xpEarned.intValue
            ) {
                RewardDialogInfo.currentDialog =
                    if (didUserLevelUp()) DialogState.LEVEL_UP else DialogState.NONE
            }
        }
        DialogState.STREAK_FREEZER_USED -> {
            StreakFreezersUsedDialog(streakFreezerReturn!!.streakFreezersUsed!!,userRepository.userInfo.streak,xpEarned.intValue) {
                RewardDialogInfo.currentDialog = DialogState.NONE
            }
        }

        DialogState.STREAK_FAILED -> {
            StreakFailedDialog(
                streakFreezerReturn = streakFreezerReturn!!
            ) {
                RewardDialogInfo.currentDialog = DialogState.NONE
            }
        }

        DialogState.NONE -> {}
        DialogState.QUICK_QUEST_REWARD -> {

            QuickRewardDialog(
                coinReward = coinsEarned,
                onDismiss = {
                    RewardDialogInfo.currentDialog = DialogState.NONE
                }
            )
        }
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
fun quickRewardUser(coins:Int){
    coinsEarned =  coins
    currentDialog = DialogState.QUICK_QUEST_REWARD
}


fun handleStreakFreezers(streakReturn: StreakFreezerReturn?){
    if(streakReturn!=null){
        streakFreezerReturn = streakReturn
        currentDialog = if (streakFreezerReturn!!.isOngoing) {
            DialogState.STREAK_FREEZER_USED
        }else{
            DialogState.STREAK_FAILED
        }
    }
}

fun showStreakUpDialog(){
    currentDialog = DialogState.STREAK_UP
}