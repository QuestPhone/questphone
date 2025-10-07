package neth.iecal.questphone.app.theme.customThemes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import neth.iecal.questphone.app.screens.theme_animations.cherry_blossoms.FocusSakura
import neth.iecal.questphone.app.screens.theme_animations.cherry_blossoms.SakuraTree
import neth.iecal.questphone.app.theme.data.CustomColor

class CherryBlossomsTheme(): BaseTheme {
    override fun getRootColorScheme(): ColorScheme {
        return lightColorScheme(
            primary = Color(0xFFFFB7C5),       // Sakura pink
            onPrimary = Color(0xFF4A0E1F),     // Deep berry text for contrast

            secondary = Color(0xFFFFD9E8),     // Petal blush
            onSecondary = Color(0xFF5A1A33),   // Muted rosewood

            tertiary = Color(0xFFFFEEF3),      // Soft blossom white-pink
            onTertiary = Color(0xFF633F4C),    // Gentle plum for text

            background = Color(0xFFFFF9FB),    // Very pale blossom white 🌸
            onBackground = Color(0xFF3D1F2D),  // Dark sakura branch tone

            surface = Color(0xFFFFCFE1),       // Cherry blossom surface pink 🌸✨
            onSurface = Color(0xFF40222D),     // Warm brownish text
            surfaceVariant = Color.White,

            error = Color(0xFFE57373),         // Soft red-pink error
            onError = Color.White
        )
    }

    override fun getExtraColorScheme(): CustomColor {
        return CustomColor(
            toolBoxContainer = Color.White.copy(alpha = 0.3f),
            heatMapCells = Color(0xFF4A0E1F),
            dialogText = Color.White
        )
    }

    @Composable
    override fun ThemeObjects(innerPadding: PaddingValues) {
        SakuraTree(innerPadding = innerPadding)
    }

    @Composable
    override fun DeepFocusThemeObjects(
        innerPadding: PaddingValues,
        progres: Float,
        uniqueIdentifier: String
    ) {
        FocusSakura(progres,innerPadding = innerPadding, seedKey = uniqueIdentifier)
    }

    override val name: String
        get() = "Cherry Blossoms"
    override val description: String
        get() = "花は桜木、人は武士"
    override val expandQuestsText: String
        get() = "✿✿✿✿✿✿✿"
    override val docLink: String?
        get() = "https://raw.githubusercontent.com/QuestPhone/docs/main/theme/cherryBlossom/cherryblossom.md"
}