package neth.iecal.questphone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import neth.iecal.questphone.app.screens.etc.DocumentViewerScreen
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
            var isDocScreenVisible by remember { mutableStateOf(false) }

            BackHandler(isDocScreenVisible) {
                isDocScreenVisible = false
            }
            LauncherTheme(
                customTheme = theme,
            ) {
                Surface{
                    Box(
                        modifier = Modifier.clickable {
                            isDocScreenVisible = true
                        }
                    ) {
                        if (!isDocScreenVisible) {
                            HomeScreen(
                                navController = null,
                                viewModel = homeScreenViewModel
                            )
                        }else{
                            theme.docLink?.let { DocumentViewerScreen(it) }
                        }
                    }
                }
            }
        }
    }
}