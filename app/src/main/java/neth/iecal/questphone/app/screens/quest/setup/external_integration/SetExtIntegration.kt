package neth.iecal.questphone.app.screens.quest.setup.external_integration

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import neth.iecal.questphone.R
import neth.iecal.questphone.app.navigation.RootRoute
import neth.iecal.questphone.data.IntegrationId
import nethical.questphone.backend.CommonQuestInfo
import nethical.questphone.backend.GenerateExtIntToken
import nethical.questphone.backend.Supabase
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.data.json
import javax.inject.Inject

@HiltViewModel
class ExternalIntegrationQuestVM @Inject constructor(
    private val questRepository: QuestRepository,
    application: Application
) : AndroidViewModel(application) {

    companion object {
        @Serializable
        data class Token(
            val token: String,
            val createdAt: Long
        )
    }

    private val prefs = application.getSharedPreferences("externalIntToken", Context.MODE_PRIVATE)
    private val FIVE_HOURS_MS = 5 * 60 * 60 * 1000L

    val token = MutableStateFlow<Token?>(null)
    val isLoading = MutableStateFlow(false)
    val isQuestCreated = MutableStateFlow(false)

    init {
        loadSavedToken()
    }

    private fun loadSavedToken() {
        val saved = prefs.getString("token", null)
        if (saved != null) {
            val tToken = json.decodeFromString<Token>(saved)
            if (System.currentTimeMillis() - tToken.createdAt <= FIVE_HOURS_MS) {
                token.value = tToken
            } else {
                prefs.edit(commit = true) { remove("token") }
            }
        }
    }

    fun copyTokenToClipboard(context: Context) {
        token.value?.token?.let { t ->
            val clip = ClipData.newPlainText("token", t)
            (context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                .setPrimaryClip(clip)
            Toast.makeText(context, "Token copied!", Toast.LENGTH_SHORT).show()
        }
    }

    fun refreshQuestForToken() = viewModelScope.launch {
        token.value?.let { t ->
            isLoading.value = true
            val remoteQuests = Supabase.supabase
                .postgrest["quests"]
                .select {
                    filter {
                        eq("user_id", Supabase.supabase.auth.currentUserOrNull()?.id.toString())
                        eq("integration_token", t.token)
                    }
                }
                .decodeList<CommonQuestInfo>()

            remoteQuests.forEach { quest ->
                questRepository.upsertQuest(quest.copy(synced = true))
            }
            isLoading.value = false
            if(remoteQuests.isNotEmpty()){
                prefs.edit(commit = true) { remove("token") }
            }
            isQuestCreated.value = remoteQuests.isNotEmpty()

        }
    }

    fun generateNewToken() = viewModelScope.launch {
        try {
            isLoading.value = true
            val generateExtIntToken = GenerateExtIntToken()
            val result = suspendCancellableCoroutine<Result<String>> { cont ->
                generateExtIntToken.generateToken(Supabase.supabase.auth.currentAccessTokenOrNull().toString()) {
                    cont.resume(it) {}
                }
            }

            result.onSuccess {
                val newToken = Token(it, System.currentTimeMillis())
                prefs.edit(commit = true) { putString("token", json.encodeToString(newToken)) }
                token.value = newToken
            }.onFailure { e ->
                Toast.makeText(getApplication(), e.message ?: "Unknown error", Toast.LENGTH_SHORT).show()
            }
        } finally {
            isLoading.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetExtIntegration(navController: NavHostController, vm: ExternalIntegrationQuestVM = hiltViewModel()) {
    val token by vm.token.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val isQuestCreated by vm.isQuestCreated.collectAsState()
    val context = LocalContext.current

    if(isQuestCreated) {
        AlertDialog(
            onDismissRequest = {
                navController.popBackStack()
            },
            title = {
                Text("Quest Created!", style = MaterialTheme.typography.headlineSmall)
            },
            text = {
                Text("Your quest has been successfully created.")
            },
            confirmButton = {
                Button(onClick = { navController.popBackStack() }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { Text("External Integration", style = MaterialTheme.typography.headlineLarge) },
                actions = {
                    Icon(
                        painter = painterResource(R.drawable.outline_help_24),
                        contentDescription = "Help",
                        modifier = Modifier
                            .clickable { navController.navigate("${RootRoute.IntegrationDocs.route}${IntegrationId.SWIFT_MARK.name}") }
                            .size(30.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.size(8.dp))
            Text(
                "Copy the token below and paste it into the app’s token field. Make sure to keep it safe. The token will automatically expire in 5 hours."
            )

            token?.let {
                Text(
                    text = it.token,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { vm.copyTokenToClipboard(context) }
                )

                Button(onClick = { vm.refreshQuestForToken() }) {
                    Text("Reload Quest")
                }
            }

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(onClick = { vm.generateNewToken() }) {
                    Text(if (token != null) "Regenerate Token" else "Generate Token")
                }
            }
        }
    }
}
