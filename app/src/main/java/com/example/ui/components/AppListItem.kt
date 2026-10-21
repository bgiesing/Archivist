package com.example.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppItem
import com.example.ui.theme.ExpressiveShapes
import com.example.ui.theme.StatusArchived
import com.example.ui.theme.StatusArchivedContainer
import com.example.ui.theme.StatusArchivedOnContainer
import com.example.ui.theme.StatusEligible
import com.example.ui.theme.StatusEligibleContainer
import com.example.ui.theme.StatusEligibleOnContainer
import com.example.ui.theme.StatusPinned
import com.example.ui.theme.dpadFocusable
import com.example.ui.theme.expressiveBounceClick

@Composable
fun AppListItem(
    app: AppItem,
    onSelectApp: (AppItem) -> Unit,
    onTogglePin: (AppItem) -> Unit,
    onArchive: (AppItem) -> Unit,
    onUnarchive: (AppItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(ExpressiveShapes.Card)
            .dpadFocusable(ExpressiveShapes.Card)
            .expressiveBounceClick { onSelectApp(app) }
            .testTag("app_item_${app.packageName}"),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isArchived) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            } else if (app.isPinned) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        shape = ExpressiveShapes.Card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon with subtle expressive squircle
            AppIconImage(
                drawable = app.icon,
                isArchived = app.isArchived,
                isPinned = app.isPinned,
                size = 54.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            // App Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Expressive Pill Badges
                    if (app.isArchived) {
                        Surface(
                            shape = ExpressiveShapes.Pill,
                            color = StatusArchivedContainer
                        ) {
                            Text(
                                text = "ARCHIVED",
                                color = StatusArchivedOnContainer,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    } else if (app.isEligibleForAutoArchive) {
                        Surface(
                            shape = ExpressiveShapes.Pill,
                            color = StatusEligibleContainer
                        ) {
                            Text(
                                text = "ELIGIBLE",
                                color = StatusEligibleOnContainer,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Size breakdown
                val sizeText = if (app.isArchived) {
                    "Preserved data: ${FormatUtils.formatBytes(app.dataBytes)}"
                } else {
                    "${FormatUtils.formatBytes(app.totalBytes)} • Reclaim: ${FormatUtils.formatBytes(app.reclaimableBytes)}"
                }
                Text(
                    text = sizeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                // Last used
                val lastUsedText = if (app.lastUsedTimestamp > 0L) {
                    "Active ${FormatUtils.formatRelativeTime(app.lastUsedTimestamp)}"
                } else if (app.firstInstallTimestamp > 0L) {
                    "Never opened • Installed ${FormatUtils.formatRelativeTime(app.firstInstallTimestamp)}"
                } else {
                    "Never opened"
                }
                Text(
                    text = lastUsedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pin toggle
            IconButton(
                onClick = { onTogglePin(app) },
                modifier = Modifier
                    .size(40.dp)
                    .expressiveBounceClick { onTogglePin(app) }
                    .testTag("pin_button_${app.packageName}")
            ) {
                Icon(
                    imageVector = if (app.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                    contentDescription = if (app.isPinned) "Unpin app" else "Pin app",
                    tint = if (app.isPinned) StatusPinned else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Archive / Unarchive Action
            if (app.isArchived) {
                FilledTonalButton(
                    onClick = { onUnarchive(app) },
                    shape = ExpressiveShapes.Pill,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .expressiveBounceClick { onUnarchive(app) }
                        .testTag("unarchive_button_${app.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Unarchive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Restore",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = { onArchive(app) },
                    shape = ExpressiveShapes.Pill,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier
                        .expressiveBounceClick { onArchive(app) }
                        .testTag("archive_button_${app.packageName}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Archive",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
