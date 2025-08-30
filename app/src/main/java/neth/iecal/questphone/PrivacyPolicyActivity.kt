package neth.iecal.questphone

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import neth.iecal.questphone.app.theme.LauncherTheme
import neth.iecal.questphone.app.theme.customThemes.PitchBlackTheme

class PrivacyPolicyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val pitchBlackTheme = PitchBlackTheme()
        setContent {
            LauncherTheme(pitchBlackTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        PrivacyPolicyScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun PrivacyPolicyScreen() {
    val context = LocalContext.current

    val privacyPolicyUrl = "https://questphone.github.io/website/terms"

    LaunchedEffect(Unit) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
        context.startActivity(intent)
    }

    Button(onClick = {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
        context.startActivity(intent)
    }) {
        Text("View Privacy Policy")
    }
}