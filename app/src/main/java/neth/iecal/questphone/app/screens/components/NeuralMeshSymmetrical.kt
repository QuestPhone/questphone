package neth.iecal.questphone.app.screens.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Represents a point in 3D space.
 * @param x The x-coordinate.
 * @param y The y-coordinate.
 * @param z The z-coordinate.
 */
private data class SPoint3D(val x: Float, val y: Float, val z: Float)

/**
 * Represents a single node in the neural network.
 * It holds its 3D position.
 * @param position The current 3D position of the node.
 */
private data class SNode(val position: SPoint3D)

@Composable
fun NeuralMeshSymmetrical(modifier: Modifier = Modifier) {
    Box( modifier = modifier) {

        val infiniteTransition = rememberInfiniteTransition(label = "infinite_rotation")
        val onSurface = MaterialTheme.colorScheme.onSurface

        val angleY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(), // 360 degrees in radians
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = "angleY"
        )

        val angleX by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 25000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ), label = "angleX"
        )

        val pulse by infiniteTransition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "pulse"
        )


        val nodes = remember { createSphericalNodes() }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Center of the canvas, for projection center.
            val centerX = size.width / 2f
            val centerY = size.height / 2f

            // The main radius of spherical mesh.
            // It's based on the smaller dimension of the canvas to fit well.
            val baseRadius = size.minDimension / 3f

            val transformedNodes = nodes.map { node ->
                // Apply pulsing effect to the radius
                val pulsatingRadius = baseRadius * pulse

                // Apply Y-axis rotation
                var x = node.position.x * cos(angleY) + node.position.z * sin(angleY)
                var z = -node.position.x * sin(angleY) + node.position.z * cos(angleY)
                var y = node.position.y

                // Apply X-axis rotation
                val tempY = y * cos(angleX) - z * sin(angleX)
                z = y * sin(angleX) + z * cos(angleX)
                y = tempY

                // Create the final transformed 3D point
                val transformedPoint = SPoint3D(x * pulsatingRadius, y * pulsatingRadius, z * pulsatingRadius)

                // Project 3D point to 2D screen space
                val (projectedX, projectedY, scale) = projectPoint(transformedPoint, centerX, centerY)

                // Return a pair of the projected 2D offset and the scale factor for sizing/alpha
                Pair(Offset(projectedX, projectedY), scale)
            }

            // Draw connections (lines) first, so nodes are drawn on top.
            drawConnections(transformedNodes,onSurface)

            // Draw the nodes (circles).
            drawNodes(transformedNodes,onSurface)
        }
    }
}

/**
 * Creates a list of nodes distributed evenly on the surface of a sphere.
 * This uses the "Fibonacci sphere" or "golden spiral" algorithm for an even distribution.
 * @param numNodes The total number of nodes to create.
 * @return A list of Node objects.
 */
private fun createSphericalNodes(numNodes: Int = 100): List<SNode> {
    val SNodes = mutableListOf<SNode>()
    val goldenRatio = (1f + sqrt(5f)) / 2f
    val angleIncrement = PI.toFloat() * 2f * goldenRatio

    for (i in 0 until numNodes) {
        val t = i.toFloat() / numNodes
        val inclination = acos(1f - 2f * t)
        val azimuth = angleIncrement * i

        val x = sin(inclination) * cos(azimuth)
        val y = sin(inclination) * sin(azimuth)
        val z = cos(inclination)
        SNodes.add(SNode(SPoint3D(x, y, z)))
    }
    return SNodes
}

/**
 * Projects a 3D point into 2D space with a perspective effect.
 * @param point The 3D point to project.
 * @param centerX The horizontal center of the screen.
 * @param centerY The vertical center of the screen.
 * @return A Triple containing the projected X, Y coordinates, and a scale factor.
 */
private fun projectPoint(point: SPoint3D, centerX: Float, centerY: Float): Triple<Float, Float, Float> {
    // Perspective is controlled by how much 'z' affects the scale.
    // A larger perspective value means a stronger depth effect.
    val perspective = 300f
    val scale = perspective / (perspective + point.z)

    val projectedX = point.x * scale + centerX
    val projectedY = point.y * scale + centerY

    return Triple(projectedX, projectedY, scale)
}

/**
 * A DrawScope extension function to draw the nodes on the canvas.
 * @param projectedNodes A list of pairs, each containing the 2D offset and scale of a node.
 */
private fun DrawScope.drawNodes(projectedNodes: List<Pair<Offset, Float>>,color: Color) {
    projectedNodes.forEach { (offset, scale) ->
        // The node's size and opacity are based on its depth (scale).
        // Nodes further away (smaller scale) are smaller and more transparent.
        val radius = 4f * scale
        val alpha = (scale - 0.5f).coerceIn(0f, 1f) * 2f // Amplify alpha for better visibility

        drawCircle(
            color = color,
            radius = radius,
            center = offset,
            alpha = alpha
        )
    }
}

/**
 * A DrawScope extension function to draw the connections between nodes.
 * @param projectedNodes A list of pairs, each containing the 2D offset and scale of a node.
 */
private fun DrawScope.drawConnections(projectedNodes: List<Pair<Offset, Float>>,color: Color) {
    // To create a well-connected web, we connect each node to its nearest neighbors.
    val maxConnections = 4

    for (i in projectedNodes.indices) {
        val (p1, scale1) = projectedNodes[i]

        // Find the closest nodes to the current node
        val neighbors = projectedNodes
            .mapIndexedNotNull { j, _ -> if (i == j) null else j }
            .sortedBy { j -> p1.distanceTo(projectedNodes[j].first) }
            .take(maxConnections)

        neighbors.forEach { j ->
            val (p2, scale2) = projectedNodes[j]

            // The line's opacity is based on the average depth of the two connected nodes.
            val alpha = ((scale1 + scale2) / 2f - 0.5f).coerceIn(0f, 1f) * 1.5f

            drawLine(
                color = color,
                start = p1,
                end = p2,
                strokeWidth = 1.2f * ((scale1 + scale2) / 2f), // Thinner lines for distant connections
                alpha = alpha,
                cap = StrokeCap.Round // Smooth line endings
            )
        }
    }
}

/**
 * Helper function to calculate the distance between two Offsets.
 */
private fun Offset.distanceTo(other: Offset): Float {
    return sqrt((x - other.x).pow(2) + (y - other.y).pow(2))
}


