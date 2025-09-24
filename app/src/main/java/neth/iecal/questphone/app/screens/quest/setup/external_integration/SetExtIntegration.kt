package neth.iecal.questphone.app.screens.quest.setup.external_integration

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavHostController
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import neth.iecal.questphone.R
import neth.iecal.questphone.app.navigation.RootRoute
import neth.iecal.questphone.data.IntegrationId
import nethical.questphone.backend.GenerateExtIntToken
import nethical.questphone.backend.Supabase
import nethical.questphone.backend.triggerQuestSync
import nethical.questphone.data.json

@Serializable
private data class Token(
    val token: String,
    val createdAt: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetExtIntegration(navController: NavHostController) {
    var token by remember { mutableStateOf<Token?>(null) }
    val context = LocalContext.current
    val clipboardManager: Clipboard = LocalClipboard.current
    val FIVE_HOURS_MS = 5 * 60 * 60 * 1000L

    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    // Load saved token on first launch
    LaunchedEffect(Unit) {
        val tokenSp = context.getSharedPreferences("externalIntToken", Context.MODE_PRIVATE)
        val saved = tokenSp.getString("token", null)
        if (saved != null) {
            val tToken = json.decodeFromString<Token>(saved)
            if (System.currentTimeMillis() - tToken.createdAt > FIVE_HOURS_MS) {
                tokenSp.edit(commit = true) { remove("token") }
            } else {
                token = tToken
            }
        }
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        text = "External Integration",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                actions = {
                    Icon(
                        painter = painterResource(R.drawable.outline_help_24),
                        contentDescription = "Help",
                        modifier = Modifier
                            .clickable {
                                navController.navigate("${RootRoute.IntegrationDocs.route}${IntegrationId.SWIFT_MARK.name}")
                            }
                            .size(30.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Spacer(Modifier.size(8.dp))
                Text(
                    "Copy the token below and paste it into the app’s token field. Make sure to keep it safe, since it allows the app to create quests for you. The token will automatically expire in 5 hours."
                )

                // Show token if available
                if (token != null) {
                    Text(
                        text = token!!.token,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable {
                            val clipData = ClipData.newPlainText("label", token!!.token)
                            clipboardManager.nativeClipboard.setPrimaryClip(clipData)
                            Toast.makeText(context, "Token copied!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    Text(
                        "Press the below reload button once the new quest has been added by the external provider."
                    )
                    Button(onClick = {
                        triggerQuestSync(context, false)
                    }) {
                        Text("Reload Quest")
                    }
                }
                if (!isLoading) {
                    Button(onClick = {
                        val generateExtIntToken = GenerateExtIntToken()
                        isLoading = true
                        scope.launch {
                            try {
                                generateExtIntToken.generateToken(
                                    Supabase.supabase.auth.currentAccessTokenOrNull()!!.toString()
                                ) {
                                    if (it.isSuccess) {
                                        val newToken = Token(
                                            token = it.getOrNull().toString(),
                                            createdAt = System.currentTimeMillis()
                                        )
                                        context.getSharedPreferences(
                                            "externalIntToken",
                                            Context.MODE_PRIVATE
                                        )
                                            .edit(commit = true) {
                                                putString("token", json.encodeToString(newToken))
                                            }
                                        token = newToken
                                        (context as? Activity)?.runOnUiThread {
                                            Toast.makeText(
                                                context,"Token Generated Successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        (context as? Activity)?.runOnUiThread {
                                            Toast.makeText(
                                                context,
                                                it.exceptionOrNull()?.message ?: "Unknown error",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    isLoading = false
                                }
                            }catch (e: Exception){
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        e.message.toString(),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }) {
                        Text(if (token != null) "Regenerate" else "Generate Token")
                    }
                } else {
                    CircularProgressIndicator()
                }
            }


        }
    }
}