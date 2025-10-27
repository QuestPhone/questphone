package neth.iecal.questphone.app.screens.launcher.dialogs

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import neth.iecal.questphone.BuildConfig

@Composable
fun DonationsDialog(onDismiss: ()-> Unit) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        DialogProperties(dismissOnClickOutside = false)
    ) {
        Surface {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {

                Text(
                    text = "Hi, We need help",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Hi, I'm Nethical, 17, and the creator of QuestPhone" + if(BuildConfig.IS_FDROID) " and Digipaws" else "" +
                            ". I’ve spent countless hours building these apps to help people take control of their screen time and also made it foss. " +
                            "But as a student with limited resources, I can’t do it alone. Your support, big or small. Can keep this dream alive and help me make these tools even better.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .align(Alignment.CenterHorizontally)
                )

                OutlinedButton(
                    onClick = {
                        val url = "https://digipaws.life/donate"
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = url.toUri()
                        }
                        context.startActivity(intent)
                        onDismiss()
                    },
                ) {
                    Text("Donate :)")
                }
                Text("Never Ask Again", modifier = Modifier.clickable {
                    onDismiss()
                })
            }
        }
    }
}