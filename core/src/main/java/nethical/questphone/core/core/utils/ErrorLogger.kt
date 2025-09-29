package nethical.questphone.core.core.utils

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashLogger(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val logFile = File(context.filesDir, "crash_log.txt")
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val timeStamp =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val writer = FileWriter(logFile, true) // Append mode
            writer.append("\n--- Crash at $timeStamp ---\n")
            writer.append("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})\n")
            val printWriter = PrintWriter(writer)
            throwable.printStackTrace(printWriter)
            printWriter.flush()
            printWriter.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        defaultHandler?.uncaughtException(thread, throwable) // Let the system handle the crash
    }
}