package neth.iecal.questphone.ui.screens.quest

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.first
import neth.iecal.questphone.data.quest.CommonQuestInfo
import neth.iecal.questphone.data.quest.QuestDatabaseProvider
import neth.iecal.questphone.ui.navigation.Screen

@Composable
fun ListAllQuests(navHostController: NavHostController) {
    var questList by remember { mutableStateOf<List<CommonQuestInfo>>(emptyList()) }
    val dao = QuestDatabaseProvider.getInstance(LocalContext.current).questDao()

    var searchQuery by remember { mutableStateOf("") }
    val filteredQuestList = remember(questList, searchQuery) {
        if (searchQuery.isBlank()) {
            questList
        } else {
            questList.filter { item ->
                item.title.contains(searchQuery, ignoreCase = true) ||
                        item.instructions.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    LaunchedEffect(Unit) {
        questList = dao.getAllQuests().first()
    }


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            Button(
                onClick = { navHostController.navigate(Screen.SelectTemplates.route)},
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
                        onValueChange = { searchQuery = it },
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
                                IconButton(onClick = { searchQuery = "" }) {
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
                items(filteredQuestList) { questBase: CommonQuestInfo ->
                    QuestItem(
                        quest = questBase,
                        onClick = {
                            navHostController.navigate(Screen.QuestStats.route + questBase.id)
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

