package com.example.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adds an expressive animated focus border when an element is focused via D-pad
 * (Google TV / Android TV remote) or hardware keyboard navigation (Chromebook).
 */
fun Modifier.dpadFocusable(
    shape: Shape = RoundedCornerShape(16.dp),
    borderWidth: Dp = 2.5.dp,
    focusedBorderColor: Color? = null
): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = focusedBorderColor ?: MaterialTheme.colorScheme.primary
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "DpadFocusBorder"
    )

    this
        .onFocusChanged { isFocused = it.isFocused }
        .focusable()
        .border(
            width = borderWidth,
            color = borderColor.copy(alpha = borderAlpha),
            shape = shape
        )
}
