package neth.iecal.questphone.ui.screens.launcher.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppItem(name: String, packageName: String, onAppPressed: (String) -> Unit) {
    val context = LocalContext.current
    Text(
        text = name,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .combinedClickable(onClick = {
                onAppPressed(packageName)
            },
                onLongClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = "package:${packageName}".toUri()
                    }
                    context.startActivity(intent)
                })
    )
}