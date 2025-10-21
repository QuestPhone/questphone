package neth.iecal.questphone.app.screens.quest.setup

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import dagger.hilt.android.lifecycle.HiltViewModel
import neth.iecal.questphone.R
import neth.iecal.questphone.app.screens.etc.DocumentViewerScreen
import neth.iecal.questphone.backed.repositories.UserRepository
import neth.iecal.questphone.data.IntegrationId
import nethical.questphone.core.core.utils.VibrationHelper
import javax.inject.Inject

@HiltViewModel
class SetIntegrationVM @Inject constructor(
    userRepository: UserRepository
): ViewModel() {
    val isAnonymous = userRepository.userInfo.isAnonymous
}

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("UnrememberedMutableState")
@Composable
fun SetIntegration(navController: NavHostController, viewModel: SetIntegrationVM = hiltViewModel()) {

    val showLoginRequiredDialog = remember { mutableStateOf(false) }
    Scaffold()
    { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)

        ) {
            if (showLoginRequiredDialog.value) {
                AlertDialog(
                    onDismissRequest = { showLoginRequiredDialog.value = false },
                    title = {
                        Text(text = "Login Required For this quest")
                    },
                    text = {
                        Text("This quest can only be performed by signed up users to prevent abuse. Please Logout and try again!")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showLoginRequiredDialog.value = false
                        }) {
                            Text("Okay")
                        }
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                val currentDocLink = remember { mutableStateOf<String?>(null) }

                BackHandler(currentDocLink.value != null) {
                    currentDocLink.value = null
                }
                if (currentDocLink.value != null) {
                    DocumentViewerScreen(url = currentDocLink.value!!)
                } else {

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {

                        item{
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                text = "Set Integration",
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                        items(IntegrationId.entries.filter { !it.isDeprecated }) { item ->

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .combinedClickable(
                                        onClick = {
//                                            navController.navigate("${item.name}/ntg") {
//                                                popUpTo(navController.currentDestination?.route ?: "") {
//                                                    inclusive = true
//                                                }
//                                            }
                                            if(!item.isLoginRequired || !viewModel.isAnonymous){
                                                navController.navigate("${item.name}/ntg") {
                                                    popUpTo(navController.currentDestination?.route ?: "") {
                                                        inclusive = true
                                                    }
                                                }
                                            }else{
                                                showLoginRequiredDialog.value = true
                                            }
                                        },
                                        onLongClick = {
                                            VibrationHelper.vibrate(100)
                                            currentDocLink.value = item.docLink
                                        }
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .size(60.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(item.icon),
                                            contentDescription = item.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize()
                                        )

                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = item.label,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                        )

                                        Text(
                                            text = item.description,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

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
                                            text = "${item.rewardCoins}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }
    }

}
