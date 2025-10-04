package neth.iecal.questphone.app.theme.customThemes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import neth.iecal.questphone.app.screens.theme_animations.bonsai.BonsaiTree
import neth.iecal.questphone.app.screens.theme_animations.bonsai.RandomBonsaiTree
import neth.iecal.questphone.app.theme.data.CustomColor

class BonsaiTheme(): BaseTheme {
    override fun getRootColorScheme(): ColorScheme {
        return lightColorScheme(
            primary = Color(0xFF3A7D44),       // Deep bonsai green 🌿
            onPrimary = Color(0xFFEFFAE3),     // Light foliage highlight

            secondary = Color(0xFF6E8B3D),     // Olive leaf tone
            onSecondary = Color(0xFFF4F8EB),   // Pale greenish background

            tertiary = Color(0xFFB8C99D),      // Soft moss green
            onTertiary = Color(0xFF2F3D1F),    // Dark earthy contrast

            background = Color(0xFFF9F9F6),    // Subtle parchment beige (bonsai soil/scroll vibe)
            onBackground = Color(0xFF2A2A2A),  // Neutral dark trunk color

            surface = Color(0xFFE6F2DA),       // Gentle pale green surface
            onSurface = Color(0xFF314029),     // Rich bark-like text
            surfaceVariant = Color(0xFFDDE8CE), // Muted foliage wash

            error = Color(0xFFD96E6E),         // Muted clay red error
            onError = Color.White
        )
    }

    override fun getExtraColorScheme(): CustomColor {
        return CustomColor(
            toolBoxContainer = Color(0xFF2F3D1F).copy(alpha = 0.3f), // Bonsai shadowed green
            heatMapCells = Color(0xFF3A7D44)                         // Deep bonsai green
        )
    }

    @Composable
    override fun ThemeObjects(innerPadding: PaddingValues) {
        BonsaiTree(innerPadding = innerPadding)
    }

    @Composable
    override fun DeepFocusThemeObjects(
        innerPadding: PaddingValues,
        progress: Float,
        uniqueIdentifier:String
    ) {
        RandomBonsaiTree(progress, innerPadding = innerPadding, seedKey = uniqueIdentifier)
    }

    override val name: String
        get() = "Bonsai"
    override val description: String
        get() = "盆栽 – A small tree, a vast universe"
    override val expandQuestsText: String
        get() = "↟↟↟↟↟↟↟"
    override val docLink: String?
        get() = "https://raw.githubusercontent.com/QuestPhone/docs/main/theme/bonsai/bonsai.md"
}
