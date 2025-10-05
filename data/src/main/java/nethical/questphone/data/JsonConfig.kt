package nethical.questphone.data

import kotlinx.serialization.json.Json

val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = false
    encodeDefaults = true
    explicitNulls = false
}
