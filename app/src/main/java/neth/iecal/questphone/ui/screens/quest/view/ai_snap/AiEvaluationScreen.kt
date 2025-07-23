package neth.iecal.questphone.ui.screens.quest.view.ai_snap

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import neth.iecal.questphone.R
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.data.quest.ai.snap.AiSnap
import neth.iecal.questphone.utils.ai.padTokenIds
import neth.iecal.questphone.utils.ai.preprocessBitmapToFloatBuffer
import neth.iecal.questphone.utils.ai.tokenizeText
import neth.iecal.questphone.utils.json
import java.io.File
import java.nio.LongBuffer

private const val MINIMUM_ZERO_SHOT_THRESHOLD = 0.5

enum class EvaluationStep(val message: String, val progress: Float) {
    INITIALIZING("Initializing...", 0.1f),
    LOADING_IMAGE("Loading image...", 0.2f),
    CHECKING_MODEL("Checking model availability...", 0.3f),
    LOADING_MODEL("Loading AI model...", 0.4f),
    PREPROCESSING("Preprocessing image...", 0.6f),
    TOKENIZING("Processing text...", 0.7f),
    EVALUATING("Running AI evaluation...", 0.9f),
    COMPLETED("Evaluation completed", 1.0f)
}

@Composable
fun AiEvaluationScreen(
    isAiEvaluating: MutableState<Boolean>,
    questId: String,
    onEvaluationComplete: () -> Unit
) {
    val context = LocalContext.current
    val photoFile = File(context.getExternalFilesDir(null), "ai_snap_captured_image.jpg")

    // State variables
    var currentStep by remember { mutableStateOf(EvaluationStep.INITIALIZING) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<Pair<String, Float>>>(emptyList()) }
    var isModelDownloaded by remember { mutableStateOf(true) }

    var questTitle by remember{ mutableStateOf(questId)}
    // Trigger validation immediately
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                currentStep = EvaluationStep.INITIALIZING
                val db = QuestDatabaseProvider.getInstance(context).questDao()
                val quest = db.getQuestById(questId)
                val aiQuest = json.decodeFromString<AiSnap>(quest!!.quest_json)

                questTitle = quest.title
                currentStep = EvaluationStep.LOADING_IMAGE

                if (!photoFile.exists()) {
                    error = "Image file not found at ${photoFile.absolutePath}"
                    return@withContext
                }
                val bitmap = getBitmapFromPath(photoFile.absolutePath)

                currentStep = EvaluationStep.CHECKING_MODEL

                val env = OrtEnvironment.getEnvironment()
                val sp = context.getSharedPreferences("models", Context.MODE_PRIVATE)
                val modelId = sp.getString("selected_one_shot_model", null)
                if (modelId == null) {
                    error = "No model selected"
                    return@withContext
                }

                val modelFile = File(context.filesDir, "$modelId.onnx")
                if (!modelFile.exists()) {
                    isModelDownloaded = false
                    val isModelDownloading = sp.contains("downloading")
                    error = if (isModelDownloading) {
                        "Please wait until the model fully downloads"
                    } else {
                        "Please Download a model."
                    }
                    return@withContext
                }

                currentStep = EvaluationStep.LOADING_MODEL

                val opts = SessionOptions()
                opts.addNnapi()
                opts.setExecutionMode(SessionOptions.ExecutionMode.SEQUENTIAL)
                opts.setOptimizationLevel(SessionOptions.OptLevel.ALL_OPT)

                val modelSession = env.createSession(modelFile.absolutePath, opts)

                currentStep = EvaluationStep.PREPROCESSING
                delay(300)

                Log.d("Started Oneshot evaluation", "run")
                val queries = listOf(aiQuest.taskDescription)
                val processedQueries = queries.map { "$it </s>" }

                currentStep = EvaluationStep.TOKENIZING
                delay(200)

                val tokenIdsList = try {
                    tokenizeText(context, processedQueries)
                } catch (e: Exception) {
                    error = "Tokenization failed: ${e.message}"
                    return@withContext
                }

                val imageTensor = OnnxTensor.createTensor(
                    env,
                    preprocessBitmapToFloatBuffer(bitmap!!),
                    longArrayOf(1, 3, 224, 224)
                )

                val maxLength = 64
                val padTokenId = 0
                val paddedTokenIdsList = tokenIdsList.map { padTokenIds(it, maxLength, padTokenId) }

                // Flatten for tensor
                val numLabels = paddedTokenIdsList.size
                val flatTokenIds =
                    paddedTokenIdsList.flatMap { it.asList() }.map { it.toLong() }.toLongArray()

                Log.d("checkpoint", "Text tensors created")
                val textTensor = OnnxTensor.createTensor(
                    env,
                    LongBuffer.wrap(flatTokenIds),
                    longArrayOf(numLabels.toLong(), maxLength.toLong())
                )

                currentStep = EvaluationStep.EVALUATING
                delay(500)

                val inputs: MutableMap<String?, OnnxTensor?> = HashMap<String?, OnnxTensor?>()
                inputs.put("pixel_values", imageTensor) // float32: [1, 3, 224, 224]
                inputs.put("input_ids", textTensor) // int32: [num_labels, max_seq_len]

                val output: OrtSession.Result? = modelSession.run(inputs)

                val logitsTensor = output!!.get(0) as? OnnxTensor
                val logitsArray = logitsTensor!!.floatBuffer?.array()

                fun sigmoid(x: Float): Float = 1f / (1f + kotlin.math.exp(-x))
                val probs =
                    logitsArray!!.map { sigmoid(it) } // each probability corresponds to a label
                val x: MutableList<Pair<String, Float>> = mutableListOf()
                probs.forEachIndexed { i, prob ->
                    Log.d("prob", prob.toString())
                    x.add(Pair(queries[i], prob))
                }
                results = x.sortedByDescending { it.second } // Sort by confidence
                Log.d("results", "$x")

                currentStep = EvaluationStep.COMPLETED
                delay(200)

                if (results[0].second > MINIMUM_ZERO_SHOT_THRESHOLD) {
                    onEvaluationComplete()
                }

            } catch (e: Exception) {
                error = "Evaluation failed: ${e.message}"
                Log.e("AiEvaluation", "Error during evaluation", e)
            }
        }
    }

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

        Text(
            text = questTitle,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary ,
            modifier = Modifier.padding(bottom= 8.dp)

        )

        if (photoFile.exists()) {
            ScanningImageCard(
                photoFile = photoFile,
                isScanning = currentStep != EvaluationStep.COMPLETED && error == null,
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
                currentStep != EvaluationStep.COMPLETED && error == null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Progress bar
                            LinearProgressIndicator(
                                progress = currentStep.progress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Circular progress indicator
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Current step text
                            Text(
                                text = currentStep.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Progress percentage
                            Text(
                                text = "${(currentStep.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                results.isNotEmpty() -> {
                    val isSuccess = results[0].second > MINIMUM_ZERO_SHOT_THRESHOLD
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess)
                                Color(0xFF4CAF50).copy(alpha = 0.1f)
                            else
                                Color(0xFFFF9800).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = if (isSuccess) "Valid" else "Invalid",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isSuccess) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Task Description: ${results[0].first}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Match Rate: ${String.format("%.6f", results[0].second)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { isAiEvaluating.value = false }) {
                                Text(text = if(isSuccess) "Close" else "Retake")
                            }
                        }
                    }
                }
                error != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
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
                            }
                            Spacer(modifier = Modifier.height(8.dp))
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

fun getBitmapFromPath(path: String): Bitmap? {
    return BitmapFactory.decodeFile(path)
}