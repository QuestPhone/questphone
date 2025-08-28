package neth.iecal.questphone.app.theme.customThemes

import androidx.compose.material3.ColorScheme
import neth.iecal.questphone.app.theme.data.CustomColor
import neth.iecal.questphone.app.theme.data.ThemeView

interface BaseTheme  {
    fun getRootColorScheme() : ColorScheme
    fun getExtraColorScheme() : CustomColor
    fun getThemeView(): ThemeView?
    val name: String
    val price: Int
        get() = 500
    val description:String

}