package neth.iecal.questphone.app.screens.launcher

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import neth.iecal.questphone.app.theme.customThemes.BaseTheme
import neth.iecal.questphone.homeWidgets
import neth.iecal.questphone.themes
import neth.iecal.questphone.backed.repositories.UserRepository
import nethical.questphone.core.core.utils.toHex
import javax.inject.Inject

enum class CustomizeCategory(val simpleName: String) {
    THEME("Theme"),
    HOME_WIDGET("Home Widget"),
}

@HiltViewModel
class CustomizeViewModel @Inject constructor(
    private val userRepository: UserRepository
): ViewModel() {
    var selectedCategory by mutableStateOf<CustomizeCategory>(CustomizeCategory.THEME)
        private set

    var purchasedThemes by mutableStateOf(userRepository.userInfo.customization_info.purchasedThemes.toList())
    var purchasedWidgets by mutableStateOf(userRepository.userInfo.customization_info.purchasedWidgets.toList())
    var equippedTheme by mutableStateOf(userRepository.userInfo.customization_info.equippedTheme)
    var equippedWidget by mutableStateOf(userRepository.userInfo.customization_info.equippedWidget)

    fun equipTheme(theme: String){
        equippedTheme = theme
        userRepository.userInfo.customization_info.equippedTheme = theme
        userRepository.saveUserInfo()
    }
    fun selectCategory(category: CustomizeCategory){
        selectedCategory = category
    }
    fun equipWidget(key: String){
        equippedWidget = key
        userRepository.userInfo.customization_info.equippedWidget = key
        userRepository.saveUserInfo()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeScreen(
    navController: NavController,
    viewModel: CustomizeViewModel = hiltViewModel(),
    currentTheme: MutableState<BaseTheme>
) {
    val context = LocalContext.current
    var selectedThemeItem by remember { mutableStateOf<BaseTheme?>(null) }
    var selectedWidgetItem by remember { mutableStateOf<String?>(null) }
    var showSuccessMessage by remember { mutableStateOf<Triple<String, String, () -> Unit>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    BuyFromStoreBtn(
                        {
                            navController.navigate(RootRoute.Store.route)
                        },
                        text = "Buy More",
                        description = "Open store"
                    )
                }
                when (viewModel.selectedCategory) {
                    CustomizeCategory.THEME -> {
                        items(viewModel.purchasedThemes) { i ->
                            themes[i]?.let {
                                ItemCard(

                                    isItemEquipped = i == viewModel.equippedTheme,
                                    onClick = { selectedThemeItem = themes[i]!! },
                                    name = it.name,
                                    icon = R.drawable.customize,
                                    description = it.description,
                                )
                            }
                        }
                    }


                    CustomizeCategory.HOME_WIDGET -> {
                        items(viewModel.purchasedWidgets) { i ->
                            homeWidgets[i]?.let {
                                ItemCard(
                                    isItemEquipped = i == viewModel.equippedTheme,
                                    onClick = { selectedWidgetItem = i },
                                    name = i,
                                    icon = R.drawable.customize,
                                    description = "",
                                )
                            }
                        }
                    }
                }
            }
        }

        selectedThemeItem?.let { item ->
            EquipThemeDialog(
                onDismiss = { selectedThemeItem = null },
                onEquip = {
                    viewModel.equipTheme(item.name)
                    showSuccessMessage = Triple("${item.name} equipped", "Go Back", {
                        navController.popBackStack()
                    })
                    currentTheme.value = item
                },
                equippedItem = viewModel.equippedTheme,
                title = item.name,
                description = item.description,
                center = {
                    OutlinedButton(
                        onClick = {
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
            )
        }
        selectedWidgetItem?.let {
            EquipThemeDialog(
                onDismiss = { selectedWidgetItem = null },
                onEquip = {
                    viewModel.equipWidget(it)
                    showSuccessMessage = Triple("$it equipped", "Go Back", {
                        navController.popBackStack()
                    })
                },
                equippedItem = viewModel.equippedWidget,
                title = it,
                description = "",
                center = {
                    homeWidgets[it]?.invoke(Modifier.size(200.dp))
                },
            )
        }

    }
}

@Composable
private fun CategorySelector(
    selectedCategory: CustomizeCategory,
    onCategorySelected: (CustomizeCategory) -> Unit
) {
    val categories = CustomizeCategory.entries.toList()

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
    category: CustomizeCategory,
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
private fun ItemCard(
    name:String,
    icon:Int,
    description: String,
    isItemEquipped: Boolean,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF191919)
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
                    contentDescription = name
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Item details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
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

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isItemEquipped) {
                    Text(
                        text = "In Use",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
@Composable
private fun EquipThemeDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit,
    onEquip: () -> Unit,
    center: @Composable ()-> Unit,
    equippedItem: String
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
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                center.invoke()

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = description,
                    fontSize = 16.sp,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

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
                        onClick = { onEquip()
                            onDismiss()},
                        enabled = equippedItem != title,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE091FF),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF4A4A4A),
                            disabledContentColor = Color.Gray
                        )
                    ) {
                        Text("Equip")
                    }
                }
            }
        }
    }
}

@Composable
private fun BuyFromStoreBtn(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text:String,
    description : String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF191919)
        ),
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    tint = Color.White,
                    contentDescription = text,
                    modifier = Modifier.size(24.dp)
                )

            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = text,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}