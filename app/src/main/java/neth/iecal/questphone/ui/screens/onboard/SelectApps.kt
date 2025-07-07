package neth.iecal.questphone.ui.screens.onboard

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import neth.iecal.questphone.data.AppInfo
import neth.iecal.questphone.services.AccessibilityService
import neth.iecal.questphone.services.INTENT_ACTION_REFRESH_APP_BLOCKER
import neth.iecal.questphone.services.ServiceInfo
import neth.iecal.questphone.utils.openAccessibilityServiceScreen
import neth.iecal.questphone.utils.reloadApps
import neth.iecal.questphone.utils.sendRefreshRequest

enum class SelectAppsModes{
    ALLOW_ADD, // only allow adding one app, block removing any apps
    ALLOW_REMOVE, // only allow removing one app, block adding any app
    ALLOW_ADD_AND_REMOVE // no restrictions
}
@Composable
fun SelectApps(selectAppsModes: SelectAppsModes = SelectAppsModes.ALLOW_ADD_AND_REMOVE) {

    // the one special app that was added for ALLOW_ADD or ALLOW_REMOVE mode. is not used for the third mode
    var specialChosenApp by remember { mutableStateOf<String?>(null) }


    val context = LocalContext.current

    val isLoading = remember { mutableStateOf(true) }
    val appsState = remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    val selectedApps = remember { mutableStateListOf<String>() }

    val sp = context.getSharedPreferences("distractions", Context.MODE_PRIVATE)

//    var isAccessibilityServiceEnabled = remember { mutableStateOf(isAccessibilityServiceEnabled(context, AccessibilityService::class.java)) }

    val lifecycleOwner = LocalLifecycleOwner.current

    val isLoadedFromSp = remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    val filteredAppList = remember(appsState.value, searchQuery) {
        if (searchQuery.isBlank()) {
            appsState.value
        } else {
            appsState.value.filter { item ->
                item.name.contains(searchQuery, ignoreCase = true) ||
                        item.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }


    fun saveToBlocker(){
        sp.edit { putStringSet("distracting_apps", selectedApps.toSet()) }
        sendRefreshRequest(context, INTENT_ACTION_REFRESH_APP_BLOCKER)
        ServiceInfo.appBlockerService?.loadLockedApps()
    }

    LaunchedEffect(lifecycleOwner) {
//        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
//            isAccessibilityServiceEnabled.value = isAccessibilityServiceEnabled(context, AccessibilityService::class.java)
//            isNextEnabled.value = isAccessibilityServiceEnabled.value
//        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.DESTROYED) {
            saveToBlocker()
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val apps = sp.getStringSet("distracting_apps", emptySet<String>()) ?: emptySet()
            selectedApps.clear() // Clear to avoid duplicates
            selectedApps.addAll(apps)
//            isNextEnabled.value = selectedApps.isNotEmpty() && isAccessibilityServiceEnabled.value
        }
    }
    LaunchedEffect(selectedApps) {
        if (isLoadedFromSp.value) {
            snapshotFlow { selectedApps.toSet() }.collect { appsSet ->
                saveToBlocker()

            }
        }
    }
    LaunchedEffect(Unit) {
        val distractions = sp.getStringSet("distracting_apps", emptySet<String>()) ?: emptySet()
        selectedApps.addAll(distractions)
        isLoadedFromSp.value = true

        withContext(Dispatchers.IO) {
            reloadApps(context.packageManager, context)
                .onSuccess { apps ->
                    appsState.value = apps
                    isLoading.value = false
                }
                .onFailure { error ->
                    Log.e("SelectApps", "Error loading apps: $error")
                    Toast.makeText(context, "Error loading apps: $error", Toast.LENGTH_SHORT).show()
                    isLoading.value = false
                }
        }
    }

    fun handleAppSelection(packageName: String) {
        when (selectAppsModes) {
            SelectAppsModes.ALLOW_ADD -> {
                // Only allow adding one app, but allow undoing that addition
                if (specialChosenApp == null && !selectedApps.contains(packageName)) {
                    // Allow adding one app
                    specialChosenApp = packageName
                    selectedApps.add(packageName)
                } else if (specialChosenApp == packageName) {
                    // Allow undoing the addition (removing the special app that was added)
                    specialChosenApp = null
                    selectedApps.remove(packageName)
                }
                // Block removing pre-existing apps or adding more apps
            }

            SelectAppsModes.ALLOW_REMOVE -> {
                // Only allow removing one app, but allow undoing that removal
                if (specialChosenApp == null && selectedApps.contains(packageName)) {
                    // Allow removing one app
                    selectedApps.remove(packageName)
                    specialChosenApp = packageName
                } else if (specialChosenApp == packageName) {
                    // Allow undoing the removal (re-adding the special app that was removed)
                    selectedApps.add(packageName)
                    specialChosenApp = null
                }
                // Block adding new apps or removing more apps
            }

            SelectAppsModes.ALLOW_ADD_AND_REMOVE -> {
                // No restrictions - toggle selection
                if (selectedApps.contains(packageName)) {
                    selectedApps.remove(packageName)
                } else {
                    selectedApps.add(packageName)
                }
            }
        }
        saveToBlocker()

    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "Select Distractions",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "These might be social media or games..",
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

//        if(selectedApps.isNotEmpty() && !isAccessibilityServiceEnabled.value){
//            AccessibilityPanel(selectedApps)
//            isNextEnabled.value = false
//        }else {
//            isNextEnabled.value = true
//        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
        ) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search Apps") },
                        placeholder = { Text("Type app name...") },
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
            items(filteredAppList) { (appName, packageName) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            handleAppSelection(packageName)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isSelected = selectedApps.contains(packageName)

                    val isEnabled = when(selectAppsModes) {
                        SelectAppsModes.ALLOW_ADD -> {
                            // Enable unselected apps if no special app chosen yet (can add)
                            // OR enable the special app that was added (can undo)
                            (specialChosenApp == null && !isSelected) || specialChosenApp == packageName
                        }
                        SelectAppsModes.ALLOW_REMOVE -> {
                            // Enable selected apps if no special app chosen yet (can remove)
                            // OR enable the special app that was removed (can undo)
                            (specialChosenApp == null && isSelected) || specialChosenApp == packageName
                        }
                        SelectAppsModes.ALLOW_ADD_AND_REMOVE -> {
                            // Always enabled - no restrictions
                            true
                        }
                    }

                    Checkbox(
                        checked = isSelected,
                        enabled = isEnabled,
                        onCheckedChange = { _ ->
                            handleAppSelection(packageName)
                        }
                    )
                    Text(
                        text = appName,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
@Composable
fun AccessibilityPanel(selectedApps: SnapshotStateList<String>) {
    var showDialog by remember { mutableStateOf(false) }

    // Panel
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "This feature needs Accessibility Service",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF212121), // Dark gray for contrast
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(
                onClick = { showDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6200EE),
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = "Learn More",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Dialog
    if (showDialog) {
        AccessibilityDialog(onDismiss = { showDialog = false },selectedApps)
    }
}

@Composable
fun AccessibilityDialog(onDismiss: () -> Unit, selectedApps: SnapshotStateList<String>) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Why We Need Accessibility",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "This feature uses Accessibility Service to:\n" +
                            "• Block Selected Distracting Apps\n" +
                            "• Double Touch to turn off screen\n" +
                            "\n This is entirely optional. If you do not enable this, BlankPhone will operate as a normal Launcher cum Habit Tracker. " +
                            "All of your data remains completely secure on your device. We never share or store your data to our servers or third party. Our top " +
                            "priority is your data security.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        openAccessibilityServiceScreen(context, AccessibilityService::class.java)
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                ) {
                    Text(
                        text = "Got It",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        onDismiss()
                        selectedApps.clear()

                    },
                    modifier = Modifier
                        .align(Alignment.End)
                ) {
                    Text(
                        text = "Reject",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
