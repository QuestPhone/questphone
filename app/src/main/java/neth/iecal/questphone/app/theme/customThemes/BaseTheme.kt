package neth.iecal.questphone.app.theme.customThemes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import neth.iecal.questphone.app.theme.data.CustomColor

interface BaseTheme  {
    fun getRootColorScheme() : ColorScheme
    fun getExtraColorScheme() : CustomColor
    @Composable fun ThemeObjects(innerPadding: PaddingValues)
    @Composable fun DeepFocusThemeObjects(innerPadding: PaddingValues,progres: Float, uniqueIdentifier:String) {}
    val name: String
    val price: Int
        get() =50
    val description:String
    val expandQuestsText: String
        get() = "View More"

    val docLink: String?
        get() = null

}