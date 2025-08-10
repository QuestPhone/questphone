package neth.iecal.questphone.app.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Navigator {
    var currentScreen by mutableStateOf<String?>(null)
}
