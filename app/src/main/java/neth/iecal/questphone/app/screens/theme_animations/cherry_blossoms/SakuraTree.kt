package neth.iecal.questphone.app.screens.theme_animations.cherry_blossoms

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
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.serialization.Serializable
import neth.iecal.questphone.backed.repositories.UserRepository
import nethical.questphone.data.json
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Serializable
private data class SakuraTreeInfo(
    var seed: Long = Random.nextLong(),
    var lastStreak: Int = -1,
)
@HiltViewModel
class SakuraTreeViewModel
@Inject constructor (application: Application,
                     private val userRepository: UserRepository) : AndroidViewModel(application) {

    var branchList: List<SimpleBranch>? by mutableStateOf(null)
        private set
    var blossoms: List<Blossom> = emptyList()
    private var generatedStreak: Int = -1

    fun getSeed(streak: Int): Long {
        val sakuraInfoJsonRaw = userRepository.userInfo.customization_info.themeData["Sakura Tree"]
        val sakuraInfoJson = if(sakuraInfoJsonRaw != null)
            json.decodeFromString<SakuraTreeInfo>(sakuraInfoJsonRaw)
        else
            SakuraTreeInfo()
        if(streak==0) {
            if (sakuraInfoJson.lastStreak != 0) {
                sakuraInfoJson.seed = Random.nextLong()
                userRepository.userInfo.customization_info.themeData["Sakura Tree"] = json.encodeToString(sakuraInfoJson)
                userRepository.saveUserInfo()
            }
        }
        if(streak!=sakuraInfoJson.lastStreak){
            sakuraInfoJson.lastStreak = streak
            userRepository.userInfo.customization_info.themeData["Sakura Tree"] = json.encodeToString(sakuraInfoJson)
            userRepository.saveUserInfo()
        }

        return sakuraInfoJson.seed
    }

    fun generate(width: Float, height: Float, streak: Int = userRepository.userInfo.streak.currentStreak) {
        if (branchList != null && streak == generatedStreak) return

        val rand = Random(getSeed(streak)) // Fixed seed for consistent tree structure
        val maxStreak = 10 // Cap significant growth at streak 30
        val effectiveStreak = if(streak<maxStreak) streak else maxStreak
        val extraBlossoms = if (streak > maxStreak) (streak - maxStreak).coerceAtMost(10) else 0

        val root = generateTree(
            rand = rand,
            start = Offset(width, height), // bottom-right corner
            length = height * (0.2f + (effectiveStreak / 100f)),
            angle = -120f, // Slight angle tweak for higher streaks
            depth = 6 + effectiveStreak / 2,
            extraBlossoms = extraBlossoms
        )

        val branches = mutableListOf<SimpleBranch>()
        val blossomsTemp = mutableListOf<Blossom>()
        flattenTree(root, branches, blossomsTemp)

        branchList = branches
        blossoms = blossomsTemp
        generatedStreak = streak
    }

    private fun flattenTree(b: Branch, list: MutableList<SimpleBranch>, bls: MutableList<Blossom>) {
        list.add(SimpleBranch(b.start, b.end, b.strokeWidth))
        bls.addAll(b.blossoms)
        b.children.forEach { flattenTree(it, list, bls) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SakuraTree(vm: SakuraTreeViewModel = hiltViewModel(), innerPadding: PaddingValues,) {
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

// ---------------- Tree Generation ----------------
private fun generateTree(rand: Random, start: Offset, length: Float, angle: Float, depth: Int, extraBlossoms: Int): Branch {
    if (depth == 0) return Branch(start, start, 0f)

    val endX = start.x + length * cos(Math.toRadians(angle.toDouble())).toFloat()
    val endY = start.y + length * sin(Math.toRadians(angle.toDouble())).toFloat()
    val end = Offset(endX, endY)

    val strokeWidth = rand.nextInt(3, 8).toFloat()
    val children = mutableListOf<Branch>()
    val blossoms = mutableListOf<Blossom>()

    if (depth > 3) {
        val angleVariation = rand.nextInt(-45, 45)
        val newAngle1 = angle + angleVariation
        val newAngle2 = angle - angleVariation
        val newLength = length * rand.nextFloat(0.5f, 0.8f)
        children.add(generateTree(rand, end, newLength, newAngle1, depth - 1, extraBlossoms))
        children.add(generateTree(rand, end, newLength, newAngle2, depth - 1, extraBlossoms))
    }

    if (depth <= 3 && rand.nextFloat() < 0.8f) {
        val blossomColor = Color(
            red = 0.9f + rand.nextFloat() * 0.1f,
            green = 0.3f + rand.nextFloat() * 0.2f,
            blue = 0.6f + rand.nextFloat() * 0.2f,
            alpha = 1f
        )
        blossoms.add(Blossom(end, rand.nextInt(8, 20).toFloat(), blossomColor))

        val petalCount = rand.nextInt(3, 7) + if (extraBlossoms > 0) rand.nextInt(0, extraBlossoms) else 0
        repeat(petalCount) {
            val petalX = end.x + rand.nextInt(-50, 50)
            val petalY = end.y + rand.nextInt(-50, 20)
            blossoms.add(
                Blossom(
                    center = Offset(petalX.toFloat(), petalY.toFloat()),
                    radius = rand.nextInt(5, 12).toFloat(),
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
    ),
)

data class Branch(
    val start: Offset,
    val end: Offset,
    val strokeWidth: Float,
    val children: List<Branch> = emptyList(),
    val blossoms: List<Blossom> = emptyList(),
)

data class Blossom(
    val center: Offset,
    val radius: Float,
    val color: Color,
)
