package neth.iecal.questphone

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import neth.iecal.questphone.app.navigation.RootRoute
import neth.iecal.questphone.app.screens.account.SetupProfileScreen
import neth.iecal.questphone.app.screens.account.UserInfoScreen
import neth.iecal.questphone.app.screens.etc.DocumentViewerScreen
import neth.iecal.questphone.app.screens.etc.ScreentimeStatsScreen
import neth.iecal.questphone.app.screens.etc.SetCoinRewardRatio
import neth.iecal.questphone.app.screens.game.RewardDialogMaker
import neth.iecal.questphone.app.screens.game.StoreScreen
import neth.iecal.questphone.app.screens.launcher.AppList
import neth.iecal.questphone.app.screens.launcher.AppListViewModel
import neth.iecal.questphone.app.screens.launcher.CustomizeScreen
import neth.iecal.questphone.app.screens.launcher.HomeScreen
import neth.iecal.questphone.app.screens.launcher.HomeScreenViewModel
import neth.iecal.questphone.app.screens.launcher.widget.WidgetScreen
import neth.iecal.questphone.app.screens.onboard.subscreens.SelectApps
import neth.iecal.questphone.app.screens.onboard.subscreens.SelectAppsModes
import neth.iecal.questphone.app.screens.onboard.subscreens.ShowSocialsScreen
import neth.iecal.questphone.app.screens.onboard.subscreens.ShowTutorial
import neth.iecal.questphone.app.screens.pet.TheSystemDialog
import neth.iecal.questphone.app.screens.quest.ListAllQuests
import neth.iecal.questphone.app.screens.quest.ViewQuest
import neth.iecal.questphone.app.screens.quest.setup.SetIntegration
import neth.iecal.questphone.app.screens.quest.stats.specific.BaseQuestStatsView
import neth.iecal.questphone.app.screens.quest.templates.SelectFromTemplates
import neth.iecal.questphone.app.screens.quest.templates.SetupTemplate
import neth.iecal.questphone.app.screens.quest.templates.TemplatesViewModel
import neth.iecal.questphone.app.theme.LauncherTheme
import neth.iecal.questphone.app.theme.customThemes.PitchBlackTheme
import neth.iecal.questphone.backed.isOnline
import neth.iecal.questphone.backed.repositories.QuestRepository
import neth.iecal.questphone.backed.repositories.StatsRepository
import neth.iecal.questphone.backed.repositories.UserRepository
import neth.iecal.questphone.backed.triggerQuestSync
import neth.iecal.questphone.backed.triggerStatsSync
import neth.iecal.questphone.core.Supabase.url
import neth.iecal.questphone.core.services.AppBlockerService
import neth.iecal.questphone.core.utils.FcmHandler
import neth.iecal.questphone.core.utils.receiver.AppInstallReceiver
import neth.iecal.questphone.core.utils.reminder.NotificationScheduler
import neth.iecal.questphone.core.workers.FileDownloadWorker
import neth.iecal.questphone.data.IntegrationId
import nethical.questphone.core.core.utils.fromHex
import java.io.File
import javax.inject.Inject


@AndroidEntryPoint(ComponentActivity::class)
class MainActivity : ComponentActivity() {
    @Inject lateinit var userRepository: UserRepository
    @Inject lateinit var questRepository: QuestRepository
    @Inject lateinit var statRepository: StatsRepository

    private lateinit var appInstallReceiver: AppInstallReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNotificationIntent(intent)
        val questId = intent.getStringExtra("quest_id")
        enableEdgeToEdge()
        val data = getSharedPreferences("onboard", MODE_PRIVATE)
        val notificationScheduler = NotificationScheduler(applicationContext,questRepository)
        val modelSp = getSharedPreferences("models", Context.MODE_PRIVATE)

        val isTokenizerDownloaded = modelSp.getBoolean("is_downloaded_tokenizer",false)
        val tokenizer = File(filesDir, "tokenizer.model")

        if(!isTokenizerDownloaded || !tokenizer.exists()){
            val inputData = Data.Builder()
                .putString(FileDownloadWorker.KEY_URL, "https://huggingface.co/onnx-community/siglip2-base-patch16-224-ONNX/resolve/main/tokenizer.model")
                .putString(FileDownloadWorker.KEY_FILE_NAME, "tokenizer.model")
                .putString(FileDownloadWorker.KEY_MODEL_ID, "tokenizer")
                .build()
            val workRequest = OneTimeWorkRequestBuilder<FileDownloadWorker>()
                .setInputData(inputData)
                .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                .build()

            WorkManager.getInstance(applicationContext).enqueue(workRequest)

        }

