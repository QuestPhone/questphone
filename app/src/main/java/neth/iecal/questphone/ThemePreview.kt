package neth.iecal.questphone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import neth.iecal.questphone.app.screens.launcher.HomeScreen
import neth.iecal.questphone.app.screens.launcher.HomeScreenViewModel
import neth.iecal.questphone.app.theme.LauncherTheme

@AndroidEntryPoint(ComponentActivity::class)
class ThemePreview : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val themeId = intent.getStringExtra("themeId") ?: "cherryBlossoms"
        val theme = themes[themeId]!!
        setContent {
            val homeScreenViewModel: HomeScreenViewModel = hiltViewModel()
            LauncherTheme(
                customTheme = theme,
            ) {
                Surface() {
                    Box() {
                        HomeScreen(
                            navController = null,
                            viewModel = homeScreenViewModel
                        )

                    }
                }
            }
        }
    }
}