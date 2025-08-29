package neth.iecal.questphone.app.theme.data

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class CustomColor(
    /**
     * color of the toolbox displayed on the left side of the home screen.
     */
    val toolBoxContainer: Color,
    val heatMapCells: Color,
    val dialogText: Color = Color.White
)
