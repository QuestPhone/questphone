package neth.iecal.questphone.core.utils

import android.annotation.SuppressLint

@SuppressLint("DefaultLocale")
fun formatNumber(value: Int): String {
    return when {
        value >= 1_000_000_000 -> String.format("%.1fB", value / 1_000_000_000.0)
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
        else -> value.toString()
    }
}
