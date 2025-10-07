package neth.iecal.questphone.app.screens.account

import android.app.Activity
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import neth.iecal.questphone.OnboardActivity
import neth.iecal.questphone.R
import neth.iecal.questphone.app.navigation.RootRoute
import neth.iecal.questphone.app.screens.game.InventoryBox
import neth.iecal.questphone.app.screens.quest.stats.components.HeatMapChart
import neth.iecal.questphone.app.theme.LocalCustomTheme
import neth.iecal.questphone.backed.repositories.UserRepository
import neth.iecal.questphone.backed.triggerProfileSync
import neth.iecal.questphone.backed.triggerQuestSync
import nethical.questphone.backend.BuildConfig
import nethical.questphone.core.core.utils.formatNumber
import nethical.questphone.data.UserInfo
import nethical.questphone.data.game.InventoryItem
import nethical.questphone.data.xpToLevelUp
import java.io.File
import javax.inject.Inject


@HiltViewModel
class UserInfoViewModel @Inject constructor(
    application: Application,
    private val userRepository: UserRepository,
) : AndroidViewModel(application) {

    val userInfo: UserInfo = userRepository.userInfo
    val totalXpForNextLevel = xpToLevelUp(userInfo.level)

    val xpProgress = (userInfo.xp.toFloat() / totalXpForNextLevel )

    val profilePicLink = if (userInfo.has_profile){
        if(userInfo.isAnonymous){
            val profileFile = File(application.filesDir, "profile")
            profileFile.absolutePath
        }else{
            "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/profile/${userRepository.getUserId()}/profile"
        }
    } else null

    init {
        triggerProfileSync(application,false)
    }


    fun logOut(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            userRepository.signOut()
            val activityManager = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.clearApplicationUserData()
            withContext(Dispatchers.Main) {
                onLoggedOut()
            }
        }
    }

    fun onForcePull() {
        if (!userInfo.isAnonymous) {
            triggerProfileSync(application, true)
            triggerQuestSync(application, true)
        }
    }

}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserInfoScreen(viewModel: UserInfoViewModel = hiltViewModel(),navController: NavController) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    Scaffold(containerColor = LocalCustomTheme.current.getRootColorScheme().surface,
        contentWindowInsets = WindowInsets(0),
        ) { innerPadding ->
        Box(Modifier
            .padding(innerPadding)

        ) {
            Box(Modifier.graphicsLayer {
                translationY = -scrollState.value * 0.5f
            }) {
                LocalCustomTheme.current.ThemeObjects(innerPadding)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState)

                ) {
                Row(
                    modifier = Modifier.padding( WindowInsets.statusBarsIgnoringVisibility.asPaddingValues()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Profile Header
                    Text(
                        text = "Profile",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))

                    Icon(
                        painter = painterResource(R.drawable.outline_share_24),
                        contentDescription = "Share Profile",
                        modifier = Modifier.clickable(true, onClick = {
                            if(!viewModel.userInfo.isAnonymous) {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "https://questphone.app/@${viewModel.userInfo.username}"
                                    )
                                }
                                val shareIntent =
                                    Intent.createChooser(sendIntent, "Share Profile via")
                                context.startActivity(shareIntent)
                            }else{
                                Toast.makeText(context,"You're profile is local", Toast.LENGTH_SHORT).show()
                            }
                        })
                    )
                    Spacer(Modifier.size(4.dp))
                    Menu(viewModel.userInfo.isAnonymous, {
                        viewModel.logOut {
                            val intent = Intent(context, OnboardActivity::class.java)
                            context.startActivity(intent)
                            (context as Activity).finish()
                        }
                    },navController,{
                        viewModel.onForcePull()
                    })
                }
                Spacer(Modifier.size(32.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable{
                                navController.navigate(RootRoute.SetupProfile.route)
                            }
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(

                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(viewModel.profilePicLink)
                                    .crossfade(true)
                                    .error(R.drawable.baseline_person_24)
                                    .placeholder(R.drawable.baseline_person_24)
                                    .build(),
                            ),
                            contentDescription = "Avatar",
                            Modifier.fillMaxSize(),
                            colorFilter = if (viewModel.profilePicLink == null)
                                ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                            else
                                null,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "@${viewModel.userInfo.username}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        viewModel.userInfo.username,
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
                                "Level ${viewModel.userInfo.level}",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 12.sp
                            )
                            Text(
                                "XP: ${viewModel.userInfo.xp} / ${viewModel.totalXpForNextLevel}",
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
                                progress = { viewModel.xpProgress },
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Stats Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = LocalCustomTheme.current.getExtraColorScheme().toolBoxContainer,
                                shape = RoundedCornerShape(16.dp),
                            )
                            .alpha(0.7f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                value = formatNumber(viewModel.userInfo.coins),
                                label = "coins"
                            )

                            StatItem(
                                value = "${formatNumber(viewModel.userInfo.streak.currentStreak)}d",
                                label = "Streak"
                            )

                            StatItem(
                                value = "${formatNumber(viewModel.userInfo.streak.longestStreak)}d",
                                label = "Top Streak"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
                HeatMapChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
                Spacer(modifier = Modifier.height(32.dp))

                InventoryBox(navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Menu(isAnonymous: Boolean,onLogout: () -> Unit,navController: NavController,onForcePull: ()->Unit ) {
    var expanded by remember { mutableStateOf(false) }
    var isLogoutInfoVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Default.MoreVert, // This is the 3-dot icon
            contentDescription = "More Options"
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text("Log Out") },
            onClick = {
                isLogoutInfoVisible = true
                expanded = false
                // handle click
            }
        )

        DropdownMenuItem(
            text = { Text("Communicate with us") },
            onClick = {
                navController.navigate(RootRoute.ShowSocials.route)
            }
        )
        DropdownMenuItem(
            text = { Text("Donate") },
            onClick = {
                navController.navigate(RootRoute.ShowSocials.route)
            }
        )

        DropdownMenuItem(
            text = { Text("Open Tutorial") },
            onClick = {
                navController.navigate(RootRoute.ShowTutorials.route)
            }
        )
        DropdownMenuItem(
            text = { Text("Share Crash Log") },
            onClick = {
                shareCrashLog(context)
            }
        )
        DropdownMenuItem(
            text = { Text("Force Pull from servers") },
            onClick = {
                onForcePull()
            }
        )
    }

    if (isLogoutInfoVisible) {
        BasicAlertDialog(
            {
                isLogoutInfoVisible = false
            }

        ) {
            Surface {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .width(IntrinsicSize.Min),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Are you sure you want to log out?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (isAnonymous) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text =
                                "You will lose all your quests, progress, stats and everything if you log out.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = {
                            isLogoutInfoVisible = false
                        }) {
                            Text("Cancel")
                        }
                        TextButton(onClick = {
                            onLogout()
                        }) {
                            Text("Log Out", color = Color.Red)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun StatItem(
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


fun shareCrashLog(context: Context) {
    val logFile = File(context.filesDir, "crash_log.txt")
    if (!logFile.exists()) {
        Toast.makeText(context, "No crash logs found", Toast.LENGTH_SHORT).show()
        return
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", logFile)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Crash Log")
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, "Share Crash Log"))
}
