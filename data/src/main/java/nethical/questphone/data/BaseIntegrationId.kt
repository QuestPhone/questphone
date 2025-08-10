package nethical.questphone.data

import kotlinx.serialization.Serializable

/**
IMPORTANT: MAKE SURE TO CHANGE VALUES at [neth.iecal.questphone.data.IntegrationId] AS WEll
 */
@Serializable
enum class BaseIntegrationId(
) {
    DEEP_FOCUS,
    HEALTH_CONNECT,
    SWIFT_MARK,
    AI_SNAP
}