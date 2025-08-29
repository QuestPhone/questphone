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
import kotlin.random.Random

/**
 * Represents a point in 3D space with an offset for dynamic movement.
 * @param x The x-coordinate.
 * @param y The y-coordinate.
 * @param z The z-coordinate.
 * @param offsetPhase Phase for oscillation to make nodes move independently.
 */
private data class APoint3D(val x: Float, val y: Float, val z: Float, val offsetPhase: Float = Random.nextFloat() * 2f * PI.toFloat())

/**
 * Represents a single node in the neural network.
 * @param basePosition The base 3D position of the node.
 */
private data class ANode(val basePosition: APoint3D)

/**
 * Represents an edge between two nodes.
 * @param node1 Index of the first node.
 * @param node2 Index of the second node.
 */
private data class AEdge(val node1: Int, val node2: Int)

@Composable
fun NeuralMeshAsymmetrical(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        val infiniteTransition = rememberInfiniteTransition(label = "infinite_rotation")

        val onSurface = MaterialTheme.colorScheme.onSurface
        val angleY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * PI.toFloat(),
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

        val nodeMovement by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ), label = "nodeMovement"
        )

        val nodesAndEdges = remember { createAsymmetricalNodesAndEdges() }
        val nodes = nodesAndEdges.first
        val edges = nodesAndEdges.second

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val baseRadius = size.minDimension / 2f

            val transformedNodes = nodes.map { node ->
                val pulsatingRadius = baseRadius * pulse
                // Apply individual node movement
                val offset = sin(node.basePosition.offsetPhase + nodeMovement * 2f * PI.toFloat()) * 0.1f
                val x = node.basePosition.x * (1f + offset)
                val y = node.basePosition.y * (1f + offset)
                var z = node.basePosition.z * (1f + offset)

                // Apply Y-axis rotation
                val rotatedX = x * cos(angleY) + z * sin(angleY)
                val rotatedZ = -x * sin(angleY) + z * cos(angleY)
                val rotatedY = y

                // Apply X-axis rotation
                val finalY = rotatedY * cos(angleX) - rotatedZ * sin(angleX)
                val finalZ = rotatedY * sin(angleX) + rotatedZ * cos(angleX)
                val finalX = rotatedX

                val transformedPoint = APoint3D(finalX * pulsatingRadius, finalY * pulsatingRadius, finalZ * pulsatingRadius)

                val (projectedX, projectedY, scale) = projectPoint(transformedPoint, centerX, centerY)
                Pair(Offset(projectedX, projectedY), scale)
            }

            drawConnections(transformedNodes, edges, onSurface)
            drawNodes(transformedNodes,onSurface)
        }
    }
}

/**
 * Creates an asymmetrical mesh of nodes with full connectivity.
 * Nodes are randomly distributed in a spherical volume, and edges form a fully connected graph.
 * @param numNodes Number of nodes to create.
 * @return Pair of list of nodes and list of edges.
 */
private fun createAsymmetricalNodesAndEdges(numNodes: Int = 50): Pair<List<ANode>, List<AEdge>> {
    val ANodes = mutableListOf<ANode>()
    val AEdges = mutableListOf<AEdge>()
    val random = Random.Default

    // Generate nodes in a spherical volume
    repeat(numNodes) {
        // Random spherical coordinates
        val theta = random.nextFloat() * 2f * PI.toFloat()
        val phi = acos(2f * random.nextFloat() - 1f)
        val r = random.nextFloat().pow(0.5f) // Square root for uniform volume distribution

        val x = r * sin(phi) * cos(theta)
        val y = r * sin(phi) * sin(theta)
        val z = r * cos(phi)
        ANodes.add(ANode(APoint3D(x, y, z)))
    }

    // Create a fully connected graph using a minimum number of edges
    // Connect each node to at least 3 others to ensure no isolated nodes
    val usedNodes = mutableSetOf<Int>()
    val availableNodes = (0 until numNodes).toMutableList()

    // Connect each node to its 3 nearest neighbors
    for (i in 0 until numNodes) {
        val neighbors = (0 until numNodes)
            .filter { it != i }
            .sortedBy { j ->
                val p1 = ANodes[i].basePosition
                val p2 = ANodes[j].basePosition
                sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2) + (p1.z - p2.z).pow(2))
            }
            .take(3)

        neighbors.forEach { j ->
            if (i < j) { // Avoid duplicate edges
                AEdges.add(AEdge(i, j))
            }
        }
        usedNodes.add(i)
    }

    // Ensure all nodes are connected by adding edges to form a single component
    while (availableNodes.isNotEmpty()) {
        val i = availableNodes.removeAt(0)
        if (usedNodes.contains(i)) continue

        // Connect to a random used node
        if (usedNodes.isNotEmpty()) {
            val j = usedNodes.random(random)
            AEdges.add(AEdge(i, j))
            usedNodes.add(i)
        }
    }

    return Pair(ANodes, AEdges)
}

/**
 * Projects a 3D point into 2D space with a perspective effect.
 */
private fun projectPoint(point: APoint3D, centerX: Float, centerY: Float): Triple<Float, Float, Float> {
    val perspective = 300f
    val scale = perspective / (perspective + point.z + 100f) // Adjust for better depth

    val projectedX = point.x * scale + centerX
    val projectedY = point.y * scale + centerY
    return Triple(projectedX, projectedY, scale)
}

/**
 * Draws the nodes on the canvas.
 */
private fun DrawScope.drawNodes(projectedNodes: List<Pair<Offset, Float>>,color: Color) {
    projectedNodes.forEach { (offset, scale) ->
        val radius = 5f * scale

        drawCircle(
            color = color, // Bright cyan for a digital look
            radius = radius,
            center = offset,
        )
    }
}

/**
 * Draws the connections between nodes.
 */
private fun DrawScope.drawConnections(projectedNodes: List<Pair<Offset, Float>>, AEdges: List<AEdge>, color: Color) {
    AEdges.forEach { edge ->
        val (p1, scale1) = projectedNodes[edge.node1]
        val (p2, scale2) = projectedNodes[edge.node2]

        drawLine(
            color = color, // Light blue for connections
            start = p1,
            end = p2,
            strokeWidth = 1.5f * ((scale1 + scale2) / 2f),
            cap = StrokeCap.Round
        )
    }
}

/**
 * Helper function to calculate the distance between two Offsets.
 */
private fun Offset.distanceTo(other: Offset): Float {
    return sqrt((x - other.x).pow(2) + (y - other.y).pow(2))
}