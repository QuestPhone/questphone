package neth.iecal.questphone.ui.screens.onboard

import android.Manifest
import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.startForegroundService
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import neth.iecal.questphone.MainActivity
import neth.iecal.questphone.core.utils.reminder.NotificationScheduler
import neth.iecal.questphone.ui.screens.account.SetupProfileScreen
import neth.iecal.questphone.ui.screens.onboard.subscreens.LoginOnboard
import neth.iecal.questphone.ui.screens.onboard.subscreens.NotificationPerm
import neth.iecal.questphone.ui.screens.onboard.subscreens.OverlayScreenPerm
import neth.iecal.questphone.ui.screens.onboard.subscreens.ScheduleExactAlarmPerm
import neth.iecal.questphone.ui.screens.onboard.subscreens.SelectApps
import neth.iecal.questphone.ui.screens.onboard.subscreens.TermsScreen
import neth.iecal.questphone.ui.screens.onboard.subscreens.UsageAccessPerm
import nethical.questphone.core.core.services.AppBlockerService
import nethical.questphone.core.core.utils.managers.checkNotificationPermission
import nethical.questphone.core.core.utils.managers.checkUsagePermission

@Composable
fun OnBoarderView(navController: NavHostController) {

    val viewModel: OnboarderViewModel = viewModel()

    val context = LocalContext.current
    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ ->
        }
    )
    val isTosAccepted = remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        isTosAccepted.value = context.getSharedPreferences("terms", MODE_PRIVATE).getBoolean("isAccepted",false)
    }
    val isNextEnabledLogin = rememberSaveable {mutableStateOf(false)}
    val isNextEnabledSetupProfile = rememberSaveable {mutableStateOf(false)}



    val onboardingPages = mutableListOf(
        OnboardingContent.StandardPage(
            "What Are You Doing?",
            "You’re not relaxing. You’re escaping.\n\nHours lost to apps that drain you.\n\nQuestPhone makes you earn screentime—by doing something real. Walk. Study. Breathe.\n\nEach day, you get less screen… until you don’t need it at all.\n\nStop feeding the machine. Start choosing yourself."
        ),
        OnboardingContent.StandardPage(
            "How it works",
            "Take control of your screen time like never before. Instead of mindless scrolling, you’ll earn your access by completing real-life Quests—like going for a walk, meditating, studying, or anything that helps you grow. Each quest rewards you with Coins and XP: spend 5 Coins to unlock your favorite distracting app for 10 minutes, and level up as you build better habits!\n" +
                    "\n" +
                    "It’s not just about restrictions—it’s a game. Stay motivated by collecting items, leveling up, and watching your progress unfold. QuestPhone makes self-discipline feel like an epic adventure."
        ),
        OnboardingContent.CustomPage(
            isNextEnabled = isNextEnabledLogin){ ->
            LoginOnboard(isNextEnabledLogin, navController)
        },


        OnboardingContent.CustomPage(
            content = {
                OverlayScreenPerm()
            },
            onNextPressed = {
                val isAllowed = Settings.canDrawOverlays(context)
                if(!isAllowed){
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:${context.packageName}".toUri()
                    )
                    context.startActivity(intent)
                    return@CustomPage false
                }
                return@CustomPage true
            }
        ),
        OnboardingContent.CustomPage(
            content = {
                UsageAccessPerm()
            }, onNextPressed = {
                if(checkUsagePermission(context)){
                    return@CustomPage true
                }
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                context.startActivity(intent)
                return@CustomPage false

            }
        ),
        OnboardingContent.CustomPage(
            onNextPressed = {
                if(checkNotificationPermission(context)){
                    return@CustomPage true
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    return@CustomPage false
                }else{
                    return@CustomPage true
                }
            }
        ){
            NotificationPerm()
        },
        OnboardingContent.CustomPage(
            content = {
                ScheduleExactAlarmPerm()
            }, onNextPressed = {
                val notificationScheduler = NotificationScheduler(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!notificationScheduler.alarmManager.canScheduleExactAlarms()) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        context.startActivity(intent)
                        false
                    }else{
                        true
                    }
                }else{
                    true
                }

            }
        ),

        OnboardingContent.CustomPage(isNextEnabled =  isNextEnabledSetupProfile) {
            SetupProfileScreen(isNextEnabledSetupProfile)
        },
        OnboardingContent.CustomPage {
            SelectApps()
        }
    )


    if(isTosAccepted.value) {
        OnBoarderView(
            viewModel,
            onFinishOnboarding = {
                startForegroundService(context, Intent(context, AppBlockerService::class.java))
                val data = context.getSharedPreferences("onboard", MODE_PRIVATE)
                data.edit { putBoolean("onboard", true) }
                val intent = Intent(context, MainActivity::class.java)
                context.startActivity(intent)
                (context as Activity).finish()
            },
            pages = onboardingPages
        )
    } else {
        TermsScreen(isTosAccepted)
    }
}
