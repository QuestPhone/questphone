package nethical.questphone.data

import java.time.ZonedDateTime

class ScreentimeStat(
    val packageName: String,
    val totalTime: Long,
    val startTimes: List<ZonedDateTime>
)
