package neth.iecal.questphone.app.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import neth.iecal.questphone.R

val defaultTypography = Typography()

val InterFont = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_black, FontWeight.Black),
    Font(R.font.inter_extra_light, FontWeight.ExtraLight),
    Font(R.font.inter_bold, FontWeight.Bold),
)

val JetBrainMono = FontFamily(
    Font (R.font.jetbrains_mono_extra_light, FontWeight.ExtraLight)
)



val customTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = InterFont),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = InterFont),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = InterFont),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = InterFont, fontWeight = FontWeight.Black),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = InterFont),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = InterFont),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = InterFont),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = InterFont),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = InterFont),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = InterFont),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = InterFont),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = InterFont),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = InterFont),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = InterFont),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = InterFont),
)
