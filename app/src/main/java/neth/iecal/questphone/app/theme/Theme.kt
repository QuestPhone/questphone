package neth.iecal.questphone.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import neth.iecal.questphone.app.theme.customThemes.CherryBlossomsTheme
import neth.iecal.questphone.app.theme.data.CustomColor
import neth.iecal.questphone.app.theme.data.ThemeView

val cherryBlossomsTheme = CherryBlossomsTheme()
val LocalAppColors = staticCompositionLocalOf { cherryBlossomsTheme.getExtraColorScheme() }
val LocalThemeView = staticCompositionLocalOf<ThemeView?> { cherryBlossomsTheme.getThemeView() }

@Composable
fun LauncherTheme(
    customColor: CustomColor,
    themeView: ThemeView,
    darkTheme: Boolean = true,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {

    CompositionLocalProvider(
        LocalAppColors provides customColor,
        LocalThemeView provides themeView
    ) {
        MaterialTheme(
            colorScheme = cherryBlossomsTheme.getRootColorScheme(),
            typography = customTypography,
            content = content
        )
    }
}