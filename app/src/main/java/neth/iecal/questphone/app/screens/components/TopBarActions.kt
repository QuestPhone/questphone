package neth.iecal.questphone.app.screens.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import neth.iecal.questphone.R
import neth.iecal.questphone.app.theme.LocalCustomTheme


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBarActions(coins: Int,streak: Int,isCoinsVisible: Boolean = false, isStreakVisible: Boolean = false ) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
    ) {
        if (isCoinsVisible) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = LocalCustomTheme.current.getExtraColorScheme().toolBoxContainer,
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
                    fontWeight = FontWeight.Bold
                )
            }

        }

        if (isStreakVisible) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = LocalCustomTheme.current.getExtraColorScheme().toolBoxContainer,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.streak),
                    contentDescription = "Streak",
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = streak.toString(),
                    fontWeight = FontWeight.Bold
                )

            }
        }
    }
}