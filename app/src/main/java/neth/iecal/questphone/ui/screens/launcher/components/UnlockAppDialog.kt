package neth.iecal.questphone.ui.screens.launcher.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun UnlockAppDialog(
    coins: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    pkgName : String
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Balance: $coins coins",
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                val appName = try {
                    context.packageManager.getApplicationInfo(pkgName, 0)
                        .loadLabel(context.packageManager).toString()
                } catch (_: Exception) {
                    pkgName
                }
                Text(
                    text = "Open $appName?",
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Are you sure you want to spend 5 coins to use this app for 10 minutes?",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onDismiss) {
                        Text("No")
                    }
                    Button(onClick = onConfirm) {
                        Text("Yes")
                    }
                }
            }
    }
}