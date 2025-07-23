package neth.iecal.questphone.utils.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import androidx.core.graphics.get
import androidx.core.graphics.scale
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer


object SentencePieceProcessor {
    init {
        System.loadLibrary("sentencepiece")
        System.loadLibrary("sentencepiece_jni")
    }
    external fun load(modelPath: String): Int
    external fun encodeAsIds(input: String): IntArray
}



fun preprocessBitmapToFloatBuffer(bitmap: Bitmap): FloatBuffer {
    val width = 224
    val height = 224
    val floatValues = FloatArray(3 * width * height)
    val mean = floatArrayOf(0.5f, 0.5f, 0.5f)
    val std = floatArrayOf(0.5f, 0.5f, 0.5f)

    val resizedBitmap = bitmap.scale(width, height)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = resizedBitmap[x, y]
            val r = Color.red(pixel) / 255f
            val g = Color.green(pixel) / 255f
            val b = Color.blue(pixel) / 255f

            val index = y * width + x
            floatValues[0 + index] = (r - mean[0]) / std[0]
            floatValues[1 * width * height + index] = (g - mean[1]) / std[1]
            floatValues[2 * width * height + index] = (b - mean[2]) / std[2]
        }
    }
    return FloatBuffer.wrap(floatValues)
}


fun tokenizeText(context: Context, texts: List<String>): List<IntArray> {
    val modelPath = copyModelToInternalStorage(context, "tokenizer.model")
    if (SentencePieceProcessor.load(modelPath) != 0) {
        throw RuntimeException("Failed to load SentencePiece model")
    }
    return texts.map { text ->
        SentencePieceProcessor.encodeAsIds(text)
    }
}

fun padTokenIds(tokenIds: IntArray, maxLength: Int = 64, padTokenId: Int = 49407): LongArray {
    val paddedIds = LongArray(maxLength) { padTokenId.toLong() }
    val length = minOf(tokenIds.size, maxLength)
    for (i in 0 until length) {
        paddedIds[i] = tokenIds[i].toLong()
    }
    Log.d("Padding", "Token IDs: ${tokenIds.joinToString()}, Padded IDs: ${paddedIds.joinToString()}")
    return paddedIds
}

fun testTokenization(context: Context) {
    val texts = listOf("a girl", "a boy", "a car")
    val tokenIdsList = tokenizeText(context, texts)
    tokenIdsList.forEachIndexed { i, ids ->
        Log.d("Tokenization", "Text: ${texts[i]}, Token IDs: ${ids.joinToString()}")
    }
}

fun copyModelToInternalStorage(context: Context, assetFileName: String): String {
    val file = File(context.filesDir, assetFileName)

    if (!file.exists()) {
        try {
            context.assets.open(assetFileName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("ModelCopy", "Copied $assetFileName to internal storage.")
        } catch (e: Exception) {
            Log.e("ModelCopy", "Failed to copy model: ${e.message}")
            e.printStackTrace()
        }
    } else {
        Log.d("ModelCopy", "$assetFileName already exists in internal storage.")
    }

    return file.absolutePath
}
