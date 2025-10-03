package neth.iecal.questphone.app.screens.launcher.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import neth.iecal.questphone.app.navigation.LauncherDialogRoutes


@Composable
fun LauncherDialog(
    coins: Int = 0,
    onDismiss: () -> Unit,
    pkgName: String = "",
    rootNavController: NavController?,
    minutesPerFiveCoins :Int = 0,
    unlockApp: (Int) -> Unit = {},
    startDestination: String,
    remainingFreePasses :Int= 0,
    onFreePassUsed : ()->Unit = {},
    areHardLockQuestsPresent: Boolean = false
) {
    val navController = rememberNavController()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            composable(LauncherDialogRoutes.ShowAllQuest.route) {
                AllQuestsDialog(rootNavController = rootNavController, onDismiss = onDismiss, showOnlyHardLockedQuests = areHardLockQuestsPresent)
            }
            composable(LauncherDialogRoutes.FreePassInfo.route) {
                FreePassInfo(
                    onShowAllQuests = { navController.navigate(LauncherDialogRoutes.ShowAllQuest.route) },
                    pkgName = pkgName,
                    onDismiss = onDismiss,
                    remainingFreePassesToday = remainingFreePasses,
                    onFreePassUsed = onFreePassUsed,
                )
            }
            composable(LauncherDialogRoutes.MakeAChoice.route) {
                MakeAChoice(
                    onQuestClick = { navController.navigate(LauncherDialogRoutes.ShowAllQuest.route) },
                    onFreePassClick = { navController.navigate(LauncherDialogRoutes.FreePassInfo.route) }
                )
            }
            composable(LauncherDialogRoutes.LowCoins.route) {
                LowCoinsDialog(
                    coins,pkgName,navController
                )
            }
            composable(LauncherDialogRoutes.UnlockAppDialog.route) {
                UnlockAppDialog(coins,onDismiss, unlockApp,pkgName,minutesPerFiveCoins)
            }
        }

    }
}