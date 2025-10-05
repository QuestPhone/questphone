package neth.iecal.questphone.app.screens.onboard

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import neth.iecal.questphone.MainActivity
import neth.iecal.questphone.app.screens.account.SetupProfileScreen
import neth.iecal.questphone.app.screens.onboard.subscreens.BlockedAppsView
import neth.iecal.questphone.app.screens.onboard.subscreens.CalculateLifeStats
import neth.iecal.questphone.app.screens.onboard.subscreens.LoginOnboard
import neth.iecal.questphone.app.screens.onboard.subscreens.NotificationPerm
import neth.iecal.questphone.app.screens.onboard.subscreens.OverlayScreenPerm
import neth.iecal.questphone.app.screens.onboard.subscreens.ScheduleExactAlarmPerm
import neth.iecal.questphone.app.screens.onboard.subscreens.SelectApps
import neth.iecal.questphone.app.screens.onboard.subscreens.ShowSocialsScreen
import neth.iecal.questphone.app.screens.onboard.subscreens.ShowTutorial
import neth.iecal.questphone.app.screens.onboard.subscreens.TermsScreen
import neth.iecal.questphone.app.screens.onboard.subscreens.UsageAccessPerm
import neth.iecal.questphone.core.services.AppBlockerService
import neth.iecal.questphone.core.utils.reminder.NotificationScheduler
import nethical.questphone.core.core.utils.managers.checkNotificationPermission
import nethical.questphone.core.core.utils.managers.checkUsagePermission

@Composable
fun OnBoarderView(navController: NavHostController) {

    val viewModel: OnboarderViewModel = hiltViewModel()

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
            content = {
                CalculateLifeStats()
            }, onNextPressed = {
                if(checkUsagePermission(context)){
                    return@CustomPage true
                }
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                context.startActivity(intent)
                return@CustomPage false

            }
        ),
        OnboardingContent.StandardPage(
            "We're here to save you",
            "Do a Quest → Earn growth → Unlock your app"
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
            if(viewModel.getDistractingApps().isEmpty()){
                SelectApps()
            }else{
                BlockedAppsView(viewModel)
            }
        },
        OnboardingContent.CustomPage {
            ShowTutorial()
        },
        OnboardingContent.CustomPage {
            ShowSocialsScreen()
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
