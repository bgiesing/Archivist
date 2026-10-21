package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ShizukuState
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusEligible

@Composable
fun PermissionStatusBanner(
    shizukuState: ShizukuState,
    hasUsageStats: Boolean,
    onRequestShizuku: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    forceExpanded: Boolean = false,
    showExpandToggle: Boolean = true
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val isShizukuOk = shizukuState is ShizukuState.Authorized
    val isAllOk = isShizukuOk && hasUsageStats
    val effectivelyExpanded = forceExpanded || isExpanded

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isAllOk) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            }
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (showExpandToggle) Modifier.clickable { isExpanded = !isExpanded } else Modifier),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val statusColor = if (isAllOk) StatusEligible else if (isShizukuOk || hasUsageStats) Color(0xFFF59E0B) else StatusDanger
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        BoxContent(
                            icon = if (isAllOk) Icons.Default.CheckCircle else Icons.Default.Warning,
                            tint = statusColor
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (isAllOk) "Privileged Archiving Active" else "Privileges & Access Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAllOk) {
                                val version = (shizukuState as? ShizukuState.Authorized)?.version ?: 13
                                "Shizuku v$version Connected • Usage Stats Active"
                            } else {
                                "Tap to configure Shizuku and Usage Access"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recheck Status",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (showExpandToggle) {
                        IconButton(onClick = { isExpanded = !isExpanded }) {
                            Icon(
                                imageVector = if (effectivelyExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Expand details",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = effectivelyExpanded || !isAllOk) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    // Shizuku section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = if (isShizukuOk) StatusEligible else StatusDanger,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Shizuku Service",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = when (shizukuState) {
                                        is ShizukuState.Authorized -> "Authorized (v${shizukuState.version})"
                                        is ShizukuState.RunningUnauthorized -> "Running • Permission needed"
                                        is ShizukuState.Dead -> "Service dead / stopped"
                                        is ShizukuState.NotInstalled -> "App not installed"
                                        is ShizukuState.Checking -> "Checking..."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        when (shizukuState) {
                            is ShizukuState.RunningUnauthorized -> {
                                FilledTonalButton(
                                    onClick = onRequestShizuku,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Authorize")
                                }
                            }
                            is ShizukuState.NotInstalled, is ShizukuState.Dead -> {
                                OutlinedButton(
                                    onClick = {
                                        openShizukuOrPlayStore(context)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Open Shizuku")
                                }
                            }
                            is ShizukuState.Authorized -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Ready",
                                    tint = StatusEligible,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            else -> Unit
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Usage Stats Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueryStats,
                                contentDescription = null,
                                tint = if (hasUsageStats) StatusEligible else Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Usage Stats Access",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (hasUsageStats) "Active • Tracking inactive apps" else "Needed to calculate last opened date",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (!hasUsageStats) {
                            FilledTonalButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Grant")
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Ready",
                                tint = StatusEligible,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxContent(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    androidx.compose.foundation.layout.Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun openShizukuOrPlayStore(context: Context) {
    val pm = context.packageManager
    val launchIntent = pm.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        ?: pm.getLaunchIntentForPackage("dev.rikka.shizuku")
    if (launchIntent != null) {
        context.startActivity(launchIntent)
    } else {
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api"))
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(marketIntent)
        } catch (_: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api"))
            webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(webIntent)
        }
    }
}
