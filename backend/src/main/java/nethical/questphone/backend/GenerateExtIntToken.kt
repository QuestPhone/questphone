package nethical.questphone.backend

import android.util.Log
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GenerateExtIntToken {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS) // Adjust timeout as needed
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GenerateExtIntToken"
        private const val BASE_URL = "https://questphone.app" // Use 10.0.2.2 for emulator localhost
    }

    fun generateToken(
        token: String,
        callback: (Result<String>) -> Unit
    ) {
        Log.d("supabaseToken",token)
        val request = Request.Builder()
            .url("$BASE_URL/api/gen-integration-token")
            .header("Authorization", "Bearer $token")
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
                        Log.e(TAG, "Server error: $errorBody $")
                        callback(Result.failure(IOException("Server error: ${response.code} - $errorBody")))
                        return
                    }

                    val responseBody = response.body?.string()
                    Log.d("response",responseBody.toString())
                    if (responseBody.isNullOrEmpty()) {
                        Log.e(TAG, "Empty response from server")
                        callback(Result.failure(IOException("Empty response from server")))
                        return
                    }

                    try {
                        val json = JSONObject(responseBody)
                        val result = json.getString("token")
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