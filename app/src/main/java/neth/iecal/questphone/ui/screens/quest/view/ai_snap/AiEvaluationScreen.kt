package neth.iecal.questphone.ui.screens.quest.view.ai_snap

import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import io.github.jan.supabase.auth.auth
import neth.iecal.questphone.R
import neth.iecal.questphone.utils.Supabase
import java.io.File

@Composable
fun AiEvaluationScreen(
    isAiEvaluating: MutableState<Boolean>,
    questId: String,
    onEvaluationComplete: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val photoFile = File(context.getExternalFilesDir(null), "ai_snap_captured_image.jpg")
    var jwtoken = Supabase.supabase.auth.currentAccessTokenOrNull()

    // State variables
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Trigger validation immediately
    LaunchedEffect(Unit) {
        if (!photoFile.exists()) {
            error = "Image file not found at ${photoFile.absolutePath}"
            isLoading = false
            return@LaunchedEffect
        }
//        client.validateTask(photoFile, questId, jwtoken.toString()) { response ->
//            isLoading = false
//            response.fold(
//                onSuccess = { validationResult ->
//                    result = validationResult
//                    onEvaluationComplete(validationResult.isValid)
//                },
//                onFailure = { e ->
//                    error = e.message
//                    onEvaluationComplete(false) // Treat error as failure
//                }
//            )
//        }
    }

    // Show toast for errors
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            error = null
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center

    ) {

        // Image Section
        if (photoFile.exists()) {
            ScanningImageCard(
                photoFile = photoFile,
                isScanning = isLoading,
                modifier = Modifier.padding(16.dp)
            )
        }

        // Status Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            when {
                isLoading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Evaluating...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
//                result != null -> {
//                    Card(
//                        modifier = Modifier.fillMaxWidth(),
//                        colors = CardDefaults.cardColors(
//                            containerColor = if (result!!.isValid)
//                                Color(0xFF4CAF50).copy(alpha = 0.1f)
//                            else
//                                Color(0xFFFF9800).copy(alpha = 0.1f)
//                        )
//                    ) {
//                        Column(
//                            modifier = Modifier
//                                .padding(16.dp)
//                                .fillMaxWidth()
//                        ) {
//                            Text(
//                                text = if (result!!.isValid) "Valid" else "Invalid",
//                                style = MaterialTheme.typography.titleLarge,
//                                color = if (result!!.isValid) Color(0xFF4CAF50) else Color(0xFFFF9800)
//                            )
//                            Spacer(modifier = Modifier.height(4.dp))
//                            Text(
//                                text = result!!.reason,
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
//                            )
//                            Spacer(modifier = Modifier.height(4.dp))
//                            Button(onClick = { isAiEvaluating.value = false }) {
//                                Text(text = if(result!!.isValid) "Close" else "Retake")
//                            }
//                        }
//                    }
//                }
                error != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.baseline_error_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(onClick = { isAiEvaluating.value = false }) {
                                Text(text = "Close")
                            }

                        }
                    }
                }
            }
        }
    }
}
@Composable
fun ScanningImageCard(
    photoFile: File,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val primary = MaterialTheme.colorScheme.primary
    val pulseAnimation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val glowAnimation = infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = modifier
            .size(250.dp)
    ) {
        // Main Image
        Card(
            elevation = CardDefaults.cardElevation(8.dp),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            val painter = rememberAsyncImagePainter(
                model = photoFile,
                contentScale = ContentScale.Crop
            )
            Image(
                painter = painter,
                contentDescription = "Captured Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Radial Pulse Animation Overlay
        if (isScanning) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                val centerX = size.width / 2
                val centerY = size.height / 2
                val maxRadius = size.width * 0.8f

                // Pulsating circle
                drawCircle(
                    color = primary.copy(alpha = 0.3f * (1f - pulseAnimation.value)),
                    radius = maxRadius * pulseAnimation.value,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 4.dp.toPx())
                )

                // Inner glow effect
                drawCircle(
                    color = primary.copy(alpha = glowAnimation.value),
                    radius = maxRadius * 0.3f * pulseAnimation.value,
                    center = Offset(centerX, centerY)
                )

                // Subtle grid lines (sci-fi effect)
                val gridSpacing = 20.dp.toPx()
                var x = 0f
                while (x <= size.width) {
                    drawLine(
                        color = primary.copy(alpha = 0.1f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += gridSpacing
                }
                var y = 0f
                while (y <= size.height) {
                    drawLine(
                        color = primary.copy(alpha = 0.1f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                    y += gridSpacing
                }
            }

            // Pulsating overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        primary.copy(alpha = 0.05f * (1f - pulseAnimation.value))
                    )
            )
        }
    }
}