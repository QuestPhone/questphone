package neth.iecal.questphone.ui.screens.onboard

import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.collectLatest
import neth.iecal.questphone.ui.screens.account.ForgotPasswordScreen
import neth.iecal.questphone.ui.screens.account.LoginScreen
import neth.iecal.questphone.ui.screens.account.LoginStep
import neth.iecal.questphone.ui.screens.account.SignUpScreen
import neth.iecal.questphone.core.utils.managers.Supabase
import neth.iecal.questphone.core.utils.isOnline
import neth.iecal.questphone.core.utils.triggerQuestSync
import neth.iecal.questphone.core.utils.triggerStatsSync

@Composable
fun LoginOnboard(isNextEnabled: MutableState<Boolean>, navController: NavHostController){
    val context = LocalContext.current

    val data = context.getSharedPreferences("onboard", MODE_PRIVATE)

    val loginStep = remember { mutableStateOf(LoginStep.SIGNUP) }

//    LaunchedEffect(Unit ) {
//        val isUserLoggedIn = Supabase.supabase.auth.currentUserOrNull() != null
//        isNextEnabled.value = isUserLoggedIn
//        if (isUserLoggedIn) {
//            loginStep.value = LoginStep.COMPLETE
//        }
//    }

    LaunchedEffect(Unit) {
        Supabase.supabase.auth.sessionStatus.collectLatest { authState ->
            Log.d("authState",authState.toString())
            when (authState) {
                is SessionStatus.Authenticated -> {
                    loginStep.value = LoginStep.COMPLETE
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

    when(loginStep.value) {
        LoginStep.LOGIN -> {
            LoginScreen(loginStep) {
                if (context.isOnline()) {
                    triggerQuestSync(context.applicationContext, true)
                    triggerStatsSync(context, true)
//                    triggerProfileSync(context)
                }
            }
        }
        LoginStep.SIGNUP -> {
            SignUpScreen(loginStep)

        }
        LoginStep.FORGOT_PASSWORD -> ForgotPasswordScreen(loginStep)
        LoginStep.COMPLETE ->
        {
            StandardPageContent(isNextEnabled,"A New Journey Begins Here!","Press Next to continue!")
        }

    }
}
