package neth.iecal.questphone.app.screens.quest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import neth.iecal.questphone.app.navigation.RootRoute
import neth.iecal.questphone.backed.repositories.QuestRepository
import neth.iecal.questphone.data.CommonQuestInfo
import javax.inject.Inject

@HiltViewModel
class ListAllQuestsViewModel @Inject constructor(
    private val questRepository: QuestRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _questList = MutableStateFlow<List<CommonQuestInfo>>(emptyList())
    var questList: MutableStateFlow<List<CommonQuestInfo>> = MutableStateFlow<List<CommonQuestInfo>>(emptyList())


    init {
        viewModelScope.launch {
            _questList.value = questRepository.getAllQuests().first()
            questList.value = _questList.value
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery

        if (_searchQuery.value.isBlank()) {
            questList.value =  _questList.value
        } else {
           questList.value =  _questList.value.filter { item ->
                item.title.contains(searchQuery.value, ignoreCase = true) ||
                        item.instructions.contains(searchQuery.value, ignoreCase = true)
            }
        }
    }

}

@Composable
fun ListAllQuests(navHostController: NavHostController, viewModel: ListAllQuestsViewModel = hiltViewModel()) {

    val searchQuery by viewModel.searchQuery.collectAsState()
    val questList by viewModel.questList.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Button(
                onClick = { navHostController.navigate(RootRoute.SelectTemplates.route)},
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(text = "Add Quest")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->

        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
            ) {

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("All Quests",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
                }
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        label = { Text("Search Quests") },
                        placeholder = { Text("Type Quest Title...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("")}) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        singleLine = true
                    )
                }
                items(questList) { questBase: CommonQuestInfo ->
                    QuestItem(
                        quest = questBase,
                        onClick = {
                            navHostController.navigate(RootRoute.QuestStats.route + questBase.id)
                        }
                    )
                }
            }

        }
    }

}

@Composable
private fun QuestItem(
    quest: CommonQuestInfo,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Item details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = quest.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    if(quest.is_destroyed){
                        Text(
                            text = "Destroyed",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = if(quest.selected_days.size == 7) "Everyday" else quest.selected_days.joinToString(", ") { it.name },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                }

            }
        }

    }

}

