package neth.iecal.questphone.ui.screens.quest.view

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressBackground(progress:MutableFloatState = mutableFloatStateOf(0f),loadingAnimationDuration: Int = 3000, questViewBody : @Composable () -> Unit,) {

    val scrollState = rememberScrollState()
    val animatedProgress by animateFloatAsState(
        targetValue = progress.floatValue,
        animationSpec = tween(durationMillis = loadingAnimationDuration, easing = LinearEasing)
    )



    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                color =  Color(0xFFB00023),
                size = Size(size.width, animatedProgress * size.height),
            )
        }
        questViewBody()
    }
}
