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
import neth.iecal.questphone.HOME_WIDGET_PRICE
import neth.iecal.questphone.R
import neth.iecal.questphone.ThemePreview
import neth.iecal.questphone.app.navigation.RootRoute
import neth.iecal.questphone.app.screens.components.TopBarActions
import neth.iecal.questphone.app.theme.customThemes.BaseTheme
import neth.iecal.questphone.homeWidgets
import neth.iecal.questphone.themes
import neth.iecal.questphone.backed.repositories.UserRepository
import nethical.questphone.core.core.utils.toHex
import nethical.questphone.data.game.InventoryItem
import nethical.questphone.data.game.StoreCategory
import javax.inject.Inject

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val userRepository: UserRepository
): ViewModel() {
    var coins = userRepository.coinsState

    var selectedStoreCategory by mutableStateOf<StoreCategory>(StoreCategory.TOOLS)
        private set

    private val _items = InventoryItem.entries

    val items: List<InventoryItem>
        get() = _items.toList()


    var themeList  by mutableStateOf( themes.keys.toList().filter { !userRepository.userInfo.customization_info.purchasedThemes.contains(it) })
        private set

    var homeWidgetList by mutableStateOf(homeWidgets.toList().filter { !userRepository.userInfo.customization_info.purchasedWidgets.contains(it.first) })

    fun hasEnoughCoins(price:Int): Boolean {
        val userCoins = userRepository.userInfo.coins
        return userCoins >= price
    }
    fun hasEnoughCoinsToPurchaseItem(item: InventoryItem): Boolean {
        return hasEnoughCoins(item.price)
    }

    fun hasEnoughCoinsToPurchaseItem(item: BaseTheme): Boolean {
        return hasEnoughCoins(item.price)
    }

    fun getItemInventoryCount(item: InventoryItem): Int{
        return userRepository.getInventoryItemCount(item)
    }

    fun getItemsByCategory(storeCategory: StoreCategory): List<InventoryItem> {
        return items.filter { it.storeCategory == storeCategory }
    }

    fun selectCategory(storeCategory: StoreCategory) {
        selectedStoreCategory = storeCategory
    }

    fun makeItemPurchase(item: InventoryItem): Boolean {
        var itemMap = hashMapOf<InventoryItem, Int>()
        itemMap.put(item,1)

        userRepository.addItemsToInventory(itemMap)
        userRepository.useCoins(item.price)
        return true
    }

    fun purchaseTheme(item: BaseTheme){
        userRepository.userInfo.customization_info.purchasedThemes.add(item.name)
        userRepository.useCoins(item.price)
        themeList = themeList.filter{ it != item.name}
    }

    fun purchaseWidget(item: String){
        userRepository.userInfo.customization_info.purchasedWidgets.add(item)
        userRepository.useCoins(HOME_WIDGET_PRICE)
        homeWidgetList = homeWidgetList.filter { item != it.first }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    navController: NavController,
    viewModel: StoreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedInventoryItem by remember { mutableStateOf<InventoryItem?>(null) }
    var selectedWidgetItem by remember { mutableStateOf<Pair<String, @Composable (Modifier)-> Unit>?>(null) }
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
                selectedStoreCategory = viewModel.selectedStoreCategory,
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when(viewModel.selectedStoreCategory){
                    StoreCategory.THEMES -> {
                        items(viewModel.themeList) { i ->
                            themes[i]?.let {
                                StoreItemCard(
                                    onClick = { selectedThemeItem = themes[i]!! },
                                    title = it.name,
                                    description = it.description,
                                    icon = R.drawable.customize,
                                    price = it.price,
                                )
                            }
                        }
                    }
                    StoreCategory.HOME_WIDGET -> {
                        items(viewModel.homeWidgetList) { i ->
                            StoreItemCard(
                                onClick = { selectedWidgetItem = i },
                                title = i.first,
                                description = "",
                                icon = R.drawable.customize,
                                price = HOME_WIDGET_PRICE,
                            )
                        }
                    }

                    else -> {
                        items(viewModel.getItemsByCategory(viewModel.selectedStoreCategory)) { item ->
                            StoreItemCard(
                                onClick = { selectedInventoryItem = item },
                                title = item.simpleName,
                                description = item.description,
                                icon = item.icon,
                                price = item.price,
                            )
                        }
                    }
                }
            }


            // Purchase dialog
            selectedInventoryItem?.let { item ->
                PurchaseDialog(
                    hasEnoughCoins = viewModel.hasEnoughCoinsToPurchaseItem(item),
                    userCoins = coins,
                    inventoryCount = viewModel.getItemInventoryCount(selectedInventoryItem!!),
                    onDismiss = { selectedInventoryItem = null },
                    onPurchase = {
                        if (viewModel.makeItemPurchase(item)) {
                            showSuccessMessage =
                                Triple("Successfully purchased ${item.simpleName}!", "Inventory", {
                                    navController.navigate(RootRoute.UserInfo.route)
                                })
                        }
                    },
                    title = item.simpleName,
                    description = item.description,
                    price = item.price,
                    center = {
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

                    }
                )
            }

            selectedWidgetItem?.let {
                PurchaseDialog(
                    hasEnoughCoins = viewModel.hasEnoughCoins(HOME_WIDGET_PRICE),
                    userCoins = coins,
                    onDismiss = { selectedWidgetItem = null },
                    onPurchase = {
                        viewModel.purchaseWidget(it.first)
                        showSuccessMessage =
                            Triple("Successfully purchased ${it.first}!", "Customize", {
                                navController.navigate(RootRoute.Customize.route)
                            })
                    },
                    center = {
                        it.second.invoke(Modifier.size(200.dp))
                    },
                    title = it.first,
                    description = "Displays right above your quest list on home screen",
                    price = HOME_WIDGET_PRICE,

                    inventoryCount = 0
                )
            }
            selectedThemeItem?.let {item ->
                PurchaseDialog(
                    hasEnoughCoins = viewModel.hasEnoughCoinsToPurchaseItem(item),
                    userCoins = coins,
                    onDismiss = { selectedThemeItem = null },
                    onPurchase = {
                        viewModel.purchaseTheme(item)
                        showSuccessMessage =
                            Triple("Successfully purchased ${item.name}!", "Customize", {
                                navController.navigate(RootRoute.Customize.route)
                            })
                    },
                    center = {
                        OutlinedButton(
                            onClick = {
                                selectedThemeItem = null
                                val intent = Intent(context, ThemePreview::class.java)
                                intent.putExtra("themeId", item.name)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color.Gray)
                        ) {
                            Text("Preview")
                        }
                        if(item.docLink!=null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = {
                                    selectedThemeItem = null
                                    navController.navigate("${RootRoute.DocViewer.route}${item.docLink!!.toHex()}")
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.White
                                ),
                                border = BorderStroke(1.dp, Color.Gray)
                            ) {
                                Text("Read Perks")
                            }
                        }
                    },
                    title = item.name,
                    description = item.description,
                    price = item.price,

                    inventoryCount = 0
                )
            }

        }
    }
}

@Composable
private fun CategorySelector(
    selectedStoreCategory: StoreCategory,
    onCategorySelected: (StoreCategory) -> Unit
) {
    val categories = StoreCategory.entries.toList()

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(categories) { category ->
            CategoryItem(
                storeCategory = category,
                isSelected = category == selectedStoreCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
private fun CategoryItem(
    storeCategory: StoreCategory,
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
                text = storeCategory.simpleName,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp
            )
        }
    }
}
@Composable
private fun StoreItemCard(
    title: String,
    description : String,
    icon: Int,
    price: Int,
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
                    painter = painterResource(icon),
                    contentDescription = title
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Item details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = description,
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
                    text = "$price",
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
    title: String,
    description: String,
    price: Int,
    center: @Composable ()-> Unit,
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
                    text = "Purchase ${title}?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                center()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = description,
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
                        text = "$price",
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
                        text = "You need ${price - userCoins} more coins!",
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
