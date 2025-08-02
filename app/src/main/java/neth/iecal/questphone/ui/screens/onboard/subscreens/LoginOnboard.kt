package neth.iecal.questphone.ui.screens.onboard.subscreens

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.collectLatest
import neth.iecal.questphone.ui.screens.account.ForgotPasswordScreen
import neth.iecal.questphone.ui.screens.account.login.AuthStep
import neth.iecal.questphone.ui.screens.account.login.LoginScreen
import neth.iecal.questphone.ui.screens.account.login.LoginViewModel
import neth.iecal.questphone.ui.screens.account.login.SignUpScreen
import neth.iecal.questphone.ui.screens.onboard.StandardPageContent
import nethical.questphone.backend.Supabase
import nethical.questphone.backend.isOnline
import nethical.questphone.backend.triggerProfileSync
import nethical.questphone.backend.triggerQuestSync
import nethical.questphone.backend.triggerStatsSync

@Composable
fun LoginOnboard(isNextEnabled: MutableState<Boolean>, navController: NavHostController){
    val context = LocalContext.current

    val viewModel: LoginViewModel = hiltViewModel()

    val authStep = viewModel.authStep
    LaunchedEffect(Unit) {
        Supabase.supabase.auth.sessionStatus.collectLatest { authState ->
            Log.d("authState",authState.toString())
            when (authState) {
                is SessionStatus.Authenticated -> {
                    authStep.value = AuthStep.COMPLETE
                    isNextEnabled.value = true
                }

                is SessionStatus.NotAuthenticated -> {
                    isNextEnabled.value = false
                }
                is SessionStatus.Initializing -> {
                    Log.d("Signup", "Initializing session...")
                }

                else -> {}
            }
        }
    }

    AnimatedContent(targetState = authStep.value, transitionSpec = {
        (fadeIn(animationSpec = tween(300))
            .togetherWith(fadeOut(animationSpec = tween(300))))
    }) { it ->

        when(it) {
            AuthStep.LOGIN -> {
                LoginScreen(viewModel) {
                    if (context.isOnline()) {
                        triggerQuestSync(context.applicationContext, true)
                        triggerStatsSync(context, true)
                        triggerProfileSync(context)
                    }
                }
            }
            AuthStep.SIGNUP -> {
                SignUpScreen(viewModel, {isNextEnabled.value = true})

            }
            AuthStep.FORGOT_PASSWORD -> ForgotPasswordScreen(viewModel)
            AuthStep.COMPLETE ->
            {
                StandardPageContent("A New Journey Begins Here!", "Press Next to continue!")
            }

        }
    }
}
