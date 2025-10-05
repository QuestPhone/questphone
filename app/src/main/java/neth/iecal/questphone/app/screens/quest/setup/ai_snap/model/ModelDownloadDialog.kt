package neth.iecal.questphone.app.screens.quest.setup.ai_snap.model

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import neth.iecal.questphone.BuildConfig
import neth.iecal.questphone.backed.fetchUrlContent
import neth.iecal.questphone.core.workers.FileDownloadWorker

@kotlinx.serialization.Serializable
data class DownloadableModel(
    val sizeMB: Int,
    val accuracy: String,
    val id: String,
    val url: String,
    val version: String,
    val isRecommended: Boolean = false,
    val isOnline:Boolean = false
)

@Composable
fun ModelDownloadDialog(
    allowSkipping: Boolean = true,
    modelDownloadDialogVisible: MutableState<Boolean>,
) {
    val context = LocalContext.current
    val sp = context.getSharedPreferences("models", Context.MODE_PRIVATE)
    var currentModel by remember { mutableStateOf<String?>(null) }
    var isModelDownloading by remember { mutableStateOf(sp.contains("downloading")) }
    var modelStates by remember { mutableStateOf<List<MutableState<DownloadableModel>>>(emptyList()) }

    LaunchedEffect(Unit) {
        currentModel = sp.getString("selected_one_shot_model", null)

        if(currentModel==null && !BuildConfig.IS_FDROID) {
            currentModel = "online"
            sp.edit(commit = true) {
                putString("selected_one_shot_model", "online")
            }
        }
        if (currentModel == null) modelDownloadDialogVisible.value = true

        val json = fetchUrlContent("https://raw.githubusercontent.com/QuestPhone/models/refs/heads/main/model_data.json")
        try {
            val parsed = Json.decodeFromString(
                ListSerializer(DownloadableModel.serializer()), json!!
            )
            modelStates = parsed.map { mutableStateOf(it) }

            // Check if any downloaded model has a version mismatch
            for (modelState in modelStates) {
                val model = modelState.value
                val localVersion = sp.getString("version_${model.id}", null)
                val isDownloaded = sp.getBoolean("is_downloaded_${model.id}", false)
                if (isDownloaded && localVersion != model.version) {
                    // Trigger re-download
                    context.deleteFile("${model.id}.onnx")
                    sp.edit {
                        remove("is_downloaded_${model.id}")
                        remove("version_${model.id}")
                    }
                    Toast.makeText(context, "Model ${model.id} is outdated. Re-downloading...", Toast.LENGTH_SHORT).show()

                    sp.edit(commit = true) { putString("downloading", model.id) }
                    isModelDownloading = true
                    val inputData = Data.Builder()
                        .putString("url", model.url)
                        .putString("fileName", "${model.id}.onnx")
                        .putString("model_id", model.id)
                        .putString("model_version", model.version)
                        .build()

                    val downloadWork = OneTimeWorkRequestBuilder<FileDownloadWorker>()
                        .setInputData(inputData)
                        .build()

                    WorkManager.getInstance(context).enqueue(downloadWork)
                }
            }

        } catch (e: Exception) {
            Log.d("Failed to load model", e.message.toString())
            Toast.makeText(context, "Failed to load model list", Toast.LENGTH_SHORT).show()
        }
    }

    fun onDismiss() {
        if (currentModel == null && !allowSkipping && !isModelDownloading) {
            Toast.makeText(context, "Please select a model to perform this quest", Toast.LENGTH_SHORT).show()
        }
        modelDownloadDialogVisible.value = false
    }

    if (modelDownloadDialogVisible.value) {
        Dialog(onDismissRequest = { onDismiss() }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Download Model", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp))
                    Text(
                        "Hey F-Droid users! Offline AISNAP quests don’t work here properly. Please use online mode for now (download the app from playstore), and if you know how to help us fix this, we’d love your support! I'm new to ai/ml and it would be great if you could help fine tune a model.",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    if (isModelDownloading) {
                        Text(
                            "Please wait while the model downloads.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    } else {
                        if(modelStates.isEmpty()){
                            Text("Loading Available Models")
                        }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f, false)) {
                            items(modelStates) { modelState ->
                                val model = modelState.value
                                val isDownloaded = sp.getBoolean("is_downloaded_${model.id}", false) || model.isOnline
                                val localVersion = sp.getString("version_${model.id}", null)
                                val isSelected = currentModel == model.id
                                val needsUpdate = (localVersion != model.version && isDownloaded)

                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                                            isDownloaded -> MaterialTheme.colorScheme.surfaceVariant
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        }
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isModelDownloading) {
                                            if(model.isOnline && neth.iecal.questphone.BuildConfig.IS_FDROID){
                                                Toast.makeText(context,"Please download the app from playstore to access this",
                                                    Toast.LENGTH_SHORT).show()
                                                return@clickable
                                            }
                                            if ((isDownloaded && !needsUpdate)|| model.isOnline ) {
                                                sp.edit(commit = true) {
                                                    putString("selected_one_shot_model", model.id)
                                                }
                                                currentModel = model.id
                                            } else {
                                                sp.edit(commit = true) {
                                                    putString("downloading", model.id)
                                                }

                                                isModelDownloading = true
                                                val inputData = Data.Builder()
                                                    .putString(FileDownloadWorker.KEY_URL, model.url)
                                                    .putString(FileDownloadWorker.KEY_FILE_NAME, "${model.id}.onnx")
                                                    .putString(FileDownloadWorker.KEY_MODEL_ID, model.id)
                                                    .putString(FileDownloadWorker.KEY_MODEL_VERSION, model.version)
                                                    .putBoolean(FileDownloadWorker.KEY_IS_ONE_SHOT, true)
                                                    .build()

                                                val workRequest = OneTimeWorkRequestBuilder<FileDownloadWorker>()
                                                    .setInputData(inputData)
                                                    .setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                                                    .build()

                                                WorkManager.getInstance(context).enqueue(workRequest)

                                                Toast.makeText(context, "Download Started", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                if (model.isRecommended) {
                                                    Text("Recommended", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Text(
                                                    model.id,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = if (isSelected)
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                    else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    "Accuracy: ${model.accuracy}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (isSelected)
                                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                                Text(
                                                    "Version: ${model.version}" + if (needsUpdate) " (Update Available)" else "",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error.takeIf { needsUpdate }
                                                        ?: MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                if (isDownloaded) {
                                                    TextButton(
                                                        onClick = {
                                                            context.deleteFile("${model.id}.onnx")
                                                            sp.edit {
                                                                remove("is_downloaded_${model.id}")
                                                                remove("version_${model.id}")
                                                            }
                                                            Toast.makeText(context, "Model deleted", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    ) {
                                                        Text("Delete", style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }else {
                                                    Text(
                                                        "${model.sizeMB}mb",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (isSelected)
                                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { onDismiss() }) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}
