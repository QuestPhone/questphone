package neth.iecal.questphone.app.theme.customThemes

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import neth.iecal.questphone.app.theme.data.CustomColor
import neth.iecal.questphone.app.theme.data.ThemeView

class HackerTheme(): BaseTheme {
    override fun getRootColorScheme(): ColorScheme {
        return darkColorScheme(
            primary = Color(0xFF00FF9F),       // Neon green 💻
            onPrimary = Color.Black,           // High contrast text

            secondary = Color(0xFF00BFFF),     // Electric cyan
            onSecondary = Color.Black,

            tertiary = Color(0xFF8A2BE2),      // Cyber purple
            onTertiary = Color.White,

            background = Color(0xFF0A0A0A),    // Almost pure black 🕶️
            onBackground = Color(0xFF00FF9F),  // Green glow text

            surface = Color(0xFF111111),       // Slightly lighter black (for cards)
            onSurface = Color(0xFFB0FFDA),     // Soft mint text/icons

            error = Color(0xFFFF0040),         // Hacker red
            onError = Color.Black
        )
    }

    override fun getExtraColorScheme(): CustomColor {
        return CustomColor(
            toolBoxContainer = Color(0xFF2A2A2A)
        )
    }

    override fun getThemeView(): ThemeView? {
        return null
    }

    override val name: String
        get() = "Hacker"

    override val description: String
        get() = "Kimi wa nawa kim"

}