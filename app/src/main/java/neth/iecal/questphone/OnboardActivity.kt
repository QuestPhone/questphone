package neth.iecal.questphone

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.jan.supabase.auth.handleDeeplinks
import neth.iecal.questphone.ui.navigation.Screen
import neth.iecal.questphone.ui.screens.account.SetupNewPassword
import neth.iecal.questphone.ui.screens.onboard.OnBoardScreen
import neth.iecal.questphone.ui.screens.onboard.TermsScreen
import neth.iecal.questphone.ui.screens.pet.PetDialog
import neth.iecal.questphone.ui.theme.LauncherTheme
import neth.iecal.questphone.utils.Supabase


class OnboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val data = getSharedPreferences("onboard", MODE_PRIVATE)
            val isUserOnboarded = remember {mutableStateOf(true)}
            isUserOnboarded.value = data.getBoolean("onboard",false)
            Log.d("onboard", isUserOnboarded.value.toString())

            if(isUserOnboarded.value) {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }

            val context = LocalContext.current

            val isPetDialogVisible = remember { mutableStateOf(true) }
            val isLoginResetPassword = remember { mutableStateOf(false) }

            val isTosAccepted = remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                val tosp = context.getSharedPreferences("terms", MODE_PRIVATE)
                isTosAccepted.value = tosp.getBoolean("isAccepted",false)
            }

            LaunchedEffect(Unit) {
                Supabase.supabase.handleDeeplinks(intent){
                    if(it.type == "recovery"){
                        isLoginResetPassword.value = true
                    }
                    Log.d("Supabase Deeplink",it.type.toString())
                }
            }

            val startDestination = if (isLoginResetPassword.value) Screen.ResetPass.route
            else if (!isTosAccepted.value) Screen.TermsScreen.route
            else Screen.OnBoard.route

            LauncherTheme {
                Surface {
                    val navController = rememberNavController()

                    PetDialog(
                        petId = "turtie",
                        isPetDialogVisible,
                        navController
                    )

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {

                        composable(Screen.OnBoard.route) {
                            OnBoardScreen(navController)
                        }
                        composable(
                            Screen.ResetPass.route
                        ) {
                            SetupNewPassword(navController)
                        }

                        composable(Screen.TermsScreen.route) {
                            TermsScreen(isTosAccepted)
                        }
                    }
                }
            }
        }
    }
}