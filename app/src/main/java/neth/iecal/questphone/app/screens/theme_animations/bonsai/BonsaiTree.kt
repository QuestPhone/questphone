package neth.iecal.questphone.app.screens.theme_animations.bonsai

import android.app.Application
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.Serializable
import neth.iecal.questphone.app.screens.theme_animations.bonsai.BonsaiTree.Branch
import neth.iecal.questphone.app.screens.theme_animations.bonsai.BonsaiTree.Leaf
import neth.iecal.questphone.app.screens.theme_animations.bonsai.BonsaiTree.SimpleBranch
import neth.iecal.questphone.backed.repositories.UserRepository
import nethical.questphone.data.json
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Serializable
private data class BonsaiTreeInfo(
    var seed: Long = Random.nextLong(),
    var lastStreak: Int = -1,
)

@HiltViewModel
class BonsaiTreeViewModel
@Inject constructor (application: Application,
                     private val userRepository: UserRepository) : AndroidViewModel(application) {

    var branchList: List<SimpleBranch>? by mutableStateOf(null)
        private set
    var leaves: List<Leaf> = emptyList()
    private var generatedStreak: Int = -1

    fun getSeed(streak: Int): Long {
        val bonsaiInfoJsonRaw = userRepository.userInfo.customization_info.themeData["Bonsai Tree"]
        val bonsaiInfoJson = if (bonsaiInfoJsonRaw != null)
            json.decodeFromString<BonsaiTreeInfo>(bonsaiInfoJsonRaw)
        else {
            BonsaiTreeInfo()
        }
        if (streak == 0) {
            if (bonsaiInfoJson.lastStreak != 0) {
                bonsaiInfoJson.seed = Random.nextLong()
                userRepository.userInfo.customization_info.themeData["Bonsai Tree"] = json.encodeToString(bonsaiInfoJson)
                userRepository.saveUserInfo()
            }
        }
        if (streak != bonsaiInfoJson.lastStreak) {
            bonsaiInfoJson.lastStreak = streak
            userRepository.userInfo.customization_info.themeData["Bonsai Tree"] = json.encodeToString(bonsaiInfoJson)
            userRepository.saveUserInfo()
        }

        return bonsaiInfoJson.seed
    }

    fun generate(width: Float, height: Float, streak: Int = userRepository.userInfo.streak.currentStreak) {
        if (branchList != null && streak == generatedStreak) return

        val rand = Random(getSeed(streak))
        val maxStreak = 15
        val effectiveStreak = if (streak < maxStreak) streak else maxStreak
        val extraLeaves = if (streak > maxStreak) (streak - maxStreak).coerceAtMost(15) else 0

        val root = generateTree(
            rand = rand,
            start = Offset(0f, height ),
            length = height * (0.15f + (effectiveStreak / 50f)),
            angle = -90f,
            depth = 2 + effectiveStreak / 3,
            extraLeaves = extraLeaves,
            width = width,
            height = height // Pass height to the generation function
        )

        val branches = mutableListOf<SimpleBranch>()
        val leavesTemp = mutableListOf<Leaf>()
        flattenTree(root, branches, leavesTemp)

        branchList = branches
        leaves = leavesTemp
        generatedStreak = streak
    }

    private fun flattenTree(b: Branch, list: MutableList<SimpleBranch>, leaves: MutableList<Leaf>) {
        list.add(SimpleBranch(b.start, b.end, b.strokeWidth, b.control))
        leaves.addAll(b.leaves)
        b.children.forEach { flattenTree(it, list, leaves) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BonsaiTree(vm: BonsaiTreeViewModel = hiltViewModel(), innerPadding: PaddingValues) {
    Box(Modifier
        .fillMaxSize()
        .zIndex(-1f)) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .zIndex(-1f)
            .padding(WindowInsets.statusBarsIgnoringVisibility.asPaddingValues())) {
            vm.generate(this.size.width, this.size.height)

            vm.branchList?.forEach { branch ->
                val path = Path().apply {
                    moveTo(branch.start.x, branch.start.y)
                    quadraticBezierTo(
                        branch.control.x, branch.control.y,
                        branch.end.x, branch.end.y
                    )
                }
                drawPath(
                    path = path,
                    brush = branch.brush,
                    style = Stroke(width = branch.strokeWidth)
                )
            }

            vm.leaves.forEach {
                drawCircle(it.color, it.radius, it.center)
            }
        }
    }
}

// ---------------- Tree Generation ----------------
private fun generateTree(rand: Random, start: Offset, length: Float, angle: Float, depth: Int, extraLeaves: Int, width: Float, height: Float): Branch {
    if (depth == 0) return Branch(start, start, 0f, Offset.Zero)

    // Compute end with slight angle variation
    val angleOffset = rand.nextFloat(-10f, 10f)
    val endX = start.x + length * cos(Math.toRadians((angle + angleOffset).toDouble())).toFloat()
    val endY = start.y + length * sin(Math.toRadians((angle + angleOffset).toDouble())).toFloat()

    // Account for stroke width in bounds - keep branches well within canvas
    val margin = 20f // Safe margin for stroke width and bezier curves
    val constrainedEndX = endX.coerceIn(margin, width - margin)
    val constrainedEndY = endY.coerceIn(margin, height - margin)
    val end = Offset(constrainedEndX, constrainedEndY)

    // Compute control point for smooth quadratic curve with safer bounds
    val dx = end.x - start.x
    val dy = end.y - start.y
    val mid = Offset(start.x + dx * 0.5f, start.y + dy * 0.5f)
    val perpLen = rand.nextFloat(-0.3f, 0.3f) * length
    val branchLength = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    val perpX = if (branchLength != 0f) -dy / branchLength * perpLen else 0f
    val perpY = if (branchLength != 0f) dx / branchLength * perpLen else 0f

    // Keep control points well within bounds too
    val controlX = (mid.x + perpX).coerceIn(margin, width - margin)
    val controlY = (mid.y + perpY).coerceIn(margin, height - margin)
    val control = Offset(controlX, controlY)

    // Stroke width tapering
    val strokeWidth = (depth * 2f + rand.nextFloat(-2f, 2f)).coerceAtLeast(2f)

    val children = mutableListOf<Branch>()
    val leaves = mutableListOf<Leaf>()

    if (depth > 1) {
        // Generate child branches
        val branchCount = rand.nextInt(2, 4)
        repeat(branchCount) {
            val angleVariation = rand.nextFloat(-60f, 60f)
            val newAngle = angle + angleVariation + rand.nextFloat(-15f, 15f)
            val newLength = length * rand.nextFloat(0.4f, 0.7f)
            children.add(generateTree(rand, end, newLength, newAngle, depth - 1, extraLeaves, width, height))
        }
    }

    if (depth <= 3) {
        val leafColor = Color(
            red = 0.05f + rand.nextFloat() * 0.1f,
            green = 0.4f + rand.nextFloat() * 0.4f,
            blue = 0.05f + rand.nextFloat() * 0.1f,
            alpha = 1f
        )
        val leafCount = rand.nextInt(8, 15) + if (extraLeaves > 0) rand.nextInt(0, extraLeaves * 2) else 0
        repeat(leafCount) {
            val leafRadius = rand.nextFloat(2f, 6f)
            val offsetX = rand.nextFloat(-15f, 15f)
            val offsetY = rand.nextFloat(-15f, 15f)

            // Account for leaf radius in bounds checking to prevent clipping
            val leafX = (end.x + offsetX).coerceIn(leafRadius, width - leafRadius)
            val leafY = (end.y + offsetY).coerceIn(leafRadius, height - leafRadius)

            leaves.add(
                Leaf(
                    center = Offset(leafX, leafY),
                    radius = leafRadius,
                    color = leafColor.copy(alpha = rand.nextFloat(0.7f, 1f))
                )
            )
        }
    }

    return Branch(start, end, strokeWidth, control, children, leaves)
}


private fun Random.nextFloat(from: Float, until: Float): Float {
    return from + nextFloat() * (until - from)
}

class BonsaiTree {
    data class SimpleBranch(
        val start: Offset,
        val end: Offset,
        val strokeWidth: Float,
        val control: Offset,
        val brush: Brush = Brush.linearGradient(
            listOf(Color(0xFF4A2F1A), Color(0xFF3C2514)) // Earthy browns
        ),
    )

    data class Branch(
        val start: Offset,
        val end: Offset,
        val strokeWidth: Float,
        val control: Offset,
        val children: List<Branch> = emptyList(),
        val leaves: List<Leaf> = emptyList(),
    )

    data class Leaf(
        val center: Offset,
        val radius: Float,
        val color: Color,
    )
}