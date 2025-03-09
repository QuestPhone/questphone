package launcher.launcher.data.quest.focus

import kotlinx.serialization.Serializable
import launcher.launcher.ui.screens.quest.setup.ExcludeFromReviewDialog


/**
 * The Deep Focus Quest blocks all apps except a few selected ones.
 *
 * @property focusTimeConfig
 * @property unrestrictedApps list of unrestricted apps during the focus session
 * @property nextFocusDurationInMillis
 */
@Serializable
data class DeepFocus(
    var focusTimeConfig: FocusTimeConfig = FocusTimeConfig(),
    var unrestrictedApps: Set<String> = emptySet(),
    @ExcludeFromReviewDialog
    var nextFocusDurationInMillis : Long = 0L
)

