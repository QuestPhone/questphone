package neth.iecal.questphone.app.screens.theme_animations.cherry_blossoms

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import neth.iecal.questphone.backed.repositories.UserRepository
import java.math.BigInteger
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

@HiltViewModel
class FocusSakuraVm
@Inject constructor (application: Application,
                     private val userRepository: UserRepository) : AndroidViewModel(application) {

    var branchList: List<FsakuraSimpleBranch>? by mutableStateOf(null)
        private set
    var fsakuraBlossoms: List<FsakuraBlossom> = emptyList()

    // Cache the fully grown tree structure
    private var fullTree: FsakuraBranch? = null
    private var cachedWidth: Float = 0f
    private var cachedHeight: Float = 0f

    /**
     * Generates/animates the sakura tree based on progress.
     * @param width Canvas width.
     * @param height Canvas height.
     * @param progress Growth progress from 0.0 (no tree) to 1.0 (fully grown)
     */
    fun generate(width: Float, height: Float, progress: Float, randSeed: String) {
        // Clamp progress to valid range
        val clampedProgress = progress.coerceIn(0f, 1f)

        // Apply easing for smoother animation (ease-out cubic)
        val easedProgress = 1f - (1f - clampedProgress).pow(3)

        // Generate the full tree structure only once or when dimensions change
        if (fullTree == null || cachedWidth != width || cachedHeight != height) {
            cachedWidth = width
            cachedHeight = height

            val rand = Random(stringToLong(randSeed))
            fullTree = generateFsakuraFull(
                rand = rand,
                start = Offset(width, height), // bottom-right corner for sakura style
                length = height * 0.36f,
                angle = -120f,
                depth = 8,
                width = width,
                height = height
            )
        }

        // Scale the tree based on progress
        val branches = mutableListOf<FsakuraSimpleBranch>()
        val blossomsTemp = mutableListOf<FsakuraBlossom>()
        fullTree?.let { tree ->
            flattenTreeWithProgress(tree, branches, blossomsTemp, easedProgress, 0)
        }

        branchList = branches
        fsakuraBlossoms = blossomsTemp
    }

    private fun flattenTreeWithProgress(
        b: FsakuraBranch,
        list: MutableList<FsakuraSimpleBranch>,
        bls: MutableList<FsakuraBlossom>,
        progress: Float,
        currentDepth: Int
    ) {
        // Calculate how much of the tree should be visible at this progress
        // Branches grow from root to tips
        val maxDepth = 8
        val visibleDepth = (maxDepth * progress).toInt()

        if (currentDepth > visibleDepth) {
            return // Don't render branches beyond visible depth
        }

        // Calculate branch progress (how much of this specific branch to show)
        val branchProgress = ((progress * maxDepth) - currentDepth).coerceIn(0f, 1f)

        if (b.strokeWidth > 0f && branchProgress > 0f) {
            // Use de Casteljau's algorithm to get the prefix of the quadratic Bezier curve
            // This ensures the growth follows the exact curve path for realism
            val p = branchProgress
            val omp = 1f - p
            val omp2 = omp * omp
            val pp = p * p

            // Animated control (Q0)
            val animatedControlX = omp * b.start.x + p * b.control.x
            val animatedControlY = omp * b.start.y + p * b.control.y
            val animatedControl = Offset(animatedControlX, animatedControlY)

            // Intermediate Q1
            val q1X = omp * b.control.x + p * b.end.x
            val q1Y = omp * b.control.y + p * b.end.y

            // Animated end (point at t=p on the curve)
            val animatedEndX = omp * animatedControlX + p * q1X
            val animatedEndY = omp * animatedControlY + p * q1Y
            val animatedEnd = Offset(animatedEndX, animatedEndY)

            // Scale stroke width with progress for growing effect
            val animatedStrokeWidth = b.strokeWidth * min(1f, branchProgress * 1.5f)

            list.add(FsakuraSimpleBranch(b.start, animatedEnd, animatedStrokeWidth, animatedControl))

            // Only show blossoms when branch is fully grown and we're past 60% progress
            if (branchProgress >= 0.95f && progress > 0.6f) {
                val blossomProgress = ((progress - 0.6f) / 0.4f).coerceIn(0f, 1f)
                val blossomEasedProgress = blossomProgress.pow(2) // Ease-in for blossoms

                b.fsakuraBlossoms.forEach { blossom ->
                    bls.add(
                        FsakuraBlossom(
                            center = blossom.center,
                            radius = blossom.radius * blossomEasedProgress,
                            color = blossom.color.copy(alpha = blossom.color.alpha * blossomEasedProgress)
                        )
                    )
                }
            }
        }

        // Recurse to children
        b.children.forEach { child ->
            flattenTreeWithProgress(child, list, bls, progress, currentDepth + 1)
        }
    }

    private fun flattenTree(b: FsakuraBranch, list: MutableList<FsakuraSimpleBranch>, bls: MutableList<FsakuraBlossom>) {
        if (b.strokeWidth > 0f) {
            list.add(FsakuraSimpleBranch(b.start, b.end, b.strokeWidth, b.control))
            bls.addAll(b.fsakuraBlossoms)
        }
        b.children.forEach { flattenTree(it, list, bls) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusSakura(
    progress: Float, // 0.0 to 1.0 for smooth animation
    vm: FocusSakuraVm = hiltViewModel(),
    innerPadding: PaddingValues,
    seedKey: String
) {
    Box(Modifier
        .fillMaxSize()
        .zIndex(-1f)) {
        Canvas(modifier = Modifier
            .fillMaxSize()
            .zIndex(-1f)) {

            // Generate/animate the tree based on current progress
            vm.generate(this.size.width, this.size.height, progress, seedKey)

            // Drawing logic
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

            vm.fsakuraBlossoms.forEach {
                drawCircle(it.color, it.radius, it.center)
            }
        }
    }
}

// Generate the complete tree structure (called once)
private fun generateFsakuraFull(
    rand: Random,
    start: Offset,
    length: Float,
    angle: Float,
    depth: Int,
    width: Float,
    height: Float
): FsakuraBranch {
    if (depth <= 0) return FsakuraBranch(start, start, 0f, Offset.Zero)

    val angleOffset = rand.nextFloat(-10f, 10f)
    val endX = start.x + length * cos(Math.toRadians((angle + angleOffset).toDouble())).toFloat()
    val endY = start.y + length * sin(Math.toRadians((angle + angleOffset).toDouble())).toFloat()

    // Removed margin to allow branches to reach screen edges
    val margin = 0f
    val constrainedEndX = endX.coerceIn(margin, width - margin)
    val constrainedEndY = endY.coerceIn(margin, height - margin)
    val end = Offset(constrainedEndX, constrainedEndY)

    val dx = end.x - start.x
    val dy = end.y - start.y
    val mid = Offset(start.x + dx * 0.5f, start.y + dy * 0.5f)
    val perpLen = rand.nextFloat(0.1f, 0.4f) * length  // Bias positive for consistent droop
    val branchLength = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    val perpX = if (branchLength != 0f) -dy / branchLength * perpLen else 0f
    val perpY = if (branchLength != 0f) dx / branchLength * perpLen else 0f

    val controlX = (mid.x + perpX).coerceIn(margin, width - margin)
    val controlY = (mid.y + perpY).coerceIn(margin, height - margin)
    val control = Offset(controlX, controlY)

    val strokeWidth = (depth * 1.5f + rand.nextFloat(-1f, 1f)).coerceAtLeast(2f)

    val children = mutableListOf<FsakuraBranch>()
    val fsakuraBlossoms = mutableListOf<FsakuraBlossom>()

    if (depth > 1) {
        val branchCount = rand.nextInt(2, 5)  // More branches for bushier look
        repeat(branchCount) {
            val angleVariation = rand.nextFloat(-30f, 30f)  // Smaller variation for spreading
            val newAngle = angle + angleVariation + rand.nextFloat(-10f, 10f)
            val newLength = length * rand.nextFloat(0.5f, 0.8f)
            children.add(generateFsakuraFull(rand, end, newLength, newAngle, depth - 1, width, height))
        }
    }

    if (depth <= 3 && rand.nextFloat() < 0.8f) {
        val blossomColor = Color(
            red = 0.9f + rand.nextFloat() * 0.1f,
            green = 0.3f + rand.nextFloat() * 0.2f,
            blue = 0.6f + rand.nextFloat() * 0.2f,
            alpha = 1f
        )
        fsakuraBlossoms.add(FsakuraBlossom(end, rand.nextFloat(8f, 20f), blossomColor))

        val petalCount = rand.nextInt(3, 7)
        repeat(petalCount) {
            val petalX = end.x + rand.nextFloat(-20f, 20f)
            val petalY = end.y + rand.nextFloat(-20f, 20f)
            fsakuraBlossoms.add(
                FsakuraBlossom(
                    center = Offset(petalX, petalY),
                    radius = rand.nextFloat(5f, 12f),
                    color = blossomColor.copy(alpha = 0.7f)
                )
            )
        }
    }

    return FsakuraBranch(start, end, strokeWidth, control, children, fsakuraBlossoms)
}

private fun Random.nextFloat(from: Float, until: Float): Float {
    return from + nextFloat() * (until - from)
}

private fun stringToLong(str: String): Long {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(str.toByteArray())
    return BigInteger(1, digest).toLong()
}

data class FsakuraSimpleBranch(
    val start: Offset,
    val end: Offset,
    val strokeWidth: Float,
    val control: Offset,
    val brush: Brush = Brush.linearGradient(
        listOf(Color(0xFF8B4513), Color(0xFF5C4033))
    ),
)

data class FsakuraBranch(
    val start: Offset,
    val end: Offset,
    val strokeWidth: Float,
    val control: Offset,
    val children: List<FsakuraBranch> = emptyList(),
    val fsakuraBlossoms: List<FsakuraBlossom> = emptyList(),
)

data class FsakuraBlossom(
    val center: Offset,
    val radius: Float,
    val color: Color,
)