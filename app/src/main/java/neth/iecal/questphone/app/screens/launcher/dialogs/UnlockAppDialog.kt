package neth.iecal.questphone.app.screens.launcher.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun UnlockAppDialog(
    coins: Int,
    onDismiss: () -> Unit,
    onConfirm: (coinsSpent: Int) -> Unit,
    pkgName: String,
    minutesPerFiveCoins : Int
) {
    val context = LocalContext.current
    val maxSpendableCoins = coins - (coins % 5)
    var coinsToSpend by remember { mutableIntStateOf(5) }



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
            text = "Select coins to spend (in 5s):",
            color = Color.White
        )

        // Coin step selector
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Button(
                onClick = { if (coinsToSpend > 5) coinsToSpend -= 5 },
                enabled = coinsToSpend > 5
            ) {
                Text("-5")
            }

            Text(
                text = "$coinsToSpend",
                color = Color.White,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Button(
                onClick = { if (coinsToSpend + 5 <= maxSpendableCoins) coinsToSpend += 5 },
                enabled = coinsToSpend + 5 <= maxSpendableCoins
            ) {
                Text("+5")
            }
        }

        Text(
            text = "You'll get ${coinsToSpend / 5 * minutesPerFiveCoins} minutes",
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onDismiss) {
                Text("No")
            }
            Button(onClick = { onConfirm(coinsToSpend) }) {
                Text("Yes")
            }
        }
    }
}
