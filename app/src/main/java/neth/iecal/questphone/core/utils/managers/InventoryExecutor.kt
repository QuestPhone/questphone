package neth.iecal.questphone.core.utils.managers

import android.util.Log
import androidx.navigation.NavController
import neth.iecal.questphone.app.navigation.RootRoute
import neth.iecal.questphone.app.screens.onboard.subscreens.SelectAppsModes
import neth.iecal.questphone.data.InventoryExecParams
import nethical.questphone.core.core.utils.getFullTimeAfter
import nethical.questphone.data.game.InventoryItem

fun executeItem(inventoryItem: InventoryItem,execParams: InventoryExecParams){
    when(inventoryItem){
        InventoryItem.XP_BOOSTER -> onUseXpBooster(execParams)
        InventoryItem.DISTRACTION_ADDER -> switchCurrentScreen(execParams.navController,RootRoute.SelectApps.route + SelectAppsModes.ALLOW_ADD.ordinal)
        InventoryItem.DISTRACTION_REMOVER -> switchCurrentScreen(execParams.navController,RootRoute.SelectApps.route + SelectAppsModes.ALLOW_REMOVE.ordinal)
        InventoryItem.REWARD_TIME_EDITOR -> switchCurrentScreen(execParams.navController,RootRoute.SetCoinRewardRatio.route)
        else -> { }
    }
}

fun onUseXpBooster(execParams: InventoryExecParams){
    execParams.userRepository.userInfo.active_boosts.put(InventoryItem.XP_BOOSTER, getFullTimeAfter(5, 0))
    execParams.userRepository.saveUserInfo()
}

fun switchCurrentScreen(navController: NavController, screen: String){
    Log.d("InventoryItem","Switching screen")
    navController.navigate( screen)
}

