package neth.iecal.questphone.core.workers

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
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
        const val TOKENIZER_WORK_NAME = "download_tokenizer.model"
        const val TOKENIZER_MODEL_URL =
            "https://huggingface.co/onnx-community/siglip2-base-patch16-224-ONNX/resolve/main/tokenizer.model"
        private const val NOTIFICATION_CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "Downloads"
        private const val DOWNLOAD_BUFFER_SIZE = 1024 * 256 // 256KB buffer for efficiency
        private const val MAX_RETRIES = 5
        private const val RETRY_DELAY_MS = 5000L
        private const val TAG = "FileDownloadWorker"

        fun enqueueTokenizerDownload(context: Context) {
            val tokenizerData = Data.Builder()
                .putString(KEY_URL, TOKENIZER_MODEL_URL)
                .putString(KEY_FILE_NAME, "tokenizer.model")
                .putString(KEY_MODEL_ID, "tokenizer")
                .putString(KEY_MODEL_VERSION, "1.0.0")
                .putBoolean(KEY_IS_ONE_SHOT, false)
                .build()

            val tokenizerWork = OneTimeWorkRequestBuilder<FileDownloadWorker>()
                .setInputData(tokenizerData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                TOKENIZER_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                tokenizerWork
            )
        }

        /**
         * Parses the total file size from a "Content-Range" header (e.g., "bytes 21010-47021/47022").
         */
        internal fun parseTotalSizeFromContentRange(contentRange: String?): Long {
            return contentRange?.substringAfter('/')?.toLongOrNull() ?: -1L
        }

        /**
         * Resolves the effective starting byte offset and total expected file size for HTTP 200 and 206 responses.
         */
        internal fun resolveEffectiveOffsetAndTotalSize(
            responseCode: Int,
            existingBytes: Long,
            contentLength: Long,
            contentRangeHeader: String?
        ): Pair<Long, Long> {
            val effectiveDownloadedBytes = if (responseCode == 200) 0L else existingBytes
            val totalFileSize = if (responseCode == 200) {
                contentLength
            } else {
                val fromHeader = parseTotalSizeFromContentRange(contentRangeHeader)
                if (fromHeader != -1L) {
                    fromHeader
                } else if (contentLength != -1L) {
                    effectiveDownloadedBytes + contentLength
                } else {
                    -1L
                }
            }
            return effectiveDownloadedBytes to totalFileSize
        }

        /**
         * Writes an InputStream into a RandomAccessFile at [downloadedBytes] offset, truncating if starting at 0.
         */
        internal fun writeStreamToRandomAccessFile(
            input: InputStream,
            file: File,
            downloadedBytes: Long,
            totalFileSize: Long,
            initialLastProgress: Int = -1,
            isCancelled: () -> Boolean = { false },
            onProgress: (Int) -> Unit = {}
        ): Long {
            var totalRead = downloadedBytes
            var lastReportedProgress = initialLastProgress
            RandomAccessFile(file, "rw").use { raf ->
                if (downloadedBytes == 0L) {
                    raf.setLength(0)
                }
                raf.seek(downloadedBytes)
                val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    if (isCancelled()) {
                        throw kotlinx.coroutines.CancellationException("Download stopped by WorkManager")
                    }
                    raf.write(buffer, 0, bytesRead)
                    totalRead += bytesRead

                    if (totalFileSize > 0) {
                        val progress = ((totalRead * 100) / totalFileSize).toInt().coerceIn(0, 100)
                        if (progress > lastReportedProgress) {
                            onProgress(progress)
                            lastReportedProgress = progress
                        }
                    }
                }
            }
            return totalRead
        }

        private val downloadingPrefLock = Any()

        internal fun updatePrefsOnSuccess(
            sp: SharedPreferences,
            modelId: String,
            version: String,
            isOneShotModel: Boolean
        ) {
            synchronized(downloadingPrefLock) {
                val activeDownload = sp.getString("downloading", null)
                sp.edit(commit = true) {
                    if (isOneShotModel) {
                        if (activeDownload == null || activeDownload == modelId) {
                            putString("selected_one_shot_model", modelId)
                        }
                        if (activeDownload == modelId) {
                            remove("downloading")
                        }
                    }
                    putBoolean("is_downloaded_$modelId", true)
                    putString("version_$modelId", version)
                }
            }
        }

        internal fun updatePrefsOnFailure(
            sp: SharedPreferences,
            modelId: String,
            isOneShotModel: Boolean
        ) {
            if (!isOneShotModel) return
            synchronized(downloadingPrefLock) {
                if (sp.getString("downloading", null) == modelId) {
                    sp.edit(commit = true) {
                        remove("downloading")
                    }
                }
            }
        }
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
        val modelId = inputData.getString(KEY_MODEL_ID) ?: return Result.failure()
        val isOneShotModel = inputData.getBoolean(KEY_IS_ONE_SHOT, false)
        val url = inputData.getString(KEY_URL)
        if (url == null) {
            updateSharedPrefsOnFailure(modelId, isOneShotModel)
            return Result.failure()
        }
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "downloaded_file.zip"
        val modelVersion = inputData.getString(KEY_MODEL_VERSION) ?: "0.0.0"

        val notificationId = fileName.hashCode()
        val file = File(applicationContext.filesDir, fileName)

        return try {
            createNotificationChannel()
            try {
                setForeground(createForegroundInfo(fileName, notificationId))
            } catch (e: Exception) {
                Log.w(TAG, "Failed to promote worker to foreground, continuing download: ${e.message}")
            }

            // Proceed with the download
            downloadFile(url, file, fileName, modelId, notificationId)

            updateSharedPrefsOnSuccess(modelId, modelVersion, isOneShotModel)
            showSuccessNotification(fileName, notificationId)
            Log.i(TAG, "Download successful for $fileName.")
            Result.success()

        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException || e is java.util.concurrent.CancellationException) {
                Log.i(TAG, "Download cancelled for $fileName.")
                updateSharedPrefsOnFailure(modelId, isOneShotModel)
                throw e
            }
            Log.e(TAG, "Download failed permanently for $fileName.", e)
            updateSharedPrefsOnFailure(modelId, isOneShotModel)
            showErrorNotification(fileName, notificationId, e.message ?: "An unknown error occurred")
            Result.failure()
        }
    }

    // Override getForegroundInfo for immediate foreground promotion
    override suspend fun getForegroundInfo(): ForegroundInfo {
        val fileName = inputData.getString(KEY_FILE_NAME) ?: "downloading..."
        val notificationId = fileName.hashCode()
        return createForegroundInfo(fileName, notificationId)
    }

    private fun createForegroundInfo(fileName: String, notificationId: Int): ForegroundInfo {
        createNotificationChannel()
        notificationBuilder = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.stat_sys_download)
            .setContentTitle("Starting Download")
            .setContentText(fileName)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(100, 0, true)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notificationBuilder.build(), FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notificationBuilder.build())
        }
    }

    private suspend fun downloadFile(
        url: String,
        file: File,
        fileName: String,
        modelId: String,
        notificationId: Int
    ) {
        val sp = applicationContext.getSharedPreferences("models", Context.MODE_PRIVATE)
        val partFile = File(file.parentFile, "$fileName.part")

        // If a previous incomplete download was written directly to `file`, migrate it to `partFile`
        if (file.exists() && !partFile.exists() && !sp.getBoolean("is_downloaded_$modelId", false)) {
            file.renameTo(partFile)
        }

        for (attempt in 1..MAX_RETRIES) {
            try {
                val downloadedBytes = if (partFile.exists()) partFile.length() else 0L

                val request = Request.Builder().url(url)
                if (downloadedBytes > 0) {
                    request.addHeader("Range", "bytes=$downloadedBytes-")
                    Log.i(TAG, "Attempt $attempt: Resuming download for $fileName from byte $downloadedBytes")
                } else {
                    Log.i(TAG, "Attempt $attempt: Starting new download for $fileName")
                }

                client.newCall(request.build()).execute().use { response ->
                    when (response.code) {
                        // HTTP 416: Range Not Satisfiable. The partial file is already complete.
                        416 -> {
                            Log.i(TAG, "File $fileName already downloaded (Server returned 416).")
                            finalizePartFile(partFile, file)
                            return
                        }
                        // HTTP 200 (OK) for new downloads (or servers ignoring Range), 206 (Partial Content) for resumed downloads.
                        200, 206 -> {
                            val body = response.body ?: throw IllegalStateException("Response body is null")

                            if (response.code == 200 && partFile.exists() && partFile.length() > 0) {
                                partFile.delete()
                            }

                            val (effectiveDownloadedBytes, totalFileSize) = resolveEffectiveOffsetAndTotalSize(
                                responseCode = response.code,
                                existingBytes = downloadedBytes,
                                contentLength = body.contentLength(),
                                contentRangeHeader = response.header("Content-Range")
                            )

                            lastProgress = if (effectiveDownloadedBytes > 0 && totalFileSize > 0) {
                                ((effectiveDownloadedBytes * 100) / totalFileSize).toInt().coerceIn(0, 100)
                            } else {
                                -1
                            }

                            if (response.code == 206 && totalFileSize != -1L && effectiveDownloadedBytes >= totalFileSize) {
                                Log.i(TAG, "File $fileName already downloaded (size check).")
                                finalizePartFile(partFile, file)
                                return
                            }

                            val finalSize = writeStreamToRandomAccessFile(
                                input = body.byteStream(),
                                file = partFile,
                                downloadedBytes = effectiveDownloadedBytes,
                                totalFileSize = totalFileSize,
                                initialLastProgress = lastProgress,
                                isCancelled = { isStopped },
                                onProgress = { progress ->
                                    lastProgress = progress
                                    updateProgressNotification(fileName, notificationId, progress)
                                }
                            )

                            if (totalFileSize != -1L && finalSize != totalFileSize) {
                                throw IllegalStateException("Download incomplete. Expected $totalFileSize, got $finalSize.")
                            }

                            finalizePartFile(partFile, file)
                            Log.i(TAG, "Download stream finished successfully on attempt $attempt.")
                            return
                        }
                        else -> {
                            throw IllegalStateException("Server error: ${response.code} ${response.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException || e is java.util.concurrent.CancellationException) {
                    throw e
                }
                Log.w(TAG, "Attempt $attempt to download $fileName failed.", e)
                if (attempt == MAX_RETRIES) {
                    throw e
                }
                delay(RETRY_DELAY_MS)
            }
        }
    }

    private fun finalizePartFile(partFile: File, targetFile: File) {
        if (!partFile.exists()) return
        try {
            java.nio.file.Files.move(
                partFile.toPath(),
                targetFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: Exception) {
            if (targetFile.exists()) targetFile.delete()
            if (!partFile.renameTo(targetFile)) {
                throw IllegalStateException("Failed to move downloaded file ${partFile.name} to ${targetFile.name}")
            }
        }
    }

    private fun updateSharedPrefsOnSuccess(
        modelId: String,
        version: String,
        isOneShotModel: Boolean
    ) {
        val sp = applicationContext.getSharedPreferences("models", Context.MODE_PRIVATE)
        Companion.updatePrefsOnSuccess(sp, modelId, version, isOneShotModel)
    }

    private fun updateSharedPrefsOnFailure(
        modelId: String,
        isOneShotModel: Boolean
    ) {
        val sp = applicationContext.getSharedPreferences("models", Context.MODE_PRIVATE)
        Companion.updatePrefsOnFailure(sp, modelId, isOneShotModel)
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
        if (!::notificationBuilder.isInitialized) return
        try {
            notificationBuilder
                .setContentTitle("Downloading $fileName")
                .setContentText("$progress% complete")
                .setProgress(100, progress, false) // Switch to determinate progress.
                .setSilent(true) // Don't make a sound for each update.
            notificationManager.notify(notificationId, notificationBuilder.build())
        } catch (_: Exception) {}
    }

    private fun showSuccessNotification(fileName: String, notificationId: Int) {
        try {
            notificationManager.cancel(notificationId) // remove the ongoing download notification
            val successNotification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.stat_sys_download_done)
                .setContentTitle("Download Complete")
                .setContentText("$fileName has been successfully downloaded.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(notificationId + 1, successNotification) // different ID so it doesn't get stuck
        } catch (_: Exception) {}
    }

    private fun showErrorNotification(fileName: String, notificationId: Int, error: String) {
        try {
            val errorNotification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.stat_notify_error)
                .setContentTitle("Download Failed")
                .setStyle(NotificationCompat.BigTextStyle().bigText("Could not download $fileName: $error"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
            notificationManager.notify(notificationId, errorNotification.build())
        } catch (_: Exception) {}
    }
}
