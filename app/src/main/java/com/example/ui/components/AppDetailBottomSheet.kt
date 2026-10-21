package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppItem
import com.example.ui.theme.StatusArchived
import com.example.ui.theme.StatusEligible
import com.example.ui.theme.StatusPinned

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailBottomSheet(
    app: AppItem,
    onDismiss: () -> Unit,
    onTogglePin: (AppItem) -> Unit,
    onArchive: (AppItem) -> Unit,
    onUnarchive: (AppItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header: Icon + Titles
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AppIconImage(
                    drawable = app.icon,
                    isArchived = app.isArchived,
                    isPinned = app.isPinned,
                    size = 64.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (app.isArchived) {
                            BadgeItem("Archived", StatusArchived)
                        } else {
                            BadgeItem("Active", StatusEligible)
                        }

                        if (app.isPinned) {
                            BadgeItem("Pinned", StatusPinned)
                        }

                        if (app.isEligibleForAutoArchive) {
                            BadgeItem("Eligible for Archive", StatusEligible)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Storage Breakdown Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Storage Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Android 15 App Archiving reclaims APK & Cache while preserving user data intact.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Visual storage bar
                    val total = app.totalBytes.coerceAtLeast(1L).toFloat()
                    val apkFraction = (app.appBytes.toFloat() / total).coerceIn(0f, 1f)
                    val cacheFraction = (app.cacheBytes.toFloat() / total).coerceIn(0f, 1f)
                    val dataFraction = (app.dataBytes.toFloat() / total).coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        if (apkFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(apkFraction)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        if (cacheFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(cacheFraction)
                                    .background(MaterialTheme.colorScheme.tertiary)
                            )
                        }
                        if (dataFraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(dataFraction)
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    StorageRow(label = "Total Storage", value = FormatUtils.formatBytes(app.totalBytes), color = MaterialTheme.colorScheme.onSurface)
                    StorageRow(label = "APK / Code (Reclaimable)", value = FormatUtils.formatBytes(app.appBytes), color = MaterialTheme.colorScheme.primary)
                    StorageRow(label = "Cache (Reclaimable)", value = FormatUtils.formatBytes(app.cacheBytes), color = MaterialTheme.colorScheme.tertiary)
                    StorageRow(label = "User Data (Preserved)", value = FormatUtils.formatBytes(app.dataBytes), color = MaterialTheme.colorScheme.secondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Usage & Dates Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "App Usage & Installation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Last Used", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val lastUsedLabel = if (app.lastUsedTimestamp > 0L) {
                            "${FormatUtils.formatFullDate(app.lastUsedTimestamp)} (${FormatUtils.formatRelativeTime(app.lastUsedTimestamp)})"
                        } else {
                            "Never opened"
                        }
                        Text(lastUsedLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Installed On", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(FormatUtils.formatFullDate(app.firstInstallTimestamp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pin Button
                OutlinedButton(
                    onClick = { onTogglePin(app) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (app.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (app.isPinned) "Unpin" else "Pin")
                }

                // Primary Archive/Unarchive Button
                if (app.isArchived) {
                    Button(
                        onClick = {
                            onUnarchive(app)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("modal_unarchive_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore App")
                    }
                } else {
                    Button(
                        onClick = {
                            onArchive(app)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("modal_archive_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Archive Now")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Buttons: Launch & System Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!app.isArchived) {
                    FilledTonalButton(
                        onClick = {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open App")
                    }
                }

                FilledTonalButton(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${app.packageName}")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("App Info")
                }
            }
        }
    }
}

@Composable
private fun StorageRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BadgeItem(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.18f)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
