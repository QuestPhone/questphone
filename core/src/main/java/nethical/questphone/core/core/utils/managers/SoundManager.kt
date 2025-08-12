package nethical.questphone.core.core.utils.managers

import android.content.Context
import android.media.MediaPlayer

class SoundManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playSound(resourceId: Int) {
        try {
            // Release previous MediaPlayer if exists
            mediaPlayer?.release()

            // Create new MediaPlayer and play sound
            mediaPlayer = MediaPlayer.create(context, resourceId)
            mediaPlayer?.setOnCompletionListener { player ->
                player.release()
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
