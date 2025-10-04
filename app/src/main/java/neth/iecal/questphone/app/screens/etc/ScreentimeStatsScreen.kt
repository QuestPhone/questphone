package neth.iecal.questphone.app.screens.etc

import android.app.AppOpsManager
import android.app.DatePickerDialog
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import neth.iecal.questphone.core.utils.UsageStatsHelper
import nethical.questphone.data.ScreentimeStat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreentimeStatsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var usageStats by remember { mutableStateOf<List<ScreentimeStat>>(emptyList()) }
    var totalUsage by remember { mutableStateOf("0m") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var currentDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var earliestDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showMenu by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var ignoredPackages by remember { mutableStateOf(mutableSetOf<String>()) }
    var isLoading by remember { mutableStateOf(true) }

    val prefs = remember { context.getSharedPreferences("digipaws_prefs", Context.MODE_PRIVATE) }

    // Load usage stats
    fun loadUsageStats(date: LocalDate = LocalDate.now()) {
        scope.launch(Dispatchers.IO) {
            isLoading = true
            val usageStatsHelper = UsageStatsHelper(context)
            val list = usageStatsHelper.getForegroundStatsByDay(date).filter {
                it.totalTime >= 180_000 && it.packageName !in ignoredPackages
            }
            val total = list.sumOf { it.totalTime }

            withContext(Dispatchers.Main) {
                usageStats = list
                totalUsage = formatTime(total)
                isLoading = false
            }
        }
    }

    // Find data range
    fun findDataRange() {
        scope.launch(Dispatchers.IO) {
            val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                0, System.currentTimeMillis()
            )
            earliestDate = stats.minOfOrNull { it.firstTimeStamp } ?: System.currentTimeMillis()
            currentDate = System.currentTimeMillis()
            selectedDate = currentDate.coerceAtLeast(earliestDate)
        }
    }

    // Initialize
    LaunchedEffect(Unit) {
        if (!hasUsageStatsPermission(context)) {
            showPermissionDialog = true
        }

        withContext(Dispatchers.IO) {
            getDefaultLauncherPackage(context.packageManager)?.let {
                ignoredPackages.add(it)
            }
            ignoredPackages.addAll(loadIgnoredPackages(prefs))
        }

        loadUsageStats()
        findDataRange()
    }

    // Permission Dialog
    if (showPermissionDialog) {
        PermissionDialog(
            onDismiss = { showPermissionDialog = false },
            onAccept = {
                requestUsageStatsPermission(context)
                showPermissionDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Usage", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ignored Apps") },
                            onClick = {
                                showMenu = false
                                // Launch SelectAppsActivity
                            },
                            leadingIcon = { Icon(painter = painterResource(neth.iecal.questphone.R.drawable.outline_lock_24), null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Apps List
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (usageStats.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No usage data available",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val context = LocalContext.current
                val pm = context.packageManager

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item{
                        // Header Card with Total Usage
                        TotalUsageCard(
                            totalUsage = totalUsage,
                            selectedDate = selectedDate,
                            onDateClick = {
                                showDatePicker(context, selectedDate, earliestDate, currentDate) { newDate ->
                                    selectedDate = newDate
                                    val localDate = Instant.ofEpochMilli(newDate)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                    loadUsageStats(localDate)
                                }
                            }
                        )
                    }
                    // Top 3 Apps Visualization
                    if (usageStats.isNotEmpty()) {
                        item {
                            TopAppsSection(usageStats = usageStats.take(3))
                        }
                    }
                    items(usageStats, key = { it.packageName }) { stat ->
                        val appInfo = pm.getApplicationInfo(stat.packageName, 0)
                        val icon = appInfo.loadIcon(pm).toBitmap().asImageBitmap()

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    bitmap = icon,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        appInfo.loadLabel(pm).toString(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        formatTime(stat.totalTime),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
fun TotalUsageCard(
    totalUsage: String,
    selectedDate: Long,
    onDateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Total Screen Time",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                totalUsage,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onDateClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(formatDate(selectedDate))
            }
        }
    }
}

@Composable
fun TopAppsSection(usageStats: List<ScreentimeStat>) {
    val context = LocalContext.current
    val pm = context.packageManager

    Card(
        modifier = Modifier
            .fillMaxWidth()

    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Most Used",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            val maxTime = usageStats.maxOfOrNull { it.totalTime } ?: 1L

            usageStats.forEachIndexed { index, stat ->
                val appInfo = pm.getApplicationInfo(stat.packageName, 0)
                val icon = appInfo.loadIcon(pm).toBitmap().asImageBitmap()
                val percentage = (stat.totalTime.toFloat() / maxTime.toFloat())

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rank Badge
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when (index) {
                                    0 -> Color(0xFFFFD700) // Gold
                                    1 -> Color(0xFFC0C0C0) // Silver
                                    else -> Color(0xFFCD7F32) // Bronze
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Image(
                        bitmap = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            appInfo.loadLabel(pm).toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(percentage)
                                    .background(
                                        when (index) {
                                            0 -> Color(0xFF2196F3)
                                            1 -> Color(0xFFF44336)
                                            else -> Color(0xFF4CAF50)
                                        }
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        formatTime(stat.totalTime),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun PermissionDialog(onDismiss: () -> Unit, onAccept: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
        title = { Text("Enable Device Usage Access") },
        text = {
            Text(
                "DigiPaws requires device usage access to monitor apps, helping you manage screen time effectively. All data stays securely on your device."
            )
        },
        confirmButton = {
            TextButton(onClick = onAccept) {
                Text("Enable")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Utility Functions

fun formatTime(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "<1m"
    }
}

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(millis)
}

fun loadIgnoredPackages(prefs: SharedPreferences): Set<String> {
    return prefs.getStringSet("ignored_packages", emptySet()) ?: emptySet()
}

fun saveIgnoredPackages(prefs: SharedPreferences, packages: Set<String>) {
    prefs.edit().putStringSet("ignored_packages", packages).apply()
}

fun getDefaultLauncherPackage(pm: PackageManager): String? {
    val intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
    }
    val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolveInfo?.activityInfo?.packageName
}

fun hasUsageStatsPermission(context: Context): Boolean {
    val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        @Suppress("DEPRECATION")
        appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

fun requestUsageStatsPermission(context: Context) {
    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

fun showDatePicker(
    context: Context,
    selectedDate: Long,
    startDate: Long,
    endDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = selectedDate

    val datePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val pickedCalendar = Calendar.getInstance()
            pickedCalendar.set(year, month, dayOfMonth)
            onDateSelected(pickedCalendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    datePicker.datePicker.minDate = startDate
    datePicker.datePicker.maxDate = endDate
    datePicker.show()
}
