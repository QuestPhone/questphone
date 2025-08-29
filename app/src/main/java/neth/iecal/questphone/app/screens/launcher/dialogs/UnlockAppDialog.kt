package neth.iecal.questphone.app.screens.launcher.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import neth.iecal.questphone.R
import neth.iecal.questphone.app.theme.LocalCustomTheme

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
    val textColor = LocalCustomTheme.current.getExtraColorScheme().dialogText


    Column(
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    color = Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.coin_icon),
                contentDescription = "Coins",
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = coins.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        val appName = try {
            context.packageManager.getApplicationInfo(pkgName, 0)
                .loadLabel(context.packageManager).toString()
        } catch (_: Exception) {
            pkgName
        }

        Text(
            text = "Open $appName?",
            color = textColor,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            modifier = Modifier.padding(bottom = 16.dp, top = 16.dp)
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
                color = textColor,
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
            text = "= ${coinsToSpend / 5 * minutesPerFiveCoins} mins",
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(0.7f),
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
