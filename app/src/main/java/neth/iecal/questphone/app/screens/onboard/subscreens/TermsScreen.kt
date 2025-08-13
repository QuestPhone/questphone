package neth.iecal.questphone.app.screens.onboard.subscreens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import nethical.questphone.core.core.utils.getCurrentDate

@Composable
fun TermsScreen(isTosAccepted: MutableState<Boolean>) {
    val context = LocalContext.current
    val isNextEnabled = remember { mutableStateOf(false) }

    val verticalScroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground ),
                contentDescription = "app icon",
                Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Hello!",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "You're one of the first to try QuestPhone. An app I've been building to help you take back control of your screen time in a fun, gamified way 🚀\n\n" +
                        "Since we're still in beta, you might spot a few rough edges. I'd love your feedback if you do.\n\n" +
                        "Ready to begin your journey toward healthier digital habits? \n ~ Nethical",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
                    .height(300.dp)
                    .verticalScroll(verticalScroll)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW).apply {
                            data = "https://questphone.app/terms".toUri()
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "View Terms and Conditions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .clickable {
                        isNextEnabled.value = !isNextEnabled.value
                    }
                    .padding(horizontal = 4.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Checkbox(
                    checked = isNextEnabled.value,
                    onCheckedChange = { checked ->
                        isNextEnabled.value = checked
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "I agree to the Terms and Conditions",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.87f),
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedButton(
            onClick = {
                isTosAccepted.value = true

                context.getSharedPreferences("terms", Context.MODE_PRIVATE)
                    .edit()
                    .apply {
                        putBoolean("isAccepted", true)
                        putString("date", getCurrentDate())
                        apply()
                    }
                      },
            enabled = isNextEnabled.value,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (isNextEnabled.value)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            ),
            border = BorderStroke(
                width = 1.dp,
                color = if (isNextEnabled.value)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
            )
        ) {
            Text(
                text = "Next",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}