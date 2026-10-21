package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppItem
import com.example.ui.theme.ExpressiveShapes
import com.example.ui.theme.StatusArchivedContainer
import com.example.ui.theme.StatusArchivedOnContainer
import com.example.ui.theme.StatusEligibleContainer
import com.example.ui.theme.StatusEligibleOnContainer
import com.example.ui.theme.StatusPinned
import com.example.ui.theme.dpadFocusable
import com.example.ui.theme.expressiveBounceClick

@Composable
fun AppGridItem(
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
            .height(236.dp)
            .clip(ExpressiveShapes.Card)
            .dpadFocusable(ExpressiveShapes.Card)
            .expressiveBounceClick { onSelectApp(app) }
            .testTag("app_grid_item_${app.packageName}"),
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Status Badge on left, Pin icon on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
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
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { onTogglePin(app) },
                    modifier = Modifier
                        .size(28.dp)
                        .expressiveBounceClick { onTogglePin(app) }
                ) {
                    Icon(
                        imageVector = if (app.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin toggle",
                        tint = if (app.isPinned) StatusPinned else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // App Icon
            AppIconImage(
                drawable = app.icon,
                isArchived = app.isArchived,
                isPinned = app.isPinned,
                size = 52.dp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // App Title
            Text(
                text = app.appName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Storage Size
            val sizeLabel = if (app.isArchived) {
                "Data: ${FormatUtils.formatBytes(app.dataBytes)}"
            } else {
                FormatUtils.formatBytes(app.totalBytes)
            }
            Text(
                text = sizeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            // Last Used
            Text(
                text = FormatUtils.formatRelativeTime(app.lastUsedTimestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Action Button
            if (app.isArchived) {
                FilledTonalButton(
                    onClick = { onUnarchive(app) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .expressiveBounceClick { onUnarchive(app) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = ExpressiveShapes.Pill,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Unarchive,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Restore",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            } else {
                FilledTonalButton(
                    onClick = { onArchive(app) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .expressiveBounceClick { onArchive(app) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = ExpressiveShapes.Pill,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Archive",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
