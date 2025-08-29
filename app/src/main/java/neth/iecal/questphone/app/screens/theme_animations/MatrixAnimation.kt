package neth.iecal.questphone.app.screens.theme_animations


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.isActive
import kotlin.math.max
import kotlin.random.Random

private data class Columns(
    val x: FloatArray,         // x position per column
    val y: FloatArray,         // head y (px)
    val speed: FloatArray,     // px per second
    val trail: IntArray,       // trail length per column
    val chars: Array<CharArray>// circular buffers of chars per column
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Columns

        if (!x.contentEquals(other.x)) return false
        if (!y.contentEquals(other.y)) return false
        if (!speed.contentEquals(other.speed)) return false
        if (!trail.contentEquals(other.trail)) return false
        if (!chars.contentDeepEquals(other.chars)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.contentHashCode()
        result = 31 * result + y.contentHashCode()
        result = 31 * result + speed.contentHashCode()
        result = 31 * result + trail.contentHashCode()
        result = 31 * result + chars.contentDeepHashCode()
        return result
    }
}

/**
 * High-performance Matrix rain for Jetpack Compose Canvas.
 *
 * Design goals:
 * - Badass hacker aesthetic with bright heads + fading trails.
 * - Glitch bursts + scanline overlay.
 * - Low allocation + minimal recomposition (all state kept in remember + primitive arrays).
 * - No PNG assets; uses Android's fast text drawing.
 */
@Composable
fun MatrixRain(
    headColor: Color = Color(0xFFA8FF60),
    tailColor: Color = Color(0xFF00FF88),
    textSizeDp: Dp = 14.dp,          // Increase for chunkier glyphs
    densityFactor: Float = 1.0f,     // 0.6..1.4. Lower = fewer columns (faster)
    fps: Int = 30,                   // 24-30 is smooth enough and saves battery
    maxTrail: Int = 20,              // per-column trail length
    glitchChance: Float = 0.006f,    // chance per frame to fire a glitch line
    scrambleChance: Float = 0.02f,   // chance per column per frame to scramble a glyph
    seed: Int = 1337,
) {
    val density = LocalDensity.current
    val textSizePx = with(density) { textSizeDp.toPx() }

    // Native Android Paint is faster for tons of short text draws.
    val textPaint = remember(textSizePx) {
        android.graphics.Paint().apply {
            isAntiAlias = false
            color = android.graphics.Color.GREEN
            textSize = textSizePx
            typeface = android.graphics.Typeface.MONOSPACE
        }
    }

    // Precompute glyph set (ASCII + Katakana for vibes)
    val glyphs: CharArray = remember {
        ("0123456789" +
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                "@#$%&_+-*/<>=?" +
                (0x30A0..0x30FF).map { it.toChar() }.joinToString("")
                ).toCharArray()
    }


    var columns by remember { mutableStateOf<Columns?>(null) }

    // Offsets to avoid full-screen invalidation when nothing changed
    var viewW by remember { mutableStateOf(0f) }
    var viewH by remember { mutableStateOf(0f) }

    // Frame clock
    val frameMillis = remember(fps) { max(8, 1000 / max(1, fps)) }
    val rng = remember(seed) { Random(seed) }

    Box(Modifier.fillMaxSize().zIndex(-1f)) {
        Canvas(Modifier.fillMaxSize()) {
            if (viewW != size.width || viewH != size.height) {
                viewW = size.width
                viewH = size.height
                columns = buildColumns(size.width, size.height, textSizePx, densityFactor, maxTrail, glyphs, rng)
            }

            val cols = columns ?: return@Canvas
            // Advance + draw
            advanceAndDraw(
                cols = cols,
                glyphs = glyphs,
                textPaint = textPaint,
                textSize = textSizePx,
                headColor = headColor,
                tailColor = tailColor,
                dt = frameMillis / 1000f,
                width = size.width,
                height = size.height,
                scrambleChance = scrambleChance,
                maxTrail = maxTrail,
                rng = rng,
            )

            // Occasional horizontal glitch line
            if (rng.nextFloat() < glitchChance) {
                val y = rng.nextFloat() * size.height
                val h = textSizePx * (0.5f + rng.nextFloat())
                drawRect(
                    color = tailColor.copy(alpha = 0.12f + rng.nextFloat() * 0.12f),
                    topLeft = Offset(0f, y),
                    size = androidx.compose.ui.geometry.Size(size.width, h)
                )
            }

            // Subtle CRT scanlines overlay
            drawScanlines(textSizePx, color = Color.Black.copy(alpha = 0.35f))
        }

        // Drive a steady frame loop *without* triggering recomposition storms.
        LaunchedEffect(viewW, viewH, fps) {
            if (viewW <= 0f || viewH <= 0f) return@LaunchedEffect
            while (isActive) {
                // Invalidate Canvas by reading a mutable state value (y positions are mutated inside draw)
                // We simply delay; Canvas will redraw during the next frame.
                kotlinx.coroutines.delay(frameMillis.toLong())
            }
        }
    }
}

private fun buildColumns(
    width: Float,
    height: Float,
    cell: Float,
    densityFactor: Float,
    maxTrail: Int,
    glyphs: CharArray,
    rng: Random,
): Columns {
    val colsCount = max(8, (width / (cell * 0.9f) * densityFactor).toInt())
    val x = FloatArray(colsCount)
    val y = FloatArray(colsCount)
    val speed = FloatArray(colsCount)
    val trail = IntArray(colsCount)
    val chars = Array(colsCount) { CharArray(maxTrail) }

    for (i in 0 until colsCount) {
        x[i] = (i + 0.2f * rng.nextFloat()) * (width / colsCount)
        y[i] = rng.nextFloat() * height
        speed[i] = (height * (0.6f + rng.nextFloat() * 1.6f)) // px/sec
        trail[i] = max(6, (maxTrail * (0.5f + rng.nextFloat() * 0.5f)).toInt())
        // Initialize chars
        for (t in 0 until maxTrail) chars[i][t] = glyphs[rng.nextInt(glyphs.size)]
    }

    return Columns(x, y, speed, trail, chars)
}

private fun DrawScope.advanceAndDraw(
    cols: Columns,
    glyphs: CharArray,
    textPaint: android.graphics.Paint,
    textSize: Float,
    headColor: Color,
    tailColor: Color,
    dt: Float,
    width: Float,
    height: Float,
    scrambleChance: Float,
    maxTrail: Int,
    rng: Random,
) {
    // Fill background once (no heavy gradients to keep it cheap)
    drawRect(Color(0xFF050A08))

    val lineH = textSize * 1.1f

    drawIntoCanvas { canvas ->
        val native = canvas.nativeCanvas
        for (i in cols.x.indices) {
            // advance
            var y = cols.y[i] + cols.speed[i] * dt
            if (y - lineH * cols.trail[i] > height + lineH) {
                // restart this column at a random negative offset so heads appear staggered
                y = -rng.nextFloat() * height * 0.3f
                cols.speed[i] = height * (0.6f + rng.nextFloat() * 1.6f)
                cols.trail[i] = max(6, (maxTrail * (0.5f + rng.nextFloat() * 0.5f)).toInt())
            }
            cols.y[i] = y

            val x = cols.x[i]

            // Occasionally scramble one char in the trail for that column
            if (rng.nextFloat() < scrambleChance) {
                val idx = rng.nextInt(cols.trail[i])
                cols.chars[i][idx] = glyphs[rng.nextInt(glyphs.size)]
            }

            // draw trail from head to tail with fading alpha
            val tl = cols.trail[i]
            var yy = y
            for (t in 0 until tl) {
                val alpha = when (t) {
                    0 -> 1f // head
                    1 -> 0.85f
                    2 -> 0.72f
                    else -> max(0.06f, 0.7f - (t / tl.toFloat()) * 0.7f)
                }
                val c = if (t == 0) headColor else tailColor
                textPaint.color = c.copy(alpha = alpha).toArgb()
                // rotate through ring buffer of chars; cheap index
                val ch = cols.chars[i][t]
                native.drawText(ch.toString(), x, yy, textPaint)
                yy -= lineH
                if (yy < -lineH) break
            }

            // ensure head char changes a bit for sparkle
            if (rng.nextFloat() < 0.2f) {
                cols.chars[i][0] = glyphs[rng.nextInt(glyphs.size)]
            }
        }
    }
}

private fun DrawScope.drawScanlines(step: Float, color: Color) {
    val h = size.height
    var y = 0f
    while (y < h) {
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1f
        )
        y += step
    }
}