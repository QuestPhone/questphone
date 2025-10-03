package neth.iecal.questphone.app.screens.onboard.subscreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import neth.iecal.questphone.app.screens.components.NeuralMeshAsymmetrical

@Composable
fun UsageAccessPerm() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
    ) {
        NeuralMeshAsymmetrical(Modifier.size(200.dp).padding(bottom = 16.dp))

        Text(
            text = "Your Phone is Eating Your Life.\nWanna Know How Much?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )


        Text(
            text = "Please allow QuestPhone to access app usage data to show you detailed statistics about your screen time usage and help find ways to reduce it. All of this data is processed 100% locally and nothing is sent to our servers.",
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.ExtraLight,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
