package neth.iecal.questphone.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlackAndWhiteColorScheme = darkColorScheme(
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


@Composable
fun LauncherTheme(
    darkTheme: Boolean = true,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BlackAndWhiteColorScheme,
        typography = customTypography,
        content = content
    )
}