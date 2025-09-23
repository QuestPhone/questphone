package neth.iecal.questphone

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import neth.iecal.questphone.app.screens.components.NeuralMeshAsymmetrical
import neth.iecal.questphone.app.screens.components.NeuralMeshSymmetrical
import neth.iecal.questphone.app.screens.quest.stats.components.HeatMapHomeScreenWrapper
import neth.iecal.questphone.app.theme.customThemes.BaseTheme
import neth.iecal.questphone.app.theme.customThemes.BonsaiTheme
import neth.iecal.questphone.app.theme.customThemes.CherryBlossomsTheme
import neth.iecal.questphone.app.theme.customThemes.HackerTheme
import neth.iecal.questphone.app.theme.customThemes.PitchBlackTheme

val themes: Map<String, BaseTheme> = mapOf(
    "Cherry Blossoms" to CherryBlossomsTheme(),
    "Hacker" to HackerTheme(),
    "Pitch Black" to PitchBlackTheme(),
    "Bonsai" to BonsaiTheme()
)

const val HOME_WIDGET_PRICE = 30
var homeWidgets: Map<String, @Composable (Modifier)-> Unit> = mapOf(
    "Heat Map" to { HeatMapHomeScreenWrapper(it) },
    "Neural Mesh Symmetrical" to { NeuralMeshSymmetrical(it) },
    "Neural Mesh ASymmetrical" to { NeuralMeshAsymmetrical(it) },
)