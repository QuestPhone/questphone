package neth.iecal.questphone.app.screens.game

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import neth.iecal.questphone.R
import neth.iecal.questphone.app.screens.account.ActiveBoostsItem
import neth.iecal.questphone.core.utils.managers.executeItem
import neth.iecal.questphone.data.InventoryExecParams
import nethical.questphone.backend.repositories.QuestRepository
import nethical.questphone.backend.repositories.StatsRepository
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.core.core.utils.formatRemainingTime
import nethical.questphone.data.game.StoreCategory
import nethical.questphone.data.game.InventoryItem
import javax.inject.Inject

@HiltViewModel
class InventoryBoxViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val questRepository: QuestRepository,
    private val statsRepository: StatsRepository
): ViewModel(){
    val userInfo = userRepository.userInfo
    val activeBoosts = userRepository.activeBoostsState

    private var _selectedInventoryItem = MutableStateFlow<InventoryItem?>(null)
    val selectedInventoryItem: StateFlow<InventoryItem?> = _selectedInventoryItem.asStateFlow()

    fun isBoosterActive(item: InventoryItem): Boolean{
        return userRepository.isBoosterActive(item)
    }

    fun useSelectedItem(navController: NavController){
        executeItem( selectedInventoryItem.value!!, InventoryExecParams(
            navController = navController,
            userRepository = userRepository,
            questRepository = questRepository,
            statsRepository = statsRepository
        ))

        userRepository.deductFromInventory(selectedInventoryItem.value!!)
        _selectedInventoryItem.value = null
    }
    fun selectedItem(item: InventoryItem?){
        _selectedInventoryItem.value = item
    }
}

@Composable
fun InventoryBox(navController: NavController,viewModel: InventoryBoxViewModel = hiltViewModel()) {
    val selectedInventoryItem by viewModel.selectedInventoryItem.collectAsState()
    val activeBoosts by viewModel.activeBoosts.collectAsState()


    if (selectedInventoryItem != null) {
        InventoryItemInfoDialog(
            selectedInventoryItem!!,
            viewModel.isBoosterActive(selectedInventoryItem!!),
            onUseRequest = {
                viewModel.useSelectedItem(navController)
            },
            onDismissRequest = {
                viewModel.selectedItem(null)
            })
    }

    if(activeBoosts.isNotEmpty()) {
        Text(
            text = "Active Boosts",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            activeBoosts.forEach { it ->
                ActiveBoostsItem(it.key, formatRemainingTime(it.value))
            }
        }
        Spacer(Modifier.padding(bottom = 16.dp))

    }
    Text(
        text = "Inventory",
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        viewModel.userInfo.inventory.forEach { it ->
            InventoryItemCard(it.key, it.value) { item ->
                viewModel.selectedItem(item)
            }

        }
    }
}


@Composable
private fun InventoryItemCard(
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
private fun InventoryItemInfoDialog(
    reward: InventoryItem,
    isBoosterActive:Boolean,
    onUseRequest: () -> Unit = {},
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
                        if (reward.storeCategory != StoreCategory.BOOSTERS || !isBoosterActive) {
                            Button(
                                onClick = {
                                    onUseRequest()
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
}