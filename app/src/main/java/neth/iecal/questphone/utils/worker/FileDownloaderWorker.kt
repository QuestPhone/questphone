package neth.iecal.questphone.utils.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

class FileDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .build()

    override suspend fun doWork(): Result {
        val context = applicationContext
        val url = inputData.getString("url") ?: return Result.failure()
        val fileName = inputData.getString("fileName") ?: "downloaded_file.zip"
        val modelId = inputData.getString("model_id") ?: return Result.failure()

        val file = File(context.filesDir, fileName)
        var downloadedBytes = 0L
        if (file.exists()) {
            downloadedBytes = file.length() // current size of partial file
        }

        try {
            showStartingDownload(context, fileName)
            createNotificationChannel(context)

            val totalFileSize = getContentLength(url)
            Log.i("Download", "Total file size is $totalFileSize bytes")

            var attempt = 0
            val maxRetries = 3

            // Retry loop - to handle intermittent failures
            while (attempt < maxRetries) {
                val requestBuilder = Request.Builder()
                    .url(url)
                // Set Range header to resume from last downloaded byte
                if (downloadedBytes > 0) {
                    requestBuilder.addHeader("Range", "bytes=$downloadedBytes-")
                    Log.i("Download", "Resuming from byte $downloadedBytes")
                }

                val request = requestBuilder.build()

                client.newCall(request).execute().use { response ->

                    if (response.code == 416) {
                        // HTTP 416 means Range Not Satisfiable - file complete
                        Log.i("Download", "File already completely downloaded by server")
                        showSuccessNotification(context, fileName)

                        // Save state to prefs - mark downloaded
                        context.getSharedPreferences("models", Context.MODE_PRIVATE)
                            .edit(commit = true) {
                                putString("selected_one_shot_model", modelId)
                                putBoolean("is_downloaded_$modelId", true)
                                remove("downloading")
                            }

                    }

                    if (!response.isSuccessful) {
                        Log.e("Download", "Server error: ${response.code}")
                        showErrorNotification(context, fileName, "Server error: ${response.code}")
                        return Result.failure()
                    }

                    val contentLength = response.body?.contentLength() ?: -1L
                    Log.i("Download", "Content-Length for this request: $contentLength")

                    // Use RandomAccessFile to write at offset for resumable
                    RandomAccessFile(file, "rw").use { raf ->

                        raf.seek(downloadedBytes) // move pointer to resume location

                        val input = response.body?.byteStream()
                            ?: throw Exception("Response body is null")

                        val buffer = ByteArray(64 * 1024)
                        var read: Int
                        var totalRead = downloadedBytes
                        var lastProgress = -1

                        showProgressNotification(context, fileName, 0)

                        while (true) {
                            read = input.read(buffer)
                            if (read == -1) break

                            raf.write(buffer, 0, read)
                            totalRead += read


                            if (totalFileSize > 0) {
                                val progress = ((totalRead * 100) / totalFileSize).toInt()
                                    .coerceIn(0, 100)
                                if (progress != lastProgress ) {
                                    showProgressNotification(context, fileName, progress)
                                    lastProgress = progress
                                }
                            }
                        }
                    }
                }

                val finalFileSize = file.length()
                if (totalFileSize > 0 && finalFileSize != totalFileSize) {
                    Log.e("Download", "Download incomplete: expected $totalFileSize but got $finalFileSize")
                    showErrorNotification(context, fileName, "Download Error")
                    attempt++
                    if (attempt >= maxRetries) {
                        return Result.failure()
                    } else {
                        // Wait a bit before retrying
                        delay(2000)
                        continue
                    }
                }
                // Success if reached here without exception
                showSuccessNotification(context, fileName)

                // Save state to prefs - mark downloaded
                context.getSharedPreferences("models", Context.MODE_PRIVATE).edit(commit = true) {
                    putString("selected_one_shot_model", modelId)
                    putBoolean("is_downloaded_$modelId", true)
                    remove("downloading")
                }

                Log.i("Download", "Download complete after $attempt attempt(s).")
                return Result.success()
            }

            // If failed after retries
            Log.e("Download", "Download failed after $maxRetries attempts")
            showErrorNotification(context, fileName, "Download failed after retries")
            return Result.failure()

        } catch (e: Exception) {
            Log.e("Download", "Error during download", e)
            showErrorNotification(context, fileName, e.message ?: "Unknown error")

            context.getSharedPreferences("models", Context.MODE_PRIVATE).edit(commit = true) {
                remove("downloading")
            }
            return Result.failure()
        }
    }

    private fun getContentLength(url: String): Long {
        // Quick HEAD request to get total content length
        val headRequest = Request.Builder()
            .url(url)
            .head()
            .build()
        client.newCall(headRequest).execute().use { response ->
            if (response.isSuccessful) {
                return response.header("Content-Length")?.toLongOrNull() ?: -1L
            }
        }
        return -1L
    }

    private fun showStartingDownload(context: Context, fileName: String) {

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, "download_channel")

            .setSmallIcon(android.R.drawable.stat_sys_download)

            .setContentTitle("Starting Download")

            .setPriority(NotificationCompat.PRIORITY_MAX)

            .build()


        notificationManager.notify(fileName.hashCode(), notification)

    }

    private fun showProgressNotification(context: Context, fileName: String, progress: Int) {

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, "download_channel")

            .setSmallIcon(android.R.drawable.stat_sys_download)

            .setContentTitle("Downloading $fileName")

            .setContentText("$progress% complete")

            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setProgress(100, progress, false)

            .setOngoing(true)
            .setSilent(true)

            .build()


        notificationManager.notify(fileName.hashCode(), notification)

    }


    private fun showSuccessNotification(context: Context, fileName: String) {

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, "download_channel")

            .setSmallIcon(android.R.drawable.stat_sys_download_done)

            .setContentTitle("Download Complete")

            .setContentText("$fileName downloaded successfully")

            .setAutoCancel(true)

            .setPriority(NotificationCompat.PRIORITY_MAX)

            .build()


        notificationManager.notify(fileName.hashCode(), notification)

    }


    private fun showErrorNotification(context: Context, fileName: String, error: String?) {

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, "download_channel")

            .setSmallIcon(android.R.drawable.stat_notify_error)

            .setContentTitle("Download Failed")

            .setContentText("$fileName: ${error ?: "Unknown error"}")

            .setAutoCancel(true)

            .build()


        notificationManager.notify(fileName.hashCode(), notification)

    }


    private fun createNotificationChannel(context: Context) {

        val channel = NotificationChannel(

            "download_channel",

            "Downloads",

            NotificationManager.IMPORTANCE_HIGH

        ).apply {

            description = "File download progress"

        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        notificationManager.createNotificationChannel(channel)

    }
}