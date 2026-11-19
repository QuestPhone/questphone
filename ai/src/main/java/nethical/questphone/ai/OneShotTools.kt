package nethical.questphone.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

object SentencePieceProcessor {
    @Volatile
    private var cachedPath: String? = null

    @Volatile
    private var nativeLibraryLoaded: Boolean = false

    init {
        try {
            System.loadLibrary("sentencepiece")
            System.loadLibrary("sentencepiece_jni")
            nativeLibraryLoaded = true
        } catch (e: UnsatisfiedLinkError) {
            Log.e("SentencePiece", "Failed to load sentencepiece native libraries", e)
        }
    }

    fun isModelLoaded(): Boolean = cachedPath != null

    fun loadModel(modelPath: String): Int {
        if (!nativeLibraryLoaded) return -1
        if (cachedPath == modelPath) return 0
        synchronized(this) {
            if (cachedPath == modelPath) return 0
            return try {
                val res = load(modelPath)
                if (res == 0) {
                    cachedPath = modelPath
                }
                res
            } catch (e: UnsatisfiedLinkError) {
                Log.e("SentencePiece", "JNI load failed", e)
                -1
            }
        }
    }

    external fun load(modelPath: String): Int
    external fun encodeAsIds(input: String): IntArray
}

fun preprocessBitmapToFloatBuffer(bitmap: Bitmap): FloatBuffer {
    val width = 224
    val height = 224

    val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false)
            ?: throw IllegalStateException("Failed to convert HARDWARE bitmap to ARGB_8888")
    } else {
        bitmap
    }

    val resizedBitmap = if (safeBitmap.width == width && safeBitmap.height == height) {
        safeBitmap
    } else {
        safeBitmap.scale(width, height)
    }

    try {
        val channelSize = width * height
        val pixels = IntArray(channelSize)
        resizedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val floatValues = FloatArray(3 * channelSize)
        val mean = 0.5f
        val std = 0.5f

        for (i in 0 until channelSize) {
            val pixel = pixels[i]
            val r = ((pixel shr 16) and 0xFF) / 255f
            val g = ((pixel shr 8) and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f

            floatValues[i] = (r - mean) / std
            floatValues[channelSize + i] = (g - mean) / std
            floatValues[2 * channelSize + i] = (b - mean) / std
        }

        val byteBuffer = ByteBuffer.allocateDirect(3 * channelSize * 4)
            .order(ByteOrder.nativeOrder())
        val floatBuffer = byteBuffer.asFloatBuffer()
        floatBuffer.put(floatValues)
        floatBuffer.flip()
        return floatBuffer
    } finally {
        if (safeBitmap != bitmap) {
            safeBitmap.recycle()
        }
        if (resizedBitmap != bitmap && resizedBitmap != safeBitmap) {
            resizedBitmap.recycle()
        }
    }
}

fun tokenizeText(context: Context, texts: List<String>): List<IntArray> {
    val modelFile = File(context.filesDir, "tokenizer.model")
    val modelPath = modelFile.absolutePath

    if (!modelFile.exists()) {
        throw IllegalStateException("SentencePiece tokenizer.model not found at $modelPath")
    }

    if (SentencePieceProcessor.loadModel(modelPath) != 0) {
        throw RuntimeException("Failed to load SentencePiece model at $modelPath")
    }

    return texts.map { text ->
        val normalized = text.lowercase().trim()
        SentencePieceProcessor.encodeAsIds(normalized)
    }
}

fun padTokenIds(
    tokenIds: IntArray,
    maxLength: Int = 64,
    padTokenId: Long = 0L,
    eosTokenId: Long = 1L
): LongArray {
    require(maxLength > 0) { "maxLength must be greater than 0" }
    val paddedIds = LongArray(maxLength) { padTokenId }
    val hasEos = tokenIds.isNotEmpty() && tokenIds.last().toLong() == eosTokenId

    if (hasEos) {
        val count = minOf(tokenIds.size, maxLength)
        for (i in 0 until count) {
            paddedIds[i] = tokenIds[i].toLong()
        }
        if (tokenIds.size >= maxLength) {
            paddedIds[maxLength - 1] = eosTokenId
        }
    } else {
        val maxTokensBeforeEos = maxLength - 1
        val count = minOf(tokenIds.size, maxTokensBeforeEos)
        for (i in 0 until count) {
            paddedIds[i] = tokenIds[i].toLong()
        }
        paddedIds[count] = eosTokenId
    }
    return paddedIds
}

fun padTokenIds(
    tokenIds: IntArray,
    maxLength: Int,
    padTokenId: Int,
    eosTokenId: Int = 1
): LongArray = padTokenIds(tokenIds, maxLength, padTokenId.toLong(), eosTokenId.toLong())

fun testTokenization(context: Context) {
    val texts = listOf("a girl", "a boy", "a car")
    val tokenIdsList = tokenizeText(context, texts)
    tokenIdsList.forEachIndexed { i, ids ->
        Log.d("Tokenization", "Text: ${texts[i]}, Token IDs: ${ids.joinToString()}")
    }
}
