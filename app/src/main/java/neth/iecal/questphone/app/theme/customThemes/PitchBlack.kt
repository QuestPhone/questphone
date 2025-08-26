package neth.iecal.questphone.app.theme.customThemes

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import neth.iecal.questphone.app.theme.data.CustomColor

val PitchBlackRoot = darkColorScheme(
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

val PitchBlackExtra = CustomColor(
    toolBoxContainer = Color(0xFF2A2A2A)
)