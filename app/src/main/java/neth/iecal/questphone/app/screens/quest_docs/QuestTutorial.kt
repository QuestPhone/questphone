package neth.iecal.questphone.app.screens.quest_docs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jeziellago.compose.markdowntext.MarkdownText
import nethical.questphone.backend.fetchUrlContent

@Composable
fun QuestTutorial(url: String){
    val isLoading = remember { mutableStateOf(true) }
    var response  = remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        response.value = fetchUrlContent(url) ?: "Failed to Load [Site]($url)"
        isLoading.value = false
    }
    if(isLoading.value){
        Text("Loading Docs")
    }
    val scrollState = rememberScrollState()

    Scaffold { innerpadding->

        Column(modifier = Modifier.verticalScroll(scrollState).padding(innerpadding).padding(16.dp).fillMaxSize()) {

            MarkdownText(
                markdown = response.value,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}