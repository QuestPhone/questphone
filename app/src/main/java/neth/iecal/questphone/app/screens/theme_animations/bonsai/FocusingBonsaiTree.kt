package neth.iecal.questphone.app.screens.theme_animations.bonsai

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import neth.iecal.questphone.app.screens.theme_animations.bonsai.BonsaiTree.Branch
import neth.iecal.questphone.app.screens.theme_animations.bonsai.BonsaiTree.Leaf
import neth.iecal.questphone.app.screens.theme_animations.bonsai.BonsaiTree.SimpleBranch
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
class RandomBonsaiTreeViewModel
@Inject constructor (application: Application,
                     private val userRepository: UserRepository) : AndroidViewModel(application) {

    var branchList: List<SimpleBranch>? by mutableStateOf(null)
        private set
    var leaves: List<Leaf> = emptyList()


    // Cache the fully grown tree structure
    private var fullTree: Branch? = null
    private var cachedWidth: Float = 0f
    private var cachedHeight: Float = 0f

    /**
     * Generates/animates the bonsai tree based on progress.
     * @param width Canvas width.
     * @param height Canvas height.
     * @param progress Growth progress from 0.0 (no tree) to 1.0 (fully grown)
     */
    fun generate(width: Float, height: Float, progress: Float,randSeed:String) {
        // Clamp progress to valid range
        val clampedProgress = progress.coerceIn(0f, 1f)

        // Apply easing for smoother animation (ease-out cubic)
        val easedProgress = 1f - (1f - clampedProgress).pow(3)

        // Generate the full tree structure only once or when dimensions change
        if (fullTree == null || cachedWidth != width || cachedHeight != height) {
            cachedWidth = width
            cachedHeight = height

            val rand = Random(stringToLong(randSeed))
            fullTree = generateFullTree(
                rand = rand,
                start = Offset(width / 2f, height),
                length = height * 0.36f,
                angle = -90f,
                depth = 8,
                width = width,
                height = height
            )
        }

        // Scale the tree based on progress
        val branches = mutableListOf<SimpleBranch>()
        val leavesTemp = mutableListOf<Leaf>()
        fullTree?.let { tree ->
            flattenTreeWithProgress(tree, branches, leavesTemp, easedProgress, 0)
        }

        branchList = branches
        leaves = leavesTemp
    }

    private fun flattenTreeWithProgress(
        b: Branch,
        list: MutableList<SimpleBranch>,
        leaves: MutableList<Leaf>,
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

            list.add(SimpleBranch(b.start, animatedEnd, animatedStrokeWidth, animatedControl))

            // Only show leaves when branch is fully grown and we're past 60% progress
            if (branchProgress >= 0.95f && progress > 0.6f) {
                val leafProgress = ((progress - 0.6f) / 0.4f).coerceIn(0f, 1f)
                val leafEasedProgress = leafProgress.pow(2) // Ease-in for leaves

                b.leaves.forEach { leaf ->
                    leaves.add(
                        Leaf(
                            center = leaf.center,
                            radius = leaf.radius * leafEasedProgress,
                            color = leaf.color.copy(alpha = leaf.color.alpha * leafEasedProgress)
                        )
                    )
                }
            }
        }

        // Recurse to children
        b.children.forEach { child ->
            flattenTreeWithProgress(child, list, leaves, progress, currentDepth + 1)
        }
    }

    private fun flattenTree(b: Branch, list: MutableList<SimpleBranch>, leaves: MutableList<Leaf>) {
        if (b.strokeWidth > 0f) {
            list.add(SimpleBranch(b.start, b.end, b.strokeWidth, b.control))
            leaves.addAll(b.leaves)
        }
        b.children.forEach { flattenTree(it, list, leaves) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RandomBonsaiTree(
    progress: Float, // 0.0 to 1.0 for smooth animation
    vm: RandomBonsaiTreeViewModel = hiltViewModel(),
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
            vm.generate(this.size.width, this.size.height, progress,seedKey)

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

            vm.leaves.forEach {
                drawCircle(it.color, it.radius, it.center)
            }
        }
    }
}

// Generate the complete tree structure (called once)
private fun generateFullTree(
    rand: Random,
    start: Offset,
    length: Float,
    angle: Float,
    depth: Int,
    width: Float,
    height: Float
): Branch {
    if (depth <= 0) return Branch(start, start, 0f, Offset.Zero)

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
    val perpLen = rand.nextFloat(-0.3f, 0.3f) * length
    val branchLength = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    val perpX = if (branchLength != 0f) -dy / branchLength * perpLen else 0f
    val perpY = if (branchLength != 0f) dx / branchLength * perpLen else 0f

    val controlX = (mid.x + perpX).coerceIn(margin, width - margin)
    val controlY = (mid.y + perpY).coerceIn(margin, height - margin)
    val control = Offset(controlX, controlY)

    val strokeWidth = (depth * 2f + rand.nextFloat(-2f, 2f)).coerceAtLeast(2f)

    val children = mutableListOf<Branch>()
    val leaves = mutableListOf<Leaf>()

    if (depth > 1) {
        val branchCount = rand.nextInt(2, 4)
        repeat(branchCount) {
            // Bias angles slightly upward for more realistic tree shape (less downward droop)
            val angleVariation = rand.nextFloat(-45f, 60f)
            val newAngle = angle + angleVariation + rand.nextFloat(-15f, 15f)
            val newLength = length * rand.nextFloat(0.4f, 0.7f)
            children.add(generateFullTree(rand, end, newLength, newAngle, depth - 1, width, height))
        }
    }

    if (depth <= 3) {
        val leafColor = Color(
            red = 0.05f + rand.nextFloat() * 0.1f,
            green = 0.4f + rand.nextFloat() * 0.4f,
            blue = 0.05f + rand.nextFloat() * 0.1f,
            alpha = 1f
        )
        val leafCount = rand.nextInt(8, 15)
        repeat(leafCount) {
            val leafRadius = rand.nextFloat(2f, 6f)
            val offsetX = rand.nextFloat(-15f, 15f)
            val offsetY = rand.nextFloat(-15f, 15f)

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
private fun stringToLong(str: String): Long {
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(str.toByteArray())
    return BigInteger(1, digest).toLong()
}