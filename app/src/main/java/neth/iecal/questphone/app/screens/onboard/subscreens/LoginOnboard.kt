package neth.iecal.questphone.app.screens.onboard.subscreens

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import dev.jeziellago.compose.markdowntext.MarkdownText
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.collectLatest
import neth.iecal.questphone.app.screens.account.ForgotPasswordScreen
import neth.iecal.questphone.app.screens.account.login.AuthStep
import neth.iecal.questphone.app.screens.account.login.LoginScreen
import neth.iecal.questphone.app.screens.account.login.LoginViewModel
import neth.iecal.questphone.app.screens.account.login.SignUpScreen
import neth.iecal.questphone.app.screens.components.NeuralMeshSymmetrical
import neth.iecal.questphone.backed.isOnline
import neth.iecal.questphone.backed.triggerProfileSync
import neth.iecal.questphone.backed.triggerQuestSync
import neth.iecal.questphone.backed.triggerStatsSync
import neth.iecal.questphone.core.Supabase

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
                    triggerProfileSync(context,true)
                    triggerQuestSync(context.applicationContext, true)
                    triggerStatsSync(context, true)
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
                        triggerProfileSync(context,true)
                        triggerQuestSync(context.applicationContext, true)
                        triggerStatsSync(context, true)
                    }
                }
            }
            AuthStep.SIGNUP -> {
                SignUpScreen(viewModel, {isNextEnabled.value = true})

            }
            AuthStep.FORGOT_PASSWORD -> ForgotPasswordScreen(viewModel)
            AuthStep.COMPLETE ->
            {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {


                    NeuralMeshSymmetrical(Modifier.size(200.dp).padding(bottom = 16.dp))

                    Text(
                        text = "A New Journey Begins Here!",
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Black
                    )


                    MarkdownText(
                        markdown = "Press *Next* to continue!",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraLight,
                            textAlign = TextAlign.Center),
                        )

                }
            }

        }
    }
}
