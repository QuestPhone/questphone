package neth.iecal.questphone.app.theme.customThemes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import neth.iecal.questphone.app.theme.data.CustomColor

class PitchBlackTheme(): BaseTheme{
    override fun getRootColorScheme(): ColorScheme {
        return darkColorScheme(
            primary = Color.White,            // Keep original color
            onPrimary = Color.Black,

            secondary = Color.Gray,
            onSecondary = Color.Black,

            tertiary = Color.LightGray,
            onTertiary = Color.Black,

            background = Color.Black,
            onBackground = Color.White,

            surface = Color.Black,
            onSurface = Color.White,

            error = Color.DarkGray,
            onError = Color.White,
        )
    }

    override fun getExtraColorScheme(): CustomColor {
        return CustomColor(
            toolBoxContainer = Color(0xFF2A2A2A),
            heatMapCells = Color(0xFFFFFFFF),
            dialogText = Color.White
        )
    }

    @Composable
    override fun ThemeObjects(innerPadding: PaddingValues) {
        null
    }

    override val name: String
        get() = "Pitch Black"
    override val description: String
        get() = "Into Nothingness"
    override val expandQuestsText: String
        get() = "✦✦✦✦✦✦✦"
}
