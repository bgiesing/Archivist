package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.ui.theme.StatusArchived
import com.example.ui.theme.StatusPinned

@Composable
fun AppIconImage(
    drawable: Drawable?,
    isArchived: Boolean,
    isPinned: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val bitmap = remember(drawable) {
        if (drawable == null) null
        else {
            try {
                if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
                    drawable.bitmap
                } else {
                    drawable.toBitmap(
                        width = 96.coerceAtLeast(drawable.intrinsicWidth),
                        height = 96.coerceAtLeast(drawable.intrinsicHeight),
                        config = Bitmap.Config.ARGB_8888
                    )
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(size * 0.22f))
                    .alpha(if (isArchived) 0.5f else 1f)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(size * 0.22f))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        }

        // Overlay status badges
        if (isArchived) {
            Box(
                modifier = Modifier
                    .size(size * 0.42f)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(StatusArchived),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = "Archived",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.26f)
                )
            }
        } else if (isPinned) {
            Box(
                modifier = Modifier
                    .size(size * 0.42f)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(StatusPinned),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.26f)
                )
            }
        }
    }
}
