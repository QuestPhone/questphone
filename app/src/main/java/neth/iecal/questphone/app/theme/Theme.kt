package neth.iecal.questphone.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import neth.iecal.questphone.app.theme.customThemes.CherryBlossomRootColorScheme
import neth.iecal.questphone.app.theme.customThemes.CherryBlossomsExtraColorScheme
import neth.iecal.questphone.app.theme.data.CustomColor

val LocalAppColors = staticCompositionLocalOf { CherryBlossomsExtraColorScheme }

@Composable
fun LauncherTheme(
    customColor: CustomColor,
    darkTheme: Boolean = true,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {

    CompositionLocalProvider(
        LocalAppColors provides customColor
    ) {
        MaterialTheme(
            colorScheme = CherryBlossomRootColorScheme,
            typography = customTypography,
            content = content
        )
    }
}