package neth.iecal.questphone.data

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import kotlinx.serialization.Serializable
import neth.iecal.questphone.R
import neth.iecal.questphone.app.screens.quest.setup.ai_snap.SetAiSnap
import neth.iecal.questphone.app.screens.quest.setup.deep_focus.SetDeepFocus
import neth.iecal.questphone.app.screens.quest.setup.external_integration.SetExtIntegration
import neth.iecal.questphone.app.screens.quest.setup.swift_mark.SetSwiftMark
import neth.iecal.questphone.app.screens.quest.view.DeepFocusQuestView
import neth.iecal.questphone.app.screens.quest.view.DeprecatedQuest
import neth.iecal.questphone.app.screens.quest.view.external_integration.ExternalIntegrationQuestView
import neth.iecal.questphone.app.screens.quest.view.SwiftMarkQuestView
import neth.iecal.questphone.app.screens.quest.view.ai_snap.AiSnapQuestView
import nethical.questphone.data.BaseIntegrationId

/**
 * Converts [BaseIntegrationId] into its equivalent [IntegrationId]
 */
fun BaseIntegrationId.toAdv(): IntegrationId{
    return IntegrationId.valueOf(this.name)
}

@Serializable
enum class IntegrationId(
    val icon: Int = R.drawable.baseline_extension_24,
    val label: String = "",
    val description: String = "",
    val setupScreen: @Composable (String?, NavHostController) -> Unit = { id, navController ->
        SetDeepFocus(
            id,
            navController
        )
    },
    val viewScreen: @Composable (CommonQuestInfo) -> Unit = { baseQuest ->
        DeepFocusQuestView(
            baseQuest
        )
    },
    val isLoginRequired: Boolean = false,
    val isDeprecated: Boolean = false,
    val rewardCoins: Int = 5,
    val docLink : String = "https://raw.githubusercontent.com/questphone/docs/refs/heads/main/integration/AiSnap.md"
) {
    /**
     * blocks all apps except a few selected ones.
     * Used in scenarios wherein user wants to block everything except a few necessary apps like phone, messaging, gmail, music etc.
     * Useful when user wants to block access to his phone and focus on some irl task like studying
     */
    DEEP_FOCUS(
        icon = R.drawable.deep_focus_icon,
        label = "Deep Focus",
        description = "Block all apps except the essential ones for a set period, allowing you to stay focused on your work.",
        setupScreen = {id,navController -> SetDeepFocus(id, navController) },
        viewScreen = { baseQuest -> DeepFocusQuestView(baseQuest) },
        docLink = "https://raw.githubusercontent.com/questphone/docs/refs/heads/main/integration/DeepFocus.md"
    ),


    HEALTH_CONNECT(
        icon = R.drawable.health_icon,
        label = "Health Connect",
        description = "Earn coins for performing health related stuff like steps, water intake and more",
        setupScreen = { id,navController ->
        },
        viewScreen = { baseQuest ->
            DeprecatedQuest(baseQuest)
        },
        isDeprecated = true,
        docLink = "https://raw.githubusercontent.com/questphone/docs/refs/heads/main/integration/HealthConnect.md"
    ),

    SWIFT_MARK(
        icon = R.drawable.swift_mark_icon,
        label = "Swift Mark",
        description = "Just mark it as done and earn coins instantly. No verification needed—your honesty is the key!",
        setupScreen = {id,navController ->
            SetSwiftMark(
                id,
                navController
            )
        },
        viewScreen = { baseQuest ->
            SwiftMarkQuestView(
                baseQuest
            )
        },
        rewardCoins = 1,
        docLink = "https://raw.githubusercontent.com/questphone/docs/refs/heads/main/integration/SwiftMark.md"
    ),

    AI_SNAP(
        icon = R.drawable.ai_snap_icon,
        label = "AI verified Snap",
        description = "Complete the task, snap a pic, and let AI verify your progress!",
        setupScreen = {id, navController ->
            SetAiSnap(
                id,
                navController
            )
        },
        viewScreen = { baseQuest ->
            AiSnapQuestView(
                baseQuest
            )
        },
        docLink = "https://raw.githubusercontent.com/questphone/docs/refs/heads/main/integration/AiSnap.md"
    ),
    EXTERNAL_INTEGRATION(
        label = "External Integration",
        description = "Connect questphone to a different app",
        setupScreen = {id,navController ->
            Log.d("id", id.toString())
            if(id==null|| id.isEmpty() == true){
                SetExtIntegration(navController)
            }else{
                SetSwiftMark(
                    id,
                    navController
                )
            }
        },
        viewScreen = {baseQuest ->
            ExternalIntegrationQuestView(baseQuest)
        },
        docLink = "https://raw.githubusercontent.com/questphone/docs/refs/heads/main/integration/externalIntegration.md"
    )
}