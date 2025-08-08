package neth.iecal.questphone.app.navigation

/**
 * Main screen navigation
 *
 * @property route
 */
sealed class LauncherDialogRoutes(val route: String) {
    data object UnlockAppDialog: LauncherDialogRoutes("unlock_app/")
    data object ShowAllQuest: LauncherDialogRoutes("all_quests/")
    data object LowCoins: LauncherDialogRoutes("low_coins/")

    // the one where the benefits of performing a quest vs using free pass shown
    data object MakeAChoice: LauncherDialogRoutes("make_a_choice")
    // the one where user can finally use a freepass
    data object FreePassInfo: LauncherDialogRoutes("free_pass_info")

}

