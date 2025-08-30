package neth.iecal.questphone.app.screens.game

import android.content.Intent
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import dagger.hilt.android.lifecycle.HiltViewModel
import neth.iecal.questphone.R
import neth.iecal.questphone.ThemePreview
import neth.iecal.questphone.app.navigation.RootRoute
import neth.iecal.questphone.app.screens.components.TopBarActions
import neth.iecal.questphone.app.theme.customThemes.BaseTheme
import neth.iecal.questphone.app.theme.themes
import nethical.questphone.backend.repositories.UserRepository
import nethical.questphone.data.game.Category
import nethical.questphone.data.game.InventoryItem
import javax.inject.Inject

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val userRepository: UserRepository
): ViewModel() {
    var coins = userRepository.coinsState

    var selectedCategory by mutableStateOf<Category>(Category.TOOLS)
        private set

    private val _items = InventoryItem.entries

    val items: List<InventoryItem>
        get() = _items.toList()

    var isSelectingTheme = false
        private set

    var themeList  by mutableStateOf( themes.keys.toList().filter { !userRepository.userInfo.purchasedThemes.contains(it) })
        private set

    fun hasEnoughCoinsToPurchaseItem(item: InventoryItem): Boolean {
        val userCoins = userRepository.userInfo.coins
        return userCoins >= item.price
    }
    fun hasEnoughCoinsToPurchaseItem(item: BaseTheme): Boolean {
        val userCoins = userRepository.userInfo.coins
        return userCoins >= item.price
    }

    fun getItemInventoryCount(item: InventoryItem): Int{
        return userRepository.getInventoryItemCount(item)
    }

    fun getItemsByCategory(category: Category): List<InventoryItem> {
        return items.filter { it.category == category }
    }

    fun selectCategory(category: Category) {
        isSelectingTheme = category == Category.Theme
        selectedCategory = category
    }

    fun makeItemPurchase(item: InventoryItem): Boolean {
        var itemMap = hashMapOf<InventoryItem, Int>()
        itemMap.put(item,1)

        userRepository.addItemsToInventory(itemMap)
        userRepository.useCoins(item.price)
        return true
    }

    fun purchaseTheme(item: BaseTheme){
        userRepository.userInfo.purchasedThemes.add(item.name)
        userRepository.useCoins(item.price)
        themeList = themeList.filter{ it != item.name}
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    navController: NavController,
    viewModel: StoreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedItem by remember { mutableStateOf<InventoryItem?>(null) }
    var selectedThemeItem by remember { mutableStateOf<BaseTheme?>(null) }
    var showSuccessMessage by remember { mutableStateOf<Triple<String, String, ()-> Unit>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coins by viewModel.coins.collectAsState()
    // auto dismiss message
    showSuccessMessage?.let { message ->
        LaunchedEffect(message) {
            val result = snackbarHostState
                .showSnackbar(
                    message = message.first,
                    actionLabel = message.second,
                )
            when (result) {
                SnackbarResult.Dismissed -> {}
                SnackbarResult.ActionPerformed -> {
                    message.third.invoke()
                }
            }
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
                    TopBarActions(coins,0,true,false)
                },
            )
        },
        containerColor = Color.Black,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CategorySelector(
                selectedCategory = viewModel.selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            if(viewModel.isSelectingTheme) {
                StoreThemeList(
                    viewModel.themeList
                ) {
                    selectedThemeItem = themes[it]!!
                }
            }else{
                StoreItemsList(
                    items = viewModel.getItemsByCategory(viewModel.selectedCategory),
                    onItemClick = { selectedItem = it },
                )
            }

            // Purchase dialog
            selectedItem?.let { item ->
                PurchaseDialog(
                    item = item,
                    hasEnoughCoins = viewModel.hasEnoughCoinsToPurchaseItem(item),
                    userCoins = coins,
                    inventoryCount = viewModel.getItemInventoryCount(selectedItem!!),
                    onDismiss = { selectedItem = null },
                    onPurchase = {
                        if (viewModel.makeItemPurchase(item)) {
                            showSuccessMessage = Triple( "Successfully purchased ${item.simpleName}!", "Inventory", {
                                navController.navigate(RootRoute.UserInfo.route)
                            })
                        }
                    }
                )
            }

            selectedThemeItem?.let {item ->
                PurchaseThemeDialog(
                    item = item,
                    hasEnoughCoins = viewModel.hasEnoughCoinsToPurchaseItem(item),
                    userCoins = coins,
                    onDismiss = { selectedThemeItem = null },
                    onPurchase = {
                        viewModel.purchaseTheme(item)
                        showSuccessMessage = Triple( "Successfully purchased ${item.name}!", "Customize", {
                            navController.navigate(RootRoute.UserInfo.route)
                        })
                    },
                    onPreview = {
                        val intent = Intent(context, ThemePreview::class.java)
                        intent.putExtra("themeId",item.name)
                        context.startActivity(intent)
                    }
                )
            }

        }
    }
}

@Composable
private fun CategorySelector(
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
private fun CategoryItem(
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
private fun StoreItemsList(
    items: List<InventoryItem>,
    onItemClick: (InventoryItem) -> Unit,
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
            )
        }
    }
}
@Composable
private fun StoreThemeList(
    themesKeys: List<String>,
    onItemClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(themesKeys) { i ->
            themes[i]?.let {
                StoreThemeCard(
                    theme = it,
                    onClick = { onItemClick(i) },
                )
            }
        }
    }
}

@Composable
private fun StoreItemCard(
    item: InventoryItem,
    onClick: () -> Unit,
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
private fun StoreThemeCard(
    theme: BaseTheme,
    onClick: () -> Unit,
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
                    painter = painterResource(R.drawable.customize),
                    contentDescription = theme.name
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Item details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = theme.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = theme.description,
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
                    text = "${theme.price}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}


@Composable
private fun PurchaseDialog(
    item: InventoryItem,
    hasEnoughCoins: Boolean,
    inventoryCount: Int,
    userCoins: Int,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit
) {
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
                        text = "$inventoryCount",
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
                        onClick = { onPurchase()
                                  onDismiss()},
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

@Composable
private fun PurchaseThemeDialog(
    item: BaseTheme,
    hasEnoughCoins: Boolean,
    userCoins: Int,
    onDismiss: () -> Unit,
    onPurchase: () -> Unit,
    onPreview: () -> Unit
) {
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
                    text = "Purchase ${item.name}?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(
                    onClick = onPreview,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Text("Preview")
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
                        onClick = { onPurchase()
                                  onDismiss()},
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
