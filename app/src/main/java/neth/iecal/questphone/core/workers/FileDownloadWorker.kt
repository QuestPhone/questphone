package neth.iecal.questphone.core.workers

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit

class FileDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_URL = "url"
        const val KEY_FILE_NAME = "fileName"
        const val KEY_MODEL_ID = "model_id"
        const val KEY_MODEL_VERSION = "version"
        const val KEY_IS_ONE_SHOT = "is_one_shot"
        private const val NOTIFICATION_CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Downloads"
        private const val DOWNLOAD_BUFFER_SIZE = 1024 * 256 // 256KB buffer for efficiency
        private const val MAX_RETRIES = 5
        private const val RETRY_DELAY_MS = 5000L
        private const val TAG = "FileDownloadWorker"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .build()

    // Single NotificationManager and Builder instance for efficiency
    private val notificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var lastProgress = -1

    override suspend fun doWork(): Result {
        // Extract input data; fail if essential data is missing.
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "downloaded_file.zip"
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return Result.failure()
        val isOneShotModel = inputData.getBoolean(KEY_IS_ONE_SHOT, false)
        val modelVersion = inputData.getString(KEY_MODEL_VERSION) ?: "0.0.0"

        val notificationId = fileName.hashCode()
        val file = File(applicationContext.filesDir, fileName)

        return try {
            // CRITICAL FIX: Create channel and set foreground FIRST
            createNotificationChannel()
            setForeground(createForegroundInfo(fileName, notificationId))

            // Now proceed with the download
            downloadFile(url, file, fileName, notificationId)

            updateSharedPrefsOnSuccess(modelId, modelVersion, isOneShotModel)
            showSuccessNotification(fileName, notificationId)
            Log.i(TAG, "Download successful for $fileName.")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Download failed permanently for $fileName.", e)
            updateSharedPrefsOnFailure()
            showErrorNotification(fileName, notificationId, e.message ?: "An unknown error occurred")
            file.delete() // IMPORTANT: Delete partial file on unrecoverable failure
            Result.failure()
        }
    }

    // CRITICAL FIX: Override getForegroundInfo for immediate foreground promotion
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "downloading..."
        val notificationId = fileName.hashCode()
        return createForegroundInfo(fileName, notificationId)
    }

    private fun createForegroundInfo(fileName: String, notificationId: Int): ForegroundInfo {
        notificationBuilder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.stat_sys_download)
            .setContentTitle("Starting Download")
            .setContentText(fileName)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(100, 0, true)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(19, notificationBuilder.build(), FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(19, notificationBuilder.build())
        }
    }

    private suspend fun downloadFile(url: String, file: File, fileName: String, notificationId: Int) {
        for (attempt in 1..MAX_RETRIES) {
            try {
                val downloadedBytes = if (file.exists()) file.length() else 0L

                val request = Request.Builder().url(url)
                if (downloadedBytes > 0) {
                    request.addHeader("Range", "bytes=$downloadedBytes-")
                    Log.i(TAG, "Attempt $attempt: Resuming download for $fileName from byte $downloadedBytes")
                } else {
                    Log.i(TAG, "Attempt $attempt: Starting new download for $fileName")
                }

                client.newCall(request.build()).execute().use { response ->
                    when (response.code) {
                        // HTTP 416: Range Not Satisfiable. The file is already fully downloaded.
                        416 -> {
                            Log.i(TAG, "File $fileName already downloaded (Server returned 416).")
                            return // Success
                        }
                        // HTTP 200 (OK) for new downloads, 206 (Partial Content) for resumed downloads.
                        200, 206 -> {
                            val body = response.body ?: throw IllegalStateException("Response body is null")

                            // Dynamically determine the total file size from the response headers.
                            val totalFileSize = if (response.code == 200) {
                                body.contentLength()
                            } else {
                                parseTotalSizeFromContentRange(response.header("Content-Range"))
                            }

                            // A secondary check to see if the file is already complete.
                            if (totalFileSize != -1L && downloadedBytes >= totalFileSize) {
                                Log.i(TAG, "File $fileName already downloaded (size check).")
                                return // Success
                            }

                            // Write the downloaded bytes to the file.
                            val finalSize = writeToFile(body.byteStream(), file, downloadedBytes, totalFileSize, fileName, notificationId)

                            // Final verification: ensure the downloaded file size matches the expected total size.
                            if (totalFileSize != -1L && finalSize != totalFileSize) {
                                throw IllegalStateException("Download incomplete. Expected $totalFileSize, got $finalSize.")
                            }

                            Log.i(TAG, "Download stream finished successfully on attempt $attempt.")
                            return // Success
                        }
                        else -> {
                            // Any other server response is treated as a temporary failure.
                            throw IllegalStateException("Server error: ${response.code} ${response.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Attempt $attempt to download $fileName failed.", e)
                if (attempt == MAX_RETRIES) {
                    throw e // Re-throw the last exception to fail the worker.
                }
                delay(RETRY_DELAY_MS) // Wait before the next attempt.
            }
        }
    }

    /**
     * Writes the input stream to a file at a specific offset, updating progress.
     */
    private fun writeToFile(
        input: InputStream,
        file: File,
        downloadedBytes: Long,
        totalFileSize: Long,
        fileName: String,
        notificationId: Int
    ): Long {
        var totalRead = downloadedBytes
        RandomAccessFile(file, "rw").use { raf ->
            raf.seek(downloadedBytes) // Move to the end of the partially downloaded file.
            val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                raf.write(buffer, 0, bytesRead)
                totalRead += bytesRead

                // Update notification progress, but not too frequently.
                if (totalFileSize > 0) {
                    val progress = ((totalRead * 100) / totalFileSize).toInt().coerceIn(0, 100)
                    if (progress > lastProgress) {
                        updateProgressNotification(fileName, notificationId, progress)
                        lastProgress = progress
                    }
                }
            }
        }
        return totalRead
    }

    /**
     * Parses the total file size from a "Content-Range" header (e.g., "bytes 21010-47021/47022").
     */
    private fun parseTotalSizeFromContentRange(contentRange: String?): Long {
        return contentRange?.substringAfter('/')?.toLongOrNull() ?: -1L
    }

    private fun updateSharedPrefsOnSuccess(
        modelId: String,
        version: String,
        isOneShotModel: Boolean
    ) {
        applicationContext.getSharedPreferences("models", Context.MODE_PRIVATE).edit(commit = true) {
            if (isOneShotModel) {
                putString("selected_one_shot_model", modelId)
            }
            putBoolean("is_downloaded_$modelId", true)
            putString("version_$modelId", version)
            remove("downloading")
        }
    }

    private fun updateSharedPrefsOnFailure() {
        applicationContext.getSharedPreferences("models", Context.MODE_PRIVATE).edit(commit = true) {
            remove("downloading")
        }
    }

    // --- Notification Management ---
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW // Use LOW to be less intrusive for long-running tasks.
        ).apply {
            description = "Shows file download progress."
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun updateProgressNotification(fileName: String, notificationId: Int, progress: Int) {
        notificationBuilder
            .setContentTitle("Downloading $fileName")
            .setContentText("$progress% complete")
            .setProgress(100, progress, false) // Switch to determinate progress.
            .setSilent(true) // Don't make a sound for each update.
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun showSuccessNotification(fileName: String, notificationId: Int) {
        notificationManager.cancel(notificationId) // remove the ongoing download notification
        val successNotification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.stat_sys_download_done)
            .setContentTitle("Download Complete")
            .setContentText("$fileName has been successfully downloaded.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId + 1, successNotification) // different ID so it doesn't get stuck
    }

    private fun showErrorNotification(fileName: String, notificationId: Int, error: String) {
        val errorNotification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.stat_notify_error)
            .setContentTitle("Download Failed")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Could not download $fileName: $error"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        notificationManager.notify(notificationId, errorNotification.build())
    }
}