package neth.iecal.questphone.app.navigation

/**
 * Main screen navigation
 *
 * @property route
 */
sealed class RootRoute(val route: String) {
    data object HomeScreen : RootRoute("home_screen/")
    data object AppList : RootRoute("app_list/")
    data object WidgetScreen : RootRoute("widgetsList/")

    data object ViewQuest : RootRoute("view_quest/")
    data object AddNewQuest : RootRoute("add_quest/")
    data object ListAllQuest : RootRoute("list_quest/")

    data object OnBoard : RootRoute("onboard/")
    data object ResetPass : RootRoute("reset_pass/")
    data object Store : RootRoute("store/")
    data object Customize : RootRoute("customize/")
    data object UserInfo : RootRoute("userInfo/")
    data object QuestStats : RootRoute("questStats/")

    data object SelectApps : RootRoute("select_apps/")

    data object TermsScreen : RootRoute("terms_screen")
    data object SelectTemplates : RootRoute("templates_screen/")
    data object SetupTemplate : RootRoute("setup_template/")

    data object SetCoinRewardRatio : RootRoute("set_coin_reward_ratio/")

    data object SetIntegration : RootRoute("set_quest_integration/")
    data object IntegrationDocs : RootRoute("tutorial/")
    data object DocViewer : RootRoute("docViewer/")
    data object SetupProfile : RootRoute("setupProfile/")
    data object ShowSocials : RootRoute("showSocials/")
    data object ShowTutorials : RootRoute("showTutorial{/")
    data object ShowScreentimeStats : RootRoute("showScreentime/")
}