        val currentTheme = themes[userRepository.userInfo.customization_info.equippedTheme]!!
        setContent {
            val isUserOnboarded = remember {mutableStateOf(true)}
            var currentTheme = remember { mutableStateOf(currentTheme) }

            LaunchedEffect(Unit) {
                isUserOnboarded.value = data.getBoolean("onboard",false)
                Log.d("onboard", isUserOnboarded.value.toString())

                if(isUserOnboarded.value){
                    startForegroundService(Intent(this@MainActivity, AppBlockerService::class.java))
                }

                notificationScheduler.createNotificationChannel()
                notificationScheduler.reloadAllReminders()
            }
            LauncherTheme(currentTheme.value) {
                Surface {
                    val navController = rememberNavController()

                    val unSyncedQuestItems = remember { questRepository.getUnSyncedQuests() }
                    val unSyncedStatsItems = remember { statRepository.getAllUnSyncedStats() }
                    val context = LocalContext.current

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
                                triggerStatsSync(applicationContext)
                            }
                        }
                    }

                    val appListViewModel : AppListViewModel = hiltViewModel()
                    val homeScreenViewModel : HomeScreenViewModel = hiltViewModel()
                    val templatesViewModel: TemplatesViewModel = hiltViewModel()

                    val scope = rememberCoroutineScope()
                    DisposableEffect(Unit) {
                        val receiver = AppInstallReceiver { packageName ->
                            scope.launch(Dispatchers.IO) {
                                appListViewModel.loadApps()
                            }
                        }

                        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
                            addDataScheme("package")
                        }

                        context.registerReceiver(receiver, filter)

                        onDispose {
                            context.unregisterReceiver(receiver)
                        }
                    }
                    NavHost(
                        navController = navController,
                        startDestination = if(questId!=null) "${RootRoute.ViewQuest.route}${questId}" else RootRoute.HomeScreen.route,
                        popEnterTransition = { fadeIn(animationSpec = tween(700)) },
                        popExitTransition = { fadeOut(animationSpec = tween(700)) },
                    ) {

                        composable(RootRoute.UserInfo.route) {
                            UserInfoScreen(navController = navController)
                        }
                        composable(
                            route = "${RootRoute.SelectApps.route}{mode}",
                            arguments = listOf(navArgument("mode") { type = NavType.IntType })
                        ) { backstack ->
                            val mode = backstack.arguments?.getInt("mode")
                            SelectApps(SelectAppsModes.entries[mode!!])
                        }
                        composable(RootRoute.HomeScreen.route) {
                            HomeScreen(navController,homeScreenViewModel)
                        }
                        composable(RootRoute.WidgetScreen.route) {
                            WidgetScreen(navController)
                        }


                        composable(RootRoute.Store.route) {
                            LauncherTheme(PitchBlackTheme()) {
                                StoreScreen(navController)
                            }
                        }

                        composable(RootRoute.Customize.route) {
                            LauncherTheme(PitchBlackTheme()) {
                                CustomizeScreen(navController, currentTheme = currentTheme)
                            }
                        }
                        composable(RootRoute.AppList.route) {
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

                            ViewQuest(navController, questRepository,id!!)
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
                            SelectFromTemplates(navController,templatesViewModel)
                        }
                        composable(RootRoute.SetupTemplate.route) {
                            SetupTemplate(navController,templatesViewModel)
                        }

                        composable(RootRoute.SetCoinRewardRatio.route){
                            SetCoinRewardRatio()
                        }
                        composable("${RootRoute.IntegrationDocs.route}{name}"){ backStackEntry ->
                            val id = backStackEntry.arguments?.getString("name")
                            val url = IntegrationId.valueOf(id.toString()).docLink
                            DocumentViewerScreen(url)
                        }
                        composable("${RootRoute.DocViewer.route}{url}"){ backStackEntry ->
                            val url = backStackEntry.arguments?.getString("url")
                            DocumentViewerScreen(String.fromHex(url.toString()))
                        }
                        composable(RootRoute.SetupProfile.route) {
                            SetupProfileScreen()
                        }
                        composable(RootRoute.ShowSocials.route) {
                            ShowSocialsScreen()
                        }

                        composable(RootRoute.ShowTutorials.route) {
                            ShowTutorial()
                        }
                        composable(RootRoute.ShowScreentimeStats.route) {
                            ScreentimeStatsScreen()
                        }
                    }

                }

            }
        }
    }

    override fun onResume() {
        handleNotificationIntent(intent)
        super.onResume()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }
    private fun handleNotificationIntent(intent: Intent?) {
        intent?.extras?.let { extras ->
            val data = mutableMapOf<String, String>()
            for (key in extras.keySet()) {
                val value = extras.get(key)?.toString() ?: ""
                data[key] = value
            }

            Log.d("notification Data", data.toString())

            FcmHandler.handleData(this, data,userRepository)
        }
    }


}


