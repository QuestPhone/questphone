package neth.iecal.questphone.app.screens.quest.setup.external_integration

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import neth.iecal.questphone.R
import neth.iecal.questphone.app.navigation.RootRoute
import neth.iecal.questphone.app.screens.quest.setup.external_integration.ExternalIntegrationQuestVM.Companion.ACTION_QUEST_CREATED
import neth.iecal.questphone.app.screens.quest.setup.swift_mark.SetSwiftMarkViewModelQuest
import neth.iecal.questphone.backed.repositories.QuestRepository
import neth.iecal.questphone.backed.repositories.UserRepository
import neth.iecal.questphone.core.Supabase
import neth.iecal.questphone.data.CommonQuestInfo
import neth.iecal.questphone.data.IntegrationId
import nethical.questphone.backend.GenerateExtIntToken
import nethical.questphone.data.json
import javax.inject.Inject

class QuestCreatedReceiver(val onQuestCreated: () -> Unit) : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: android.content.Intent?) {
        if (intent?.action == ACTION_QUEST_CREATED) {
            onQuestCreated()
        }
    }
}

@HiltViewModel
class ExternalIntegrationQuestVM @Inject constructor(
    private val questRepository: QuestRepository,
    private val userRepository: UserRepository,
    application: Application,
) : AndroidViewModel(application) {

    companion object {
        @Serializable
        data class Token(
            val token: String,
            val createdAt: Long,
        )
        const val ACTION_QUEST_CREATED = "neth.iecal.questphone.ACTION_QUEST_CREATED"
    }

    private val prefs = application.getSharedPreferences("externalIntToken", Context.MODE_PRIVATE)
    private val ONEHOURMS = 1 * 60 * 60 * 1000L

    val token = MutableStateFlow<Token?>(null)
    val isLoading = MutableStateFlow(false)
    val isQuestCreated = MutableStateFlow(false)
    val isProfileSynced = !userRepository.userInfo.needsSync

    init {
        loadSavedToken()
    }

    private fun loadSavedToken() {
        val saved = prefs.getString("token", null)
        if (saved != null) {
            val tToken = json.decodeFromString<Token>(saved)
            if (System.currentTimeMillis() - tToken.createdAt <= ONEHOURMS) {
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
            try {
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

                if (remoteQuests.isNotEmpty()) {
                    onQuestCreated()
                }
            } finally {
                isLoading.value = false
            }
        }
    }

    fun onQuestCreated(){
        prefs.edit(commit = true) { remove("token") }
        token.value = null
        isQuestCreated.value = true
    }

    fun generateNewToken() = viewModelScope.launch {
        try {
            if(!userRepository.userInfo.isAnonymous) {
                isLoading.value = true
                val generateExtIntToken = GenerateExtIntToken()
                val result = suspendCancellableCoroutine<Result<String>> { cont ->
                    generateExtIntToken.generateToken(
                        Supabase.supabase.auth.currentAccessTokenOrNull()!!.toString()
                    ) {
                        cont.resume(it) {}
                    }
                }
                result.onSuccess {
                    val newToken = Token(it, System.currentTimeMillis())
                    prefs.edit(commit = true) { putString("token", json.encodeToString(newToken)) }
                    token.value = newToken
                }.onFailure { e ->
                    Toast.makeText(
                        getApplication(),
                        e.message ?: "Unknown error",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }else{
                Toast.makeText(application,"Please login or use playstore variant", Toast.LENGTH_SHORT).show()
            }
        } finally {
            isLoading.value = false
        }
    }

    fun getTimeRemaining(createdAt: Long): Long {
        return ONEHOURMS - (System.currentTimeMillis() - createdAt)
    }

    fun getProgressPercentage(createdAt: Long): Float {
        val remaining = getTimeRemaining(createdAt)
        return (remaining.toFloat() / ONEHOURMS).coerceIn(0f, 1f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetExtIntegration(navController: NavHostController, vm: ExternalIntegrationQuestVM = hiltViewModel(),swVm : SetSwiftMarkViewModelQuest = hiltViewModel()) {
    val token by vm.token.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val isQuestCreated by vm.isQuestCreated.collectAsState()
    val context = LocalContext.current

    var questJson by remember { mutableStateOf("") }
    val questCreatedReceiver = remember {
        QuestCreatedReceiver {
            vm.onQuestCreated()
            Toast.makeText(context, "Quest created!", Toast.LENGTH_SHORT).show()
        }
    }
    val cScope = rememberCoroutineScope()
    var jsonError by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val filter = android.content.IntentFilter(ACTION_QUEST_CREATED)
        ContextCompat.registerReceiver(
            context,
            questCreatedReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    // Remember to unregister
    DisposableEffect(Unit) {
        onDispose {
            context.unregisterReceiver(questCreatedReceiver)
        }
    }
    if (isQuestCreated) {
        AlertDialog(
            onDismissRequest = { navController.popBackStack() },
            title = {
                Text("Quest Created", style = MaterialTheme.typography.headlineSmall)
            },
            text = {
                Text("Your quest has been successfully created and synced.")
            },
            confirmButton = {
                Button(onClick = { navController.popBackStack() }) {
                    Text("Done")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "External Integration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    IconButton(
                        onClick = { navController.navigate("${RootRoute.IntegrationDocs.route}${IntegrationId.SWIFT_MARK.name}") }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.outline_help_24),
                            contentDescription = "Help"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Instructions
            Text(
                text = "Generate a secure token to link external apps with your quests. Tokens expire in 1 hour.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            // Token Card
            AnimatedVisibility(
                visible = token != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                token?.let { t ->
                    TokenCard(
                        token = t,
                        onCopy = { vm.copyTokenToClipboard(context) },
                        getTimeRemaining = { vm.getTimeRemaining(t.createdAt) },
                        getProgress = { vm.getProgressPercentage(t.createdAt) }
                    )
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp
                        )
                    }
                } else {
                    if (token != null) {
                        Button(
                            onClick = { vm.refreshQuestForToken() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Check for Quest", fontSize = 16.sp)
                        }

                        OutlinedButton(
                            onClick = { vm.generateNewToken() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Generate New Token", fontSize = 16.sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (vm.isProfileSynced) {
                                    vm.generateNewToken()
                                } else {
                                    Toast.makeText(
                                        context, "Sync ongoing, comeback after a few mins!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Generate Token", fontSize = 16.sp)
                        }
                        if(jsonError.isNotEmpty()) {
                            Text(jsonError)
                        }
                    }
                }
            }


            Text(
                "OR",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = questJson,
                    onValueChange = {
                        questJson = it
                    },
                    label = {
                        Text("Quest Json")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                if (questJson.isNotEmpty()) {
                    Spacer(Modifier.size(12.dp))
                    Button(
                        onClick = {
                            cScope.launch {
                                try {
                                    val commonQuestInfo =
                                        json.decodeFromString<CommonQuestInfo>(questJson)
                                    val json = json.decodeFromString<Map<String, String>>(commonQuestInfo.quest_json)
                                    swVm.loadQuestUpperData(commonQuestInfo)

                                    swVm.addQuestToDb(commonQuestInfo.quest_json, commonQuestInfo.reward) {
                                        navController.popBackStack()
                                        Toast.makeText(context,"Quest Creation Success", Toast.LENGTH_SHORT).show()
                                    }
                                }catch (e: Exception){
                                    jsonError = e.message.toString()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Generate Quest From Json", fontSize = 16.sp)
                    }
                }

            }
        }
    }
}

@Composable
fun TokenCard(
    token: ExternalIntegrationQuestVM.Companion.Token,
    onCopy: () -> Unit,
    getTimeRemaining: () -> Long,
    getProgress: () -> Float,
) {
    var timeRemaining by remember { mutableStateOf(getTimeRemaining()) }
    var progress by remember { mutableStateOf(getProgress()) }

    LaunchedEffect(token) {
        while (true) {
            timeRemaining = getTimeRemaining()
            progress = getProgress()
            delay(1000)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with timer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Token",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TimeDisplay(timeRemaining = timeRemaining)
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    progress > 0.5f -> MaterialTheme.colorScheme.primary
                    progress > 0.25f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
            )

            // Token value
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCopy),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = token.token,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Copy hint
            Text(
                text = "Tap token to copy",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TimeDisplay(timeRemaining: Long) {
    val minutes = (timeRemaining / 1000 / 60).toInt()
    val seconds = (timeRemaining / 1000 % 60).toInt()

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.baseline_timer_24),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = String.format("%02d:%02d", minutes, seconds),
            style = MaterialTheme.typography.labelLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}