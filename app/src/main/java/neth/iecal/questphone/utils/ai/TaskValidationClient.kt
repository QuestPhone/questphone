package neth.iecal.questphone.utils.ai

import neth.iecal.questphone.BuildConfig
import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class TaskValidationClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // Adjust timeout as needed
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "TaskValidationClient"
        private const val BASE_URL = BuildConfig.API_URL
//        private const val BASE_URL = "http://localhost:8000"
    }

    data class ValidationResult(
        val isValid: Boolean,
        val reason: String
    )

    fun validateTask(
        imageFile: File,
        questId: String,
        token: String,
        callback: (Result<ValidationResult>) -> Unit
    ) {
        // Create multipart form data
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("quest_id", questId)

            .addFormDataPart(
                "image",
                imageFile.name,
                imageFile.asRequestBody("image/jpeg".toMediaType())
            )
            .build()

        // Build the request
        val request = Request.Builder()
            .url("$BASE_URL/validate-task")
            .header("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        // Execute the request asynchronously
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network error: ${e.message}", e)
                callback(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        val errorBody = response.body?.string() ?: "No error details"
                        Log.e(TAG, "Server error: $errorBody")
                        callback(Result.failure(IOException("Server error: ${response.code} - $errorBody")))
                        return
                    }

                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        Log.e(TAG, "Empty response from server")
                        callback(Result.failure(IOException("Empty response from server")))
                        return
                    }
                    Log.d("server Response",responseBody)
                    try {
                        val json = JSONObject(responseBody)
                        val result = ValidationResult(
                            isValid = json.getBoolean("is_valid"),
                            reason = json.getString("reason")
                        )
                        Log.i(TAG, "Validation result: $result")
                        callback(Result.success(result))
                    } catch (e: Exception) {
                        Log.e(TAG, "JSON parsing error: ${e.message}", e)
                        callback(Result.failure(e))
                    }
                }
            }
        })
    }
}