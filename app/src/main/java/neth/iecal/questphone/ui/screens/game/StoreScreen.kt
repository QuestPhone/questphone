package neth.iecal.questphone.ui.screens.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import neth.iecal.questphone.R
import neth.iecal.questphone.data.Category
import neth.iecal.questphone.data.InventoryItem
import nethical.questphone.data.game.User
import nethical.questphone.data.game.addItemsToInventory
import nethical.questphone.data.game.getInventoryItemCount
import nethical.questphone.data.game.useCoins


// View model for the store
class StoreViewModel {
    var coins by mutableIntStateOf(User.userInfo.coins)
    // Currently selected category
    var selectedCategory by mutableStateOf<Category>(Category.BOOSTERS)
        private set

    // Store items - in a real app, this would come from a repository
    private val _items = InventoryItem.entries

    // Public store items getter
    val items: List<InventoryItem>
        get() = _items.toList()

    // Get items by category
    fun getItemsByCategory(category: Category): List<InventoryItem> {
        return items.filter { it.category == category }
    }

    // Select a category
    fun selectCategory(category: Category) {
        selectedCategory = category
    }

    // Purchase an item
    fun purchaseItem(item: InventoryItem): Boolean {
        var itemMap = hashMapOf<InventoryItem, Int>()
        itemMap.put(item,1)
        User.addItemsToInventory(itemMap)
        User.useCoins(item.price)
        coins = User.userInfo.coins
        return true
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    navController: NavController,
    viewModel: StoreViewModel = remember { StoreViewModel() }
) {
    val coroutineScope = rememberCoroutineScope()
    var showPurchaseDialog by remember { mutableStateOf<InventoryItem?>(null) }
    var showSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Success snackbar
    showSuccessMessage?.let { message ->
        LaunchedEffect(message) {
            delay(2000)
            showSuccessMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Store",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White
                ),
                actions = {
                    // Coins display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = Color(0xFF2A2A2A),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.coin_icon),
                            contentDescription = "Coins",
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${viewModel.coins}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category selector
            CategorySelector(
                selectedCategory = viewModel.selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            // Store items
            StoreItemsList(
                items = viewModel.getItemsByCategory(viewModel.selectedCategory),
                onItemClick = { showPurchaseDialog = it },
                onEquipClick = { item ->
//                    if (viewModel.equipItem(item.id)) {
//                        showSuccessMessage = "${item.name} equipped!"
//                    }
                }
            )

            // Purchase dialog
            showPurchaseDialog?.let { item ->
                PurchaseDialog(
                    item = item,
                    onDismiss = { showPurchaseDialog = null },
                    onPurchase = {
                        if (viewModel.purchaseItem(item)) {
                            showSuccessMessage = "Successfully purchased ${item.name}!"
                            showPurchaseDialog = null
                        }
                    }
                )
            }

            // Success message
            AnimatedVisibility(
                visible = showSuccessMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.shadow(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = showSuccessMessage ?: "",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategorySelector(
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit
) {
    val categories = Category.entries.toList()

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(categories) { category ->
            CategoryItem(
                category = category,
                isSelected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
fun CategoryItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) Color.White else Color.Black
    val contentColor = if (isSelected) Color.Black else Color.White

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        onClick = onClick,
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = category.simpleName,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun StoreItemsList(
    items: List<InventoryItem>,
    onItemClick: (InventoryItem) -> Unit,
    onEquipClick: (InventoryItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            StoreItemCard(
                item = item,
                onClick = { onItemClick(item) },
                onEquipClick = { onEquipClick(item) }
            )
        }
    }
}

@Composable
fun StoreItemCard(
    item: InventoryItem,
    onClick: () -> Unit,
    onEquipClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
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
                    color = Color.White
                )

                Text(
                    text = item.description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Price or actions
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.coin_icon),
                    contentDescription = "Coins",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${item.price}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun PurchaseDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit
) {
    val userCoins = User.userInfo.coins
    val hasEnoughCoins = userCoins >= item.price

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1A1A1A)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Purchase ${item.simpleName}?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Item preview
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2A2A2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(item.icon),
                        contentDescription = item.simpleName
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = item.description,
                    fontSize = 16.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Price
                Row(
                    modifier = Modifier
                        .background(
                            color = Color(0xFF2A2A2A),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.coin_icon),
                        contentDescription = "Coins",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${item.price}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(16.dp))

                    Icon(
                        painter = painterResource(R.drawable.baseline_inventory_24),
                        contentDescription = "Inventory",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${User.getInventoryItemCount(item)}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                // Not enough coins message
                if (!hasEnoughCoins) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You need ${item.price - userCoins} more coins!",
                        color = Color(0xFFFF5252),
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onPurchase,
                        modifier = Modifier.weight(1f),
                        enabled = hasEnoughCoins,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE091FF),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF4A4A4A),
                            disabledContentColor = Color.Gray
                        )
                    ) {
                        Text("Purchase")
                    }
                }
            }
        }
    }
}
