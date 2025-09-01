package nethical.questphone.core.core.utils

fun String.toHex(): String =
    this.encodeToByteArray().joinToString("") { "%02x".format(it) }

fun String.Companion.fromHex(hex: String): String =
    hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray().decodeToString()
