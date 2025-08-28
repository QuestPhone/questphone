package neth.iecal.questphone.app.screens.theme_animations

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SakuraTree(vm: SakuraTreeViewModel = viewModel(),innerPadding: PaddingValues) {
    // Draw once, no animations
    Box(Modifier.padding(innerPadding)) {
        Canvas(modifier = Modifier.fillMaxSize().zIndex(-1f).padding(WindowInsets.statusBarsIgnoringVisibility.asPaddingValues())) {
            vm.generate(size.width, size.height)

            vm.branchList?.forEach { branch ->
                drawLine(
                    brush = branch.brush,
                    start = branch.start,
                    end = branch.end,
                    strokeWidth = branch.strokeWidth
                )
            }

            vm.blossoms.forEach {
                drawCircle(it.color, it.radius, it.center)
            }
        }
    }

}
class SakuraTreeViewModel : ViewModel() {
    var branchList: List<SimpleBranch>? by mutableStateOf(null)
        private set
    var blossoms: List<Blossom> = emptyList()

    fun generate(width: Float, height: Float) {
        if (branchList != null) return

        val root = generateTree(
            start = Offset(width, height), // bottom-right corner
            length = height * 0.35f,
            angle = -120f,                 // grow leftwards
            depth = 12
        )

        val branches = mutableListOf<SimpleBranch>()
        val blossomsTemp = mutableListOf<Blossom>()
        flattenTree(root, branches, blossomsTemp)

        branchList = branches
        blossoms = blossomsTemp
    }

    private fun flattenTree(b: Branch, list: MutableList<SimpleBranch>, bls: MutableList<Blossom>) {
        list.add(SimpleBranch(b.start, b.end, b.strokeWidth))
        bls.addAll(b.blossoms)
        b.children.forEach { flattenTree(it, list, bls) }
    }
}

// ---------------- Tree Generation ----------------
private fun generateTree(start: Offset, length: Float, angle: Float, depth: Int): Branch {
    if (depth == 0) return Branch(start, start, 0f)

    val endX = start.x + length * cos(Math.toRadians(angle.toDouble())).toFloat()
    val endY = start.y + length * sin(Math.toRadians(angle.toDouble())).toFloat()
    val end = Offset(endX, endY)

    val strokeWidth = Random.nextInt(3, 8).toFloat()
    val children = mutableListOf<Branch>()
    val blossoms = mutableListOf<Blossom>()

    if (depth > 3) {
        val angleVariation = Random.nextInt(-45, 45)
        val newAngle1 = angle + angleVariation
        val newAngle2 = angle - angleVariation
        val newLength = length * Random.nextFloat(0.5f, 0.8f)
        children.add(generateTree(end, newLength, newAngle1, depth - 1))
        children.add(generateTree(end, newLength, newAngle2, depth - 1))
    }

    if (depth <= 3 && Random.nextFloat() < 0.8f) {
        val blossomColor = Color(
            red = 0.9f + Random.nextFloat() * 0.1f,
            green = 0.3f + Random.nextFloat() * 0.2f,
            blue = 0.6f + Random.nextFloat() * 0.2f,
            alpha = 1f
        )
        blossoms.add(Blossom(end, Random.nextInt(8, 20).toFloat(), blossomColor))

        repeat(Random.nextInt(3, 7)) {
            val petalX = end.x + Random.nextInt(-50, 50)
            val petalY = end.y + Random.nextInt(-50, 20)
            blossoms.add(
                Blossom(
                    center = Offset(petalX.toFloat(), petalY.toFloat()),
                    radius = Random.nextInt(5, 12).toFloat(),
                    color = blossomColor.copy(alpha = 0.7f)
                )
            )
        }
    }

    return Branch(start, end, strokeWidth, children, blossoms)
}

// ---------------- Data ----------------
private fun Random.nextFloat(from: Float, until: Float): Float {
    return from + nextFloat() * (until - from)
}

data class SimpleBranch(
    val start: Offset,
    val end: Offset,
    val strokeWidth: Float,
    val brush: Brush = Brush.linearGradient(
        listOf(Color(0xFF8B4513), Color(0xFF5C4033))
    )
)

data class Branch(
    val start: Offset,
    val end: Offset,
    val strokeWidth: Float,
    val children: List<Branch> = emptyList(),
    val blossoms: List<Blossom> = emptyList()
)

data class Blossom(
    val center: Offset,
    val radius: Float,
    val color: Color
)
