package neth.iecal.questphone.app.screens.quest.view.ai_snap

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.OrtSession.SessionOptions
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.graphics.scale
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import neth.iecal.questphone.app.screens.quest.view.ViewQuestVM
import nethical.questphone.ai.padTokenIds
import nethical.questphone.ai.preprocessBitmapToFloatBuffer
import nethical.questphone.ai.tokenizeText
import nethical.questphone.backend.Supabase
import nethical.questphone.backend.TaskValidationClient
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.backend.repositories.StatsRepository
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.data.json
import nethical.questphone.data.quest.ai.snap.AiSnap
import java.io.File
import java.io.FileOutputStream
import java.nio.LongBuffer
import javax.inject.Inject
import kotlin.random.Random

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

const val MINIMUM_ZERO_SHOT_THRESHOLD = 0.5

@HiltViewModel
class AiSnapQuestViewVM @Inject constructor(
    questRepository: QuestRepository,
    userRepository: UserRepository,
    statsRepository: StatsRepository,
    application: Application,
) : ViewQuestVM(
    questRepository, userRepository, statsRepository, application,
){
    val isAiEvaluating = MutableStateFlow(false)
    val isCameraScreen = MutableStateFlow(false)
    var aiQuest = AiSnap()


    val currentStep = MutableStateFlow(EvaluationStep.INITIALIZING)
    val error = MutableStateFlow<String?>(null)
    val results = MutableStateFlow<TaskValidationClient.ValidationResult?>(null)
    val isModelDownloaded = MutableStateFlow(true)


    private var modelSession: OrtSession? = null
    private var env: OrtEnvironment? = null
    private var isModelLoaded = false

    private lateinit var modelId: String

    private var isOnlineInferencing = true

    private val client = TaskValidationClient()
    init {
        viewModelScope.launch(Dispatchers.IO) {
            loadModel()
        }
    }

    fun setAiSnap(){
        aiQuest = json.decodeFromString<AiSnap>(commonQuestInfo.quest_json)
    }

    fun onAiSnapQuestDone(){
        saveMarkedQuestToDb()
        isCameraScreen.value = false
    }


    fun loadModel(): Boolean {
        return try {
            if (isModelLoaded) return true
            currentStep.value = EvaluationStep.CHECKING_MODEL
            env = OrtEnvironment.getEnvironment()
            val sp = application.getSharedPreferences("models", Context.MODE_PRIVATE)
            modelId = sp.getString("selected_one_shot_model", "online") ?: run {
                error.value = "No model selected"
                return false
            }
            if(modelId == "online"){
                isModelLoaded = true
                isOnlineInferencing = true
                return true

            }

            Log.d("Loading Model","Starting to load model $modelId ")
            val modelFile = File(application.filesDir, "$modelId.onnx")
            if (!modelFile.exists()) {
                isModelDownloaded.value = false
                error.value = if (sp.contains("downloading")) {
                    "Please wait until the model fully downloads"
                } else {
                    "Please download a model."
                }
                return false
            }
            currentStep.value = EvaluationStep.LOADING_MODEL
            val opts = SessionOptions().apply {
                addNnapi()
                setExecutionMode(SessionOptions.ExecutionMode.SEQUENTIAL)
                setOptimizationLevel(SessionOptions.OptLevel.ALL_OPT)
            }

            modelSession = env!!.createSession(modelFile.absolutePath, opts)
            isModelLoaded = true
            true
        } catch (e: Exception) {
            error.value = "Failed to load model: ${e.message}"
            false
        }
    }

    fun evaluateQuest(onEvaluationComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!isModelLoaded && !loadModel()) return@launch
            if(!isOnlineInferencing) {
                runOfflineInference(onEvaluationComplete)
            } else {
                runOnlineInference(onEvaluationComplete)
            }

        }
    }

    fun resetResults(){
        isAiEvaluating.value = true
        results.value = null
    }
    private suspend fun runOnlineInference(onEvaluationComplete: () -> Unit) {

        currentStep.value = EvaluationStep.INITIALIZING
        currentStep.value = EvaluationStep.LOADING_MODEL
        val photoFile = File(application.filesDir, AI_SNAP_PIC)
        val compressedFile = resizeAndCompressImage(photoFile, 1080, 50)
        client.validateTask(
            compressedFile,
            aiQuest.taskDescription,
            aiQuest.features.joinToString(","),
            Supabase.supabase.auth.currentAccessTokenOrNull()!!.toString()
        ) {
            results.value = it.getOrNull()
            currentStep.value = EvaluationStep.COMPLETED
            if(results.value?.isValid == true) {
                onEvaluationComplete()
            }
        }
        val allSteps = EvaluationStep.entries
        var currentStepInt = 0
        while(results.value != null){
            delay(Random.nextInt(500,2000).toLong())
            currentStep.value = EvaluationStep.valueOf( allSteps[currentStepInt].name)
            if(currentStepInt != EvaluationStep.EVALUATING.ordinal) currentStepInt++
        }

    }


    private fun runOfflineInference(onEvaluationComplete: () -> Unit){
        try {
            currentStep.value = EvaluationStep.INITIALIZING


            val photoFile = File(application.filesDir, AI_SNAP_PIC)
            currentStep.value = EvaluationStep.LOADING_IMAGE

            if (!photoFile.exists()) {
                error.value = "Image file not found at ${photoFile.absolutePath}"
                Log.d("Not Found",photoFile.absolutePath)
                return
            }

            val bitmap = getBitmapFromPath(photoFile.absolutePath)

            currentStep.value = EvaluationStep.PREPROCESSING

            val queries = listOf(aiQuest.taskDescription)
            val processedQueries = queries.map { "$it </s>" }

            currentStep.value = EvaluationStep.TOKENIZING

            val tokenIdsList = try {
                tokenizeText(application, processedQueries)
            } catch (e: Exception) {
                error.value = "Tokenization failed: ${e.message}"
                return
            }

            val imageTensor = OnnxTensor.createTensor(
                env,
                preprocessBitmapToFloatBuffer(bitmap!!),
                longArrayOf(1, 3, 224, 224)
            )

            val maxLength = 64
            val padTokenId = 0
            val paddedTokenIdsList = tokenIdsList.map { padTokenIds(it, maxLength, padTokenId) }
            val flatTokenIds =
                paddedTokenIdsList.flatMap { it.asList() }.map { it.toLong() }.toLongArray()

            val textTensor = OnnxTensor.createTensor(
                env,
                LongBuffer.wrap(flatTokenIds),
                longArrayOf(paddedTokenIdsList.size.toLong(), maxLength.toLong())
            )

            currentStep.value = EvaluationStep.EVALUATING

            val inputs = mapOf(
                "pixel_values" to imageTensor,
                "input_ids" to textTensor
            )

            val output = modelSession?.run(inputs)
            val logitsTensor = output?.get(0) as? OnnxTensor
            val logitsArray = logitsTensor?.floatBuffer?.array() ?: return

            val probs = logitsArray.map { 1f / (1f + kotlin.math.exp(-it)) }
            val sorted = queries.mapIndexed { i, q -> q to probs[i] }
                .sortedByDescending { it.second }

            results.value = TaskValidationClient.ValidationResult(
                sorted[0].second > MINIMUM_ZERO_SHOT_THRESHOLD,
                sorted[0].second.toString()
            )
            currentStep.value = EvaluationStep.COMPLETED

            if (sorted[0].second > MINIMUM_ZERO_SHOT_THRESHOLD) {
                onEvaluationComplete()
            }

        } catch (e: Exception) {
            error.value = "Evaluation failed: ${e.message}"
        }
    }
    override fun onCleared() {
        super.onCleared()
        try {
            modelSession?.close()
            env?.close()
            modelSession = null
            env = null
            isModelLoaded = false
        } catch (e: Exception) {
            Log.e("AiEvaluation", "Failed to close resources", e)
        }
    }


}
fun resizeAndCompressImage(file: File, maxSize: Int = 1080, quality: Int = 70): File {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath)

    // Maintain aspect ratio
    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val width: Int
    val height: Int
    if (ratio > 1) {
        width = maxSize
        height = (maxSize / ratio).toInt()
    } else {
        height = maxSize
        width = (maxSize * ratio).toInt()
    }

    val scaledBitmap = bitmap.scale(width, height)

    val compressedFile = File(file.parent, "compressed_upload.jpg")
    val out = FileOutputStream(compressedFile)
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
    out.flush()
    out.close()

    return compressedFile
}

