package neth.iecal.questphone.ui.screens.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import neth.iecal.questphone.R
import neth.iecal.questphone.data.game.Category
import neth.iecal.questphone.data.game.InventoryItem
import neth.iecal.questphone.data.game.User
import neth.iecal.questphone.data.game.isBoosterActive
import neth.iecal.questphone.data.game.useInventoryItem
import neth.iecal.questphone.data.game.xpToLevelUp
import neth.iecal.questphone.data.quest.stats.StatsDatabaseProvider
import neth.iecal.questphone.ui.screens.quest.stats.components.HeatMapChart
import neth.iecal.questphone.utils.formatNumber
import neth.iecal.questphone.utils.formatRemainingTime
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserInfoScreen() {
    val context = LocalContext.current

    val totalXpForNextLevel = xpToLevelUp(User.userInfo.level + 1)
    val totalXpForCurrentLevel = xpToLevelUp(User.userInfo.level)
    val xpProgress = (User.userInfo.xp - totalXpForCurrentLevel).toFloat() /
            (totalXpForNextLevel - totalXpForCurrentLevel)

    val selectedInventoryItem = remember { mutableStateOf<InventoryItem?>(null) }

    val successfulDates = remember { mutableStateMapOf<LocalDate, List<String>>() }

    LaunchedEffect (Unit) {
        val dao = StatsDatabaseProvider.getInstance(context).statsDao()

        var stats = dao.getAllStatsForUser().first()

        stats = stats.toMutableList()
        stats.addAll(dao.getAllUnSyncedStats().first())

        stats.forEach {
            val prevList = (successfulDates[it.date]?: emptyList()).toMutableList()
            prevList.add(it.quest_id)
            successfulDates[it.date] = prevList
        }
    }

    if(selectedInventoryItem.value!=null){
        InventoryItemInfoDialog(selectedInventoryItem.value!!, onDismissRequest = {
            selectedInventoryItem.value = null
        })
    }

    Scaffold { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Header
            Text(
                text = "Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Avatar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                val data = if (User.userInfo.has_profile){
                    if(User.userInfo.isAnonymous){
                        val profileFile = File(context.filesDir, "profile")
                        profileFile.absolutePath
                    }else{
                        "https://hplszhlnchhfwngbojnc.supabase.co/storage/v1/object/public/profile/${User.getUserId()}/profile"
                    }
                } else R.drawable.baseline_person_24

                Image(
                    painter = rememberAsyncImagePainter(

                        model = ImageRequest.Builder(LocalContext.current)
                            .data(data)
                            .crossfade(true)
                            .error(R.drawable.baseline_person_24)
                            .placeholder(R.drawable.baseline_person_24)
                            .build(),
                    ),
                    contentDescription = "Avatar",
                    Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Username
            Text(
                "@${User.userInfo.username}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                User.userInfo.full_name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))


            // Level Progress Bar
            Column(
                modifier = Modifier.width(250.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Level ${User.userInfo.level}",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp
                    )
                    Text(
                        "XP: ${User.userInfo.xp} / $totalXpForNextLevel",
                        color = MaterialTheme.colorScheme.outline,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    LinearProgressIndicator(
                        progress = { xpProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Stats Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        value = formatNumber(User.userInfo.coins),
                        label = "coins"
                    )

                    StatItem(
                        value = "${formatNumber(User.userInfo.streak.currentStreak)}d",
                        label = "Streak"
                    )

                    StatItem(
                        value = "${formatNumber(User.userInfo.streak.longestStreak)}d",
                        label = "Top Streak"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            HeatMapChart(
                questMap = successfulDates,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            if(User.userInfo.active_boosts.isNotEmpty()) {

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Active Boosts",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        User.userInfo.active_boosts.forEach { it ->
                            ActiveBoostsItem(it.key, formatRemainingTime(it.value))
                        }
                    }

                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Inventory Section
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Inventory",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    User.userInfo.inventory.forEach { it ->
                        InventoryItemCard(it.key,it.value) { item ->
                            selectedInventoryItem.value = item
                        }

                    }
                }

            }
        }
    }
}



@Composable
fun StatItem(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,

        )
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    quantity: Int,
    onClick: (InventoryItem) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item preview/icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(item.icon),
                    contentDescription = item.simpleName
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Item details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.simpleName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = item.description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Price or actions
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_inventory_24),
                    contentDescription = "Coins",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$quantity",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun ActiveBoostsItem(
    item: InventoryItem,
    remaining: String,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item preview/icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(item.icon),
                    contentDescription = item.simpleName
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Item details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.simpleName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_timer_24),
                        contentDescription = "Remaining Time",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = remaining,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }


        }
    }
}

@Composable
fun InventoryItemInfoDialog(
    reward: InventoryItem,
    onDismissRequest: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {

        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 8.dp,
            modifier = Modifier
                .padding(24.dp)
                .wrapContentSize()
        ) {

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {


                Image(
                    painter = painterResource(reward.icon),
                    contentDescription = reward.simpleName,
                    modifier = Modifier.size(60.dp)
                )

                Text(
                    text = reward.simpleName,
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = reward.description,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Close")

                    }

                    if (reward.isDirectlyUsableFromInventory) {

                        if (reward.category == Category.BOOSTERS && !User.isBoosterActive(reward)) {
                            Button(
                                onClick = {
                                    reward.onUse()
                                    User.useInventoryItem(reward)
                                    onDismissRequest()
                                }) {
                                Text("Use")
                            }
                        }
                    }

                    if (reward.category != Category.BOOSTERS) {
                        Button(
                            onClick = {
                                reward.onUse()
                                User.useInventoryItem(reward)
                                onDismissRequest()
                            }) {
                            Text("Use")
                        }
                    }
                }
            }
        }
    }
}