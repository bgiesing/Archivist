@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ArchiveRuleSettings
import com.example.data.model.InactivityUnit
import com.example.data.model.StorageConditionType
import com.example.data.model.ThemeMode
import com.example.data.model.WorkerIntervalUnit
import com.example.ui.MainViewModel
import com.example.ui.components.DryRunBottomSheet
import com.example.ui.components.FormatUtils
import com.example.ui.theme.ExpressiveShapes
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.dpadFocusable
import com.example.ui.theme.expressiveBounceClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.updateNotificationsEnabled(granted)
    }

    // Dry Run sheet
    uiState.dryRunResult?.let { result ->
        DryRunBottomSheet(
            result = result,
            onDismiss = { viewModel.dismissDryRun() },
            onRunNow = { viewModel.runCheckNow() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Automation & Rules",
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isWideScreen) 24.dp else 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master Switch Card
            MasterSwitchCard(settings = settings, viewModel = viewModel)

            // Quick Actions: Dry Run Preview & Run Check Now
            QuickActionsRow(viewModel = viewModel)

            if (isWideScreen) {
                // Adaptive 2-Column Layout on Tablets, Foldables & Desktops
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        InactivityThresholdCard(settings = settings, viewModel = viewModel)
                        StorageConditionCard(settings = settings, viewModel = viewModel)
                        WorkerIntervalCard(settings = settings, viewModel = viewModel)
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MinimumAppSizeCard(settings = settings, viewModel = viewModel)
                        NotificationsAndSystemAppsCard(
                            settings = settings,
                            viewModel = viewModel,
                            onRequestNotificationPermission = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    viewModel.updateNotificationsEnabled(true)
                                } else {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                        AppearanceThemeCard(settings = settings, viewModel = viewModel)
                        SetupWizardCard(viewModel = viewModel)
                    }
                }
            } else {
                // Single Column for Compact Screens (Phones)
                InactivityThresholdCard(settings = settings, viewModel = viewModel)
                MinimumAppSizeCard(settings = settings, viewModel = viewModel)
                StorageConditionCard(settings = settings, viewModel = viewModel)
                WorkerIntervalCard(settings = settings, viewModel = viewModel)
                AppearanceThemeCard(settings = settings, viewModel = viewModel)
                NotificationsAndSystemAppsCard(
                    settings = settings,
                    viewModel = viewModel,
                    onRequestNotificationPermission = {
                        val hasPerm = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPerm) {
                            viewModel.updateNotificationsEnabled(true)
                        } else {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
                SetupWizardCard(viewModel = viewModel)
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}

@Composable
private fun MasterSwitchCard(
    settings: ArchiveRuleSettings,
    viewModel: MainViewModel
) {
    Card(
        shape = ExpressiveShapes.Card,
        colors = CardDefaults.cardColors(
            containerColor = if (settings.autoArchiveEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .dpadFocusable(ExpressiveShapes.Card)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (settings.autoArchiveEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Automatic App Archiving",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (settings.autoArchiveEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (settings.autoArchiveEnabled) "Active in background via WorkManager" else "Disabled — manual archiving only",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (settings.autoArchiveEnabled) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = settings.autoArchiveEnabled,
                onCheckedChange = { viewModel.updateAutoArchiveEnabled(it) },
                modifier = Modifier
                    .dpadFocusable(RoundedCornerShape(14.dp))
                    .testTag("auto_archive_master_switch")
            )
        }
    }
}

@Composable
private fun QuickActionsRow(
    viewModel: MainViewModel
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = { viewModel.generateDryRun() },
            modifier = Modifier
                .weight(1f)
                .dpadFocusable(ExpressiveShapes.Pill)
                .expressiveBounceClick { viewModel.generateDryRun() }
                .testTag("dry_run_preview_button"),
            shape = ExpressiveShapes.Pill
        ) {
            Icon(imageVector = Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Dry Run Preview")
        }

        Button(
            onClick = { viewModel.runCheckNow() },
            modifier = Modifier
                .weight(1f)
                .dpadFocusable(ExpressiveShapes.Pill)
                .expressiveBounceClick { viewModel.runCheckNow() }
                .testTag("run_check_now_button"),
            shape = ExpressiveShapes.Pill
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Run Check Now")
        }
    }
}

@Composable
private fun InactivityThresholdCard(
    settings: ArchiveRuleSettings,
    viewModel: MainViewModel
) {
    Card(
        shape = ExpressiveShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Inactivity Threshold",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Archive apps unused for more than ${settings.inactivityAmount} ${settings.inactivityUnit.label.lowercase()}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Unit selector (Days, Weeks, Months, Years)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InactivityUnit.entries.forEach { unit ->
                    FilterChip(
                        selected = unit == settings.inactivityUnit,
                        onClick = {
                            val safeAmount = settings.inactivityAmount.coerceIn(1, unit.maxRange)
                            viewModel.updateInactivity(safeAmount, unit)
                        },
                        label = { Text(unit.label) },
                        shape = ExpressiveShapes.Pill,
                        modifier = Modifier.dpadFocusable(ExpressiveShapes.Pill)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val maxSlider = settings.inactivityUnit.maxRange.toFloat()
            Slider(
                value = settings.inactivityAmount.toFloat().coerceIn(1f, maxSlider),
                onValueChange = { viewModel.updateInactivity(it.toInt().coerceAtLeast(1), settings.inactivityUnit) },
                valueRange = 1f..maxSlider,
                steps = (maxSlider - 2).toInt().coerceAtLeast(0),
                modifier = Modifier.dpadFocusable(RoundedCornerShape(8.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1 ${settings.inactivityUnit.label.dropLast(1)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${settings.inactivityAmount} ${settings.inactivityUnit.label}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("${maxSlider.toInt()} ${settings.inactivityUnit.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MinimumAppSizeCard(
    settings: ArchiveRuleSettings,
    viewModel: MainViewModel
) {
    Card(
        shape = ExpressiveShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Minimum App Size",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Only archive apps taking up at least ${FormatUtils.formatBytes(settings.minAppSizeBytes)} (ideal for heavy apps and 10–50+ GB games).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            val presets = listOf(
                25L * 1024 * 1024 to "25 MB",
                50L * 1024 * 1024 to "50 MB",
                100L * 1024 * 1024 to "100 MB",
                250L * 1024 * 1024 to "250 MB",
                500L * 1024 * 1024 to "500 MB",
                1L * 1024 * 1024 * 1024 to "1 GB",
                5L * 1024 * 1024 * 1024 to "5 GB",
                10L * 1024 * 1024 * 1024 to "10 GB",
                25L * 1024 * 1024 * 1024 to "25 GB",
                50L * 1024 * 1024 * 1024 to "50 GB"
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { (bytes, label) ->
                    FilterChip(
                        selected = settings.minAppSizeBytes == bytes,
                        onClick = { viewModel.updateMinSize(bytes) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        shape = ExpressiveShapes.Pill,
                        modifier = Modifier.dpadFocusable(ExpressiveShapes.Pill)
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageConditionCard(
    settings: ArchiveRuleSettings,
    viewModel: MainViewModel
) {
    Card(
        shape = ExpressiveShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Device Storage Condition",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            StorageConditionType.entries.forEach { type ->
                val isSelected = type == settings.storageConditionType
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
                    ),
                    onClick = { viewModel.updateStorageCondition(type, settings.lowStorageThresholdPercent, settings.lowStorageThresholdGB) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .dpadFocusable(RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = type.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = type.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Percentage Slider
            AnimatedVisibility(visible = settings.storageConditionType == StorageConditionType.BELOW_PERCENTAGE) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Trigger when free storage drops below ${settings.lowStorageThresholdPercent}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = settings.lowStorageThresholdPercent.toFloat(),
                        onValueChange = { viewModel.updateStorageCondition(settings.storageConditionType, it.toInt(), settings.lowStorageThresholdGB) },
                        valueRange = 5f..50f,
                        steps = 44,
                        modifier = Modifier.dpadFocusable(RoundedCornerShape(8.dp))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("5% free", style = MaterialTheme.typography.labelSmall)
                        Text("${settings.lowStorageThresholdPercent}% free", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("50% free", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // GB Slider
            AnimatedVisibility(visible = settings.storageConditionType == StorageConditionType.BELOW_FREE_SPACE) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = "Trigger when free storage drops below ${settings.lowStorageThresholdGB} GB",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Slider(
                        value = settings.lowStorageThresholdGB.toFloat(),
                        onValueChange = { viewModel.updateStorageCondition(settings.storageConditionType, settings.lowStorageThresholdPercent, it.toInt()) },
                        valueRange = 2f..50f,
                        steps = 47,
                        modifier = Modifier.dpadFocusable(RoundedCornerShape(8.dp))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("2 GB", style = MaterialTheme.typography.labelSmall)
                        Text("${settings.lowStorageThresholdGB} GB", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("50 GB", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkerIntervalCard(
    settings: ArchiveRuleSettings,
    viewModel: MainViewModel
) {
    Card(
        shape = ExpressiveShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Background Check Interval",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "WorkManager evaluates rules every ${settings.workerIntervalAmount} ${settings.workerIntervalUnit.label.lowercase()}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            val intervals = listOf(
                Pair(6L, WorkerIntervalUnit.HOURS) to "6 Hours",
                Pair(12L, WorkerIntervalUnit.HOURS) to "12 Hours",
                Pair(24L, WorkerIntervalUnit.HOURS) to "24 Hours",
                Pair(3L, WorkerIntervalUnit.DAYS) to "3 Days",
                Pair(7L, WorkerIntervalUnit.DAYS) to "7 Days"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                intervals.take(3).forEach { item ->
                    val (amount, unit) = item.first
                    val label = item.second
                    FilterChip(
                        selected = settings.workerIntervalAmount == amount && settings.workerIntervalUnit == unit,
                        onClick = { viewModel.updateWorkerSchedule(amount, unit) },
                        label = { Text(label, fontSize = 12.sp) },
                        shape = ExpressiveShapes.Pill,
                        modifier = Modifier.dpadFocusable(ExpressiveShapes.Pill)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                intervals.drop(3).forEach { item ->
                    val (amount, unit) = item.first
                    val label = item.second
                    FilterChip(
                        selected = settings.workerIntervalAmount == amount && settings.workerIntervalUnit == unit,
                        onClick = { viewModel.updateWorkerSchedule(amount, unit) },
                        label = { Text(label, fontSize = 12.sp) },
                        shape = ExpressiveShapes.Pill,
                        modifier = Modifier.dpadFocusable(ExpressiveShapes.Pill)
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceThemeCard(
    settings: ArchiveRuleSettings,
    viewModel: MainViewModel
) {
    Card(
        shape = ExpressiveShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Appearance & Theme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.updateThemeMode(mode) },
                        label = { Text(mode.displayName) },
                        shape = ExpressiveShapes.Pill,
                        modifier = Modifier.dpadFocusable(ExpressiveShapes.Pill)
                    )
                }
            }

            // AMOLED switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AMOLED Pure Black", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Use pure #000000 black in Dark Mode", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.amoledBlack,
                    onCheckedChange = { viewModel.updateAmoledBlack(it) },
                    modifier = Modifier.dpadFocusable(RoundedCornerShape(14.dp))
                )
            }
        }
    }
}

@Composable
private fun NotificationsAndSystemAppsCard(
    settings: ArchiveRuleSettings,
    viewModel: MainViewModel,
    onRequestNotificationPermission: () -> Unit
) {
    Card(
        shape = ExpressiveShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Notifications & System Apps",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Notification toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Archive Notifications", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Summary alert when apps are archived in background", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.notificationsEnabled,
                    onCheckedChange = { enable ->
                        if (enable) {
                            onRequestNotificationPermission()
                        } else {
                            viewModel.updateNotificationsEnabled(false)
                        }
                    },
                    modifier = Modifier.dpadFocusable(RoundedCornerShape(14.dp))
                )
            }

            // System Apps toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Include Updated System Apps", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Scan user-updated launchable system apps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.includeSystemApps,
                    onCheckedChange = { viewModel.updateIncludeSystemApps(it) },
                    modifier = Modifier.dpadFocusable(RoundedCornerShape(14.dp))
                )
            }
        }
    }
}

@Composable
private fun SetupWizardCard(
    viewModel: MainViewModel
) {
    Card(
        shape = ExpressiveShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Setup Wizard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Replay the onboarding walkthrough", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(
                onClick = { viewModel.openOnboardingWizard() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.dpadFocusable(RoundedCornerShape(12.dp))
            ) {
                Icon(imageVector = Icons.Default.RocketLaunch, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Replay")
            }
        }
    }
}
