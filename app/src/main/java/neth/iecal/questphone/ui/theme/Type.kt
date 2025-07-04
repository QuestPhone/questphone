package neth.iecal.questphone.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import neth.iecal.questphone.R

// JetBrains Mono Font Family
val JetBrainsMonoFont = FontFamily(
    Font(R.font.jetbrains_regular, FontWeight.Normal)
)
val defaultTypography = Typography()

val customTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = JetBrainsMonoFont),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = JetBrainsMonoFont),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = JetBrainsMonoFont),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = JetBrainsMonoFont),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = JetBrainsMonoFont),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = JetBrainsMonoFont),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = JetBrainsMonoFont),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = JetBrainsMonoFont),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = JetBrainsMonoFont),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = FontFamily.Serif),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = FontFamily.Serif),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = FontFamily.Serif),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = JetBrainsMonoFont),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = JetBrainsMonoFont),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = JetBrainsMonoFont),
)
