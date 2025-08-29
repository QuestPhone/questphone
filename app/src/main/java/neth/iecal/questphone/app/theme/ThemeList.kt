package neth.iecal.questphone.app.theme

import neth.iecal.questphone.app.theme.customThemes.BaseTheme
import neth.iecal.questphone.app.theme.customThemes.CherryBlossomsTheme
import neth.iecal.questphone.app.theme.customThemes.HackerTheme
import neth.iecal.questphone.app.theme.customThemes.PitchBlackTheme

val themes: Map<String, BaseTheme> = mapOf(
    "CherryBlossoms" to CherryBlossomsTheme(),
    "hacker" to HackerTheme(),
    "pitchBlack" to PitchBlackTheme()
)
