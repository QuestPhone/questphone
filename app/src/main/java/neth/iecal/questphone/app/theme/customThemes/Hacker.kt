package neth.iecal.questphone.app.theme.customThemes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import neth.iecal.questphone.app.screens.theme_animations.MatrixRain
import neth.iecal.questphone.app.theme.data.CustomColor

class HackerTheme(): BaseTheme {
    override fun getRootColorScheme(): ColorScheme {
        return darkColorScheme(
            primary = Color(0xFF00FF9F),       // Neon green 💻
            onPrimary = Color.Black,           // High contrast text

            secondary = Color(0xFF00BFFF),     // Electric cyan
            onSecondary = Color.Black,

            tertiary = Color(0xFF8A2BE2),      // Cyber purple
            onTertiary = Color.White,

            background = Color(0xFF0A0A0A),    // Almost pure black
            onBackground = Color(0xFF00FF9F),  // Green glow text

            surface = Color(0xFF111111),       // Slightly lighter black (for cards)
            onSurface = Color(0xFFB0FFDA),     // Soft mint text/icons

            error = Color(0xFFFF0040),         // Hacker red
            onError = Color.Black
        )
    }

    override fun getExtraColorScheme(): CustomColor {
        return CustomColor(
            toolBoxContainer = Color(0xFF1A1F1D),
            heatMapCells = Color(0xFF00FF9F),
            dialogText = Color.White

        )
    }

    @Composable
    override fun ThemeObjects(innerPaddingValues: PaddingValues) {
        MatrixRain(
            textSizeDp = 16.dp,
            densityFactor = 0.7f,
            fps = 20,
            maxTrail = 22,
        )
    }

    override val name: String
        get() = "Hacker"

    override val description: String
        get() = "root@nethical:~#"

    override val expandQuestsText: String
        get() = "▓▒░▓▒░▓▒░"
}