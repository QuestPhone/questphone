package neth.iecal.questphone

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import neth.iecal.questphone.core.utils.reminder.NotificationScheduler
import neth.iecal.questphone.data.IntegrationId
import neth.iecal.questphone.ui.navigation.Navigator
import neth.iecal.questphone.ui.navigation.RootRoute
import neth.iecal.questphone.ui.screens.account.UserInfoScreen
import neth.iecal.questphone.ui.screens.game.RewardDialogMaker
import neth.iecal.questphone.ui.screens.game.StoreScreen
import neth.iecal.questphone.ui.screens.launcher.AppList
import neth.iecal.questphone.ui.screens.launcher.AppListViewModel
import neth.iecal.questphone.ui.screens.launcher.HomeScreen
import neth.iecal.questphone.ui.screens.launcher.HomeScreenViewModel
import neth.iecal.questphone.ui.screens.onboard.subscreens.SelectApps
import neth.iecal.questphone.ui.screens.onboard.subscreens.SelectAppsModes
import neth.iecal.questphone.ui.screens.onboard.subscreens.SetCoinRewardRatio
import neth.iecal.questphone.ui.screens.pet.TheSystemDialog
import neth.iecal.questphone.ui.screens.quest.ListAllQuests
import neth.iecal.questphone.ui.screens.quest.ViewQuest
import neth.iecal.questphone.ui.screens.quest.setup.SetIntegration
import neth.iecal.questphone.ui.screens.quest.stats.specific.BaseQuestStatsView
import neth.iecal.questphone.ui.screens.quest.templates.SelectFromTemplates
import neth.iecal.questphone.ui.screens.quest.templates.SetupTemplate
import neth.iecal.questphone.ui.theme.LauncherTheme
import nethical.questphone.backend.isOnline
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.backend.repositories.StatsRepository
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.backend.triggerQuestSync
import nethical.questphone.backend.worker.FileDownloadWorker
import nethical.questphone.core.core.services.AppBlockerService
import java.io.File
import javax.inject.Inject


@AndroidEntryPoint(ComponentActivity::class)
class MainActivity : ComponentActivity() {
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var questRepository: QuestRepository
    @Inject lateinit var statRepository: StatsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        val data = getSharedPreferences("onboard", MODE_PRIVATE)
        val notificationScheduler = NotificationScheduler(applicationContext)
        val modelSp = getSharedPreferences("models", Context.MODE_PRIVATE)

        val isTokenizerDownloaded = modelSp.getBoolean("is_downloaded_tokenizer",false)
        val tokenizer = File(filesDir, "tokenizer.model")

        if(!isTokenizerDownloaded || !tokenizer.exists()){
            val inputData = Data.Builder()
                .putString(FileDownloadWorker.KEY_URL, "https://huggingface.co/onnx-community/siglip2-base-patch16-224-ONNX/resolve/main/tokenizer.model")
                .putString(FileDownloadWorker.KEY_FILE_NAME, "tokenizer.model")
                .putString(FileDownloadWorker.KEY_MODEL_ID, "tokenizer")
                .build()

            val downloadWork = OneTimeWorkRequestBuilder<FileDownloadWorker>()
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(applicationContext).enqueue(downloadWork)
        }

        setContent {
            val isUserOnboarded = remember {mutableStateOf(true)}

            LaunchedEffect(Unit) {
                isUserOnboarded.value = data.getBoolean("onboard",false)
                Log.d("onboard", isUserOnboarded.value.toString())

                if(isUserOnboarded.value){
                    startForegroundService(Intent(this@MainActivity, AppBlockerService::class.java))
                }

                notificationScheduler.createNotificationChannel()
                notificationScheduler.reloadAllReminders()
            }
            LauncherTheme {
                Surface {
                    val navController = rememberNavController()

                    val unSyncedQuestItems = remember { questRepository.getUnSyncedQuests() }
                    val unSyncedStatsItems = remember { statRepository.getAllUnSyncedStats() }
                    val context = LocalContext.current

                    val forceCurrentScreen = remember { derivedStateOf { Navigator.currentScreen } }
                    RewardDialogMaker(userRepository)

                    TheSystemDialog()
                    LaunchedEffect(Unit) {
                        unSyncedQuestItems.collect {
                            notificationScheduler.reloadAllReminders()
                            if (context.isOnline() && !userRepository.userInfo.isAnonymous) {
                                triggerQuestSync(applicationContext)
                            }
                        }
                        unSyncedStatsItems.collect {
                            if (context.isOnline() && !userRepository.userInfo.isAnonymous ) {
                                triggerQuestSync(applicationContext)
                            }
                        }
                    }
                    LaunchedEffect(forceCurrentScreen.value) {
                        Log.d("MainActivity", "triggered screen change")
                        if (forceCurrentScreen.value != null) {
                            navController.navigate(forceCurrentScreen.value!!)
                            Navigator.currentScreen = null
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = RootRoute.HomeScreen.route,
                    ) {

                        composable(RootRoute.UserInfo.route) {
                            UserInfoScreen()
                        }
                        composable(
                            route = "${RootRoute.SelectApps.route}{mode}",
                            arguments = listOf(navArgument("mode") { type = NavType.IntType })
                        ) { backstack ->
                            val mode = backstack.arguments?.getInt("mode")
                            SelectApps(SelectAppsModes.entries[mode!!])
                        }
                        composable(RootRoute.HomeScreen.route) {
                            val homeScreenViewModel : HomeScreenViewModel = hiltViewModel()
                            HomeScreen(navController,homeScreenViewModel)
                        }

                        composable(RootRoute.Store.route) {
                            StoreScreen(navController)
                        }
                        composable(RootRoute.AppList.route) {
                            val appListViewModel : AppListViewModel = hiltViewModel()
                            AppList(navController,appListViewModel)
                        }

                        composable(RootRoute.ListAllQuest.route) {
                            ListAllQuests(navController)
                        }
                        composable(
                            route = "${RootRoute.ViewQuest.route}{id}",
                            arguments = listOf(navArgument("id") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id")

                            ViewQuest(navController, id!!)
                        }

                        navigation(
                            startDestination = RootRoute.SetIntegration.route,
                            route = RootRoute.AddNewQuest.route
                        ) {
                            composable(RootRoute.SetIntegration.route) {
                                SetIntegration(
                                    navController
                                )
                            }
                            IntegrationId.entries.forEach { item ->
                                composable(
                                    route = item.name + "/{id}",
                                    arguments = listOf(navArgument("id") {
                                        type = NavType.StringType
                                    })
                                ) { backstack ->
                                    var id = backstack.arguments?.getString("id")
                                    if (id == "ntg") {
                                        id = null
                                    }
                                    item.setupScreen.invoke(id, navController)
                                }
                            }
                        }
                        composable("${RootRoute.QuestStats.route}{id}") { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id")

                            BaseQuestStatsView(id!!, navController)
                        }
                        composable(RootRoute.SelectTemplates.route) {
                            SelectFromTemplates(navController)
                        }
                        composable(RootRoute.SetCoinRewardRatio.route){
                            SetCoinRewardRatio()
                        }
                        composable("${RootRoute.SetupTemplate.route}{id}") { backStackEntry ->
                            val id = backStackEntry.arguments?.getString("id")
                            SetupTemplate(id!!,navController)
                        }
                    }
                }
            }
        }
    }
}

