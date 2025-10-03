package neth.iecal.questphone.app.screens.onboard.subscreens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import neth.iecal.questphone.R

@Composable
fun ShowSocialsScreen() {
    val context = LocalContext.current
    val verticalScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "app icon",
                Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Support QuestPhone!",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "QuestPhone is community-driven 💜\n\n" +
                        "If you'd like to help us grow, here are some ways:\n\n" +
                        "• Support development with a donation\n" +
                        "• Join our Discord community\n" +
                        "• Connect with us on Telegram" +
                        "• Follow us on instagram and other platforms",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .height(250.dp)
                    .verticalScroll(verticalScroll)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Donate button
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            data = "https://questphone.app/donate".toUri()
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                },

                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Donate 💖",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            data = "https://questphone.app/socials".toUri()
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Follow us on social media",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

        }
    }
}
