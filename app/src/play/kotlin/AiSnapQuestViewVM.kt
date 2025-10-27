package neth.iecal.questphone

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
import neth.iecal.questphone.app.screens.quest.view.ai_snap.AI_SNAP_PIC
import neth.iecal.questphone.backed.repositories.QuestRepository
import neth.iecal.questphone.backed.repositories.StatsRepository
import neth.iecal.questphone.backed.repositories.UserRepository
import neth.iecal.questphone.core.Supabase
import nethical.questphone.backend.TaskValidationClient
import nethical.questphone.data.EvaluationStep
import nethical.questphone.data.json
import nethical.questphone.data.quest.ai.snap.AiSnap
import javax.inject.Inject
import kotlin.random.Random


@HiltViewModel
class AiSnapQuestViewVM @Inject constructor(
    questRepository: QuestRepository,
    userRepository: UserRepository,
    statsRepository: StatsRepository,
    application: android.app.Application,
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
        isModelLoaded = true
        isOnlineInferencing = true
        return true
    }

    fun evaluateQuest(onEvaluationComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!isModelLoaded && !loadModel()) return@launch
            if(isOnlineInferencing) {
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
        val photoFile = java.io.File(application.filesDir, AI_SNAP_PIC)
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


    override fun onCleared() {
        super.onCleared()
        try {
            isModelLoaded = false
        } catch (e: Exception) {
            android.util.Log.e("AiEvaluation", "Failed to close resources", e)
        }
    }


}
fun resizeAndCompressImage(file: java.io.File, maxSize: Int = 1080, quality: Int = 70): java.io.File {
    val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)

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

    val compressedFile = java.io.File(file.parent, "compressed_upload.jpg")
    val out = java.io.FileOutputStream(compressedFile)
    scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
    out.flush()
    out.close()

    return compressedFile
}

