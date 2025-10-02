package neth.iecal.questphone.core.utils.reminder.streak

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import neth.iecal.questphone.R
import neth.iecal.questphone.app.screens.game.DialogState
import neth.iecal.questphone.app.screens.game.RewardDialogInfo
import neth.iecal.questphone.backed.repositories.UserRepository
import nethical.questphone.core.core.utils.daysSince
import java.io.BufferedReader
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.pow

fun generateStreakReminder(userRepository: UserRepository, context: Context) {
    val streakInfo = userRepository.userInfo.streak
    val irregularity = calculateIrregularity(streakInfo.streakFailureHistory)
    val gap = daysSince(streakInfo.lastCompletedDate, DayOfWeek.entries.toSet())

    val failureHistory = streakInfo.streakFailureHistory.values.toList()
    val prevStreak = failureHistory.getOrElse(failureHistory.size - 1) { 0 }       // most recent failed streak
    val lastToLast = failureHistory.getOrElse(failureHistory.size - 2) { 0 }       // one before last

    Log.d("irregularity", irregularity.toString())
    Log.d("last streak", prevStreak.toString())
    Log.d("last to last streak", lastToLast.toString())
    Log.d("days since last streak", gap.toString())

    fun sendNextNotif(prefKey: String, filePath: String,title:String?=null,positive:Boolean = true) {
        val prefs = context.getSharedPreferences(prefKey, Context.MODE_PRIVATE)
        val lastSent = prefs.getInt("last_sent", -1)
        var index = lastSent + 1

        var line = getLine(context, filePath, index)
        if (line == null) { // restart from beginning
            index = 0
            line = getLine(context, filePath, index)
        }

        prefs.edit(commit = true) { putInt("last_sent", index) }
        if (line != null) {
            if(title==null){
            sendCustomNotifcation("Streak", "Streak", context, line,positive)
            }else{
                sendCustomNotifcationWithTitle("Streak", "Streak", context, line,title,positive)

            }
        }
    }

    when(RewardDialogInfo.currentDialog) {
        DialogState.STREAK_FREEZER_USED -> {
            val streakFreezersUsed = RewardDialogInfo.streakFreezerReturn?.streakFreezersUsed ?: 1
            if (streakFreezersUsed < 7) {
                sendNextNotif(
                    "streak_freezers_notif",
                    "streak/streak_freezers.txt",
                    "$streakFreezersUsed Streak Freezers Used",
                    true
                )
            }else{
                sendNextNotif("long_break_resumed", "streak/streak_resumed/long_break.txt", positive = true)
            }
            return
        }
        else -> {}
    }

    // Case 1: user failed streak (gap >= 1, current streak = 0)
    if (gap >= 1 && streakInfo.currentStreak == 0) {
        val level = when (prevStreak) {
            in 0..7 -> "short_streak"
            in 8..20 -> "mid_streak"
            else -> "long_streak"
        }
        sendNextNotif("${level}_failed", "streak/streak_failed/$level.txt", positive = false)
        return
    }

    // Case 2: user resumed streak (current = 1)
    if (streakInfo.currentStreak == 1) {
        if (irregularity > 3) {
            sendNextNotif("streak_irregular_notif", "streak/irregular.txt",positive = false)
            return
        }
        val level = when (lastToLast) {
            in 0..7 -> "short_break"
            in 8..20 -> "mid_break"
            else -> "long_break"
        }
        sendNextNotif("${level}_resumed", "streak/streak_resumed/$level.txt", positive = true)
        return
    }

    // Case 3: user is continuing streak normally
    if (streakInfo.currentStreak > 1) {
        sendNextNotif("streak_up_notif", "streak/streak_up.txt",positive = true)
    }
}

fun getLine(context: Context,file:String, line: Int): String? {
    return try {
        val inputStream = context.assets.open(file)
        val reader = BufferedReader(inputStream.reader())
        val lines = reader.readLines()

        if (lines.isNotEmpty()) {
            lines[line]
        } else {
            null // File is empty
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun calculateIrregularity(streakBreaks: Map<String, Int>): Double {
    if (streakBreaks.isEmpty()) return 0.0

    // Sort dates from oldest to newest
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val sortedEntries = streakBreaks.entries.sortedBy { LocalDate.parse(it.key, formatter) }

    var weightedSum = 0.0
    var totalWeight = 0.0
    val decayFactor = 0.9 // recent streaks count more

    // We assign higher weight to more recent streaks
    for ((index, entry) in sortedEntries.withIndex()) {
        val daysBroken = entry.value.toDouble()
        val weight = decayFactor.pow(sortedEntries.size - index - 1) // more recent gets higher weight
        weightedSum += daysBroken * weight
        totalWeight += weight
    }

    // Weighted average of broken streaks
    val irregularityScore = if (totalWeight > 0) weightedSum / totalWeight else 0.0
    return irregularityScore
}


fun sendCustomNotifcation(
    channelId: String,
    channelName: String,
    context: Context,
    message: String,
    positive: Boolean
) {
    // Create notification channel (required for Android 8+)

    val soundUri = if(positive) "android.resource://${context.packageName}/raw/game_streak_up".toUri() else "android.resource://${context.packageName}/raw/streap_fail".toUri()
    val attributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .build()
    val channel = NotificationChannel(
        channelId,
        channelName,
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Notifications to motivate users"
        enableLights(true)
        lightColor = 0xFFFFA500.toInt() // bright orange light
        enableVibration(true)
        setSound(soundUri, attributes)

    }
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)

    // Build the notification
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground) // minimal icon
        .setContentTitle(message) // single catchy text
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setColor(0xFF00000.toInt())
        .setAutoCancel(true)
        .build()

    // Show the notification
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        return
    }
    NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
}

fun sendCustomNotifcationWithTitle(
    channelId: String,
    channelName: String,
    context: Context,
    message: String,
    title: String,
    positive: Boolean
) {
    val soundUri = if(positive) "android.resource://${context.packageName}/${R.raw.game_streak_up}".toUri() else "android.resource://${context.packageName}/${R.raw.streap_fail}".toUri()
    val attributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .build()
    val channel = NotificationChannel(
        channelId,
        channelName,
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        description = "Notifications to motivate users"
        enableLights(true)
        lightColor = 0xFFFFA500.toInt() // bright orange light
        enableVibration(true)
        setSound(soundUri, attributes)
    }
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)

    // Build the notification
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_launcher_foreground) // minimal icon
        .setContentTitle(title) // single catchy text
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setColor(0xFF00000.toInt())
        .setAutoCancel(true)
        .build()

    // Show the notification
    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        return
    }
    NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
}
