package neth.iecal.questphone.ui.screens.quest.view

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import neth.iecal.questphone.R
import neth.iecal.questphone.ui.screens.components.TopBarActions
import nethical.questphone.core.core.utils.VibrationHelper
import neth.iecal.questphone.data.InventoryItem
import nethical.questphone.data.game.User
import nethical.questphone.data.game.getInventoryItemCount
import nethical.questphone.data.game.useInventoryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseQuestView(topBar: @Composable () -> Unit = {}, startButtonTitle: String = "Start Quest", hideStartQuestBtn: Boolean = false, onQuestStarted: () -> Unit, loadingAnimationDuration: Int = 3000, isFailed: MutableState<Boolean> = mutableStateOf(false), progress:MutableFloatState = mutableFloatStateOf(0f), onQuestCompleted: ()->Unit = {}, isQuestCompleted: MutableState<Boolean> = mutableStateOf(false), questViewBody : @Composable () -> Unit,) {
    val context = LocalContext.current

    val scrollState = rememberScrollState()
    val animatedProgress by animateFloatAsState(
        targetValue = progress.floatValue,
        animationSpec = tween(durationMillis = loadingAnimationDuration, easing = LinearEasing)
    )

    val isUseQuestSkipperDialogVisible = remember { mutableStateOf(false) }


    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            if(isFailed.value){
            drawRect(
                color =  Color(0xFFB00023),
                size = Size(size.width, animatedProgress * size.height),
            )
                }
        }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(title = {}, actions = { TopBarActions(true) })
                     },
            floatingActionButton = {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if(!isQuestCompleted.value && User.getInventoryItemCount(InventoryItem.QUEST_SKIPPER) > 0){
                            Image(
                                painter = painterResource(R.drawable.quest_skipper),
                                contentDescription = "use quest skipper",
                                modifier = Modifier.size(50.dp)
                                    .clickable{
                                        VibrationHelper.vibrate(50)
                                        isUseQuestSkipperDialogVisible.value = true
                                    }
                            )

                        }
                        if(!hideStartQuestBtn) {
                            Spacer(modifier = Modifier.width(15.dp))
                            Button(
                                onClick = {
                                    VibrationHelper.vibrate(100)
                                    onQuestStarted()
                                }
                            ) {
                                Text(text = startButtonTitle)
                            }
                        }
                    }
            },
            floatingActionButtonPosition = FabPosition.End
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(innerPadding),
                horizontalAlignment = Alignment.Start,

                ) {

                if(isUseQuestSkipperDialogVisible.value){
                    Dialog(onDismissRequest = { isUseQuestSkipperDialogVisible.value = false }) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Do you want to use a QUEST SKIPPER to skip this quest for today?",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Available: ${User.getInventoryItemCount(InventoryItem.QUEST_SKIPPER)}",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Row {
                                Button(
                                    onClick = {
                                        VibrationHelper.vibrate(200)
                                        User.useInventoryItem(InventoryItem.QUEST_SKIPPER)
                                        onQuestCompleted()
                                        isUseQuestSkipperDialogVisible.value = false
                                    },
                                ) {
                                    Text("Yes")
                                }
                                Spacer(modifier = Modifier.size(16.dp))

                                Button(
                                    onClick = {
                                        isUseQuestSkipperDialogVisible.value = false
                                    },
                                ) {
                                    Text("No")
                                }
                            }
                        }
                    }

                }


                questViewBody()
            }
        }
    }
}
