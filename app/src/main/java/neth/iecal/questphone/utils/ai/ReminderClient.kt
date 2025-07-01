package neth.iecal.questphone.utils.ai


import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ReminderClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // Adjust timeout as needed
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "ReminderClient"
                private const val BASE_URL = "http://34.10.142.128:80"
//        private const val BASE_URL = "http://localhost:8000"
    }

    data class ReminderResult(
        val title: String,
        val description: String
    )

    fun generateReminder(
        remainingTime: String,
        questId: String,
        token: String,
        callback: (Result<ReminderResult>) -> Unit
    ) {
        // Create multipart form data
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("quest_id", questId)

            .addFormDataPart(
                "remaining_time",
                remainingTime
            )
            .build()

        // Build the request
        val request = Request.Builder()
            .url("$BASE_URL/generate-reminder")
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
                        val result = ReminderResult(
                            json.getString("title"),
                            json.getString("description")
                        )
                        Log.i(TAG, "reminder generation result: $result")
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