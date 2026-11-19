package neth.iecal.questphone.core.utils

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.platform.Fp16Conversions
import nethical.questphone.backend.TaskValidationClient
import java.nio.DoubleBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.util.Locale
import kotlin.math.exp

const val MINIMUM_ZERO_SHOT_THRESHOLD = 0.08f

/**
 * Computes numerically stable sigmoid probability for a given logit.
 */
internal fun sigmoidProbability(logit: Float): Float {
    if (logit < -88.0f) return 0.0f
    if (logit > 88.0f) return 1.0f
    return (1.0 / (1.0 + exp(-logit.toDouble()))).toFloat()
}

/**
 * Sanitizes zero-shot feature queries, falling back to [taskDescription] when explicit features are absent.
 */
internal fun sanitizeZeroShotQueries(features: List<String>, taskDescription: String): List<String> {
    val cleanedFeatures = features.map { it.trim() }.filter { it.isNotEmpty() }
    if (cleanedFeatures.isNotEmpty()) return cleanedFeatures
    val cleanedTask = taskDescription.trim()
    return if (cleanedTask.isNotEmpty()) listOf(cleanedTask) else emptyList()
}

/**
 * Extracts a [FloatArray] from an ONNX output logits tensor, supporting FLOAT, FLOAT16, BFLOAT16, and DOUBLE.
 */
internal fun extractLogitsFloatArray(logitsTensor: OnnxTensor): FloatArray {
    val type = logitsTensor.info.type
    return extractLogitsFromBuffers(
        type = type,
        floatBuffer = logitsTensor.floatBuffer,
        shortBuffer = logitsTensor.shortBuffer,
        doubleBuffer = logitsTensor.doubleBuffer
    )
}

/**
 * Buffer-level logits extraction helper supporting FLOAT, FLOAT16, BFLOAT16, and DOUBLE.
 */
internal fun extractLogitsFromBuffers(
    type: OnnxJavaType,
    floatBuffer: FloatBuffer?,
    shortBuffer: ShortBuffer?,
    doubleBuffer: DoubleBuffer? = null
): FloatArray {
    val resolvedFloatBuffer: FloatBuffer? = when (type) {
        OnnxJavaType.FLOAT -> floatBuffer
        OnnxJavaType.FLOAT16 -> shortBuffer?.let { Fp16Conversions.convertFp16BufferToFloatBuffer(it.duplicate()) }
        OnnxJavaType.BFLOAT16 -> shortBuffer?.let { Fp16Conversions.convertBf16BufferToFloatBuffer(it.duplicate()) }
        OnnxJavaType.DOUBLE -> {
            if (doubleBuffer == null) {
                throw IllegalStateException("Unsupported logits tensor type: $type (doubleBuffer is null)")
            }
            val copy = doubleBuffer.duplicate()
            val array = FloatArray(copy.remaining())
            for (i in array.indices) {
                array[i] = copy.get().toFloat()
            }
            return array
        }
        else -> floatBuffer
    }

    if (resolvedFloatBuffer == null) {
        throw IllegalStateException("Unsupported logits tensor type: $type")
    }

    val copy = resolvedFloatBuffer.duplicate()
    val array = FloatArray(copy.remaining())
    copy.get(array)
    return array
}

/**
 * Evaluates zero-shot detection logits against queries and confidence thresholds.
 * Returns the list of matched query strings and the formatted [TaskValidationClient.ValidationResult].
 */
internal fun evaluateZeroShotFeatures(
    queries: List<String>,
    hasExplicitFeatures: Boolean,
    taskDescription: String,
    logitsArray: FloatArray,
    threshold: Float = MINIMUM_ZERO_SHOT_THRESHOLD
): Pair<List<String>, TaskValidationClient.ValidationResult> {
    if (queries.isEmpty() || logitsArray.isEmpty()) {
        return emptyList<String>() to TaskValidationClient.ValidationResult(
            isValid = false,
            reason = "Empty logits produced during zero-shot evaluation."
        )
    }

    val detectedFeatures = mutableListOf<String>()
    val detectedWithConfidence = mutableListOf<String>()
    val allScores = StringBuilder()

    for (i in queries.indices) {
        val logit = if (i < logitsArray.size) logitsArray[i] else -100f
        val prob = sigmoidProbability(logit)
        val percentage = "%.1f%%".format(Locale.US, prob * 100f)
        if (prob >= threshold) {
            detectedFeatures.add(queries[i])
            detectedWithConfidence.add("${queries[i]} (Confidence: $percentage)")
        }
        allScores.append("\n- ${queries[i]}: $percentage (Logit: ${"%.2f".format(Locale.US, logit)})")
    }

    val isSuccess = if (hasExplicitFeatures) {
        detectedFeatures.size == queries.size && queries.isNotEmpty()
    } else {
        detectedFeatures.isNotEmpty()
    }

    val fallbackResult = TaskValidationClient.ValidationResult(
        isValid = isSuccess,
        reason = if (isSuccess) {
            "Verified locally via SigLIP 2!\nMatched features:\n${detectedWithConfidence.joinToString("\n") { "[OK] $it" }}"
        } else {
            "Could not confidently verify all required features for '$taskDescription'.\n\nDetected Scores:$allScores"
        }
    )

    return detectedFeatures to fallbackResult
}
