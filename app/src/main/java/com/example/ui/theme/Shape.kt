package com.example.ui.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Material 3 Expressive shapes:
 * - Asymmetric cards with playful corner radii
 * - Chunky rounded buttons and pills
 * - Scalloped rosette / flower badges for badges and highlights
 */
object ExpressiveShapes {
    val Card = RoundedCornerShape(26.dp)
    val HeroCard = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
    val AsymmetricCard = RoundedCornerShape(topStart = 28.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 28.dp)
    val Button = RoundedCornerShape(22.dp)
    val SmallButton = RoundedCornerShape(16.dp)
    val Pill = RoundedCornerShape(50)
    val Badge = RoundedCornerShape(10.dp)
    val Squircle = RoundedCornerShape(18.dp)
    val SmallSquircle = RoundedCornerShape(14.dp)

    /**
     * 12-point scalloped rosette flower shape, as seen in official Material 3 Expressive badges.
     */
    val Rosette: Shape = GenericShape { size, _ ->
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val outerRadius = minOf(centerX, centerY)
        val innerRadius = outerRadius * 0.82f
        val points = 12
        val step = (2.0 * Math.PI) / points

        for (i in 0 until points) {
            val angle = i * step - Math.PI / 2.0
            val nextAngle = (i + 1) * step - Math.PI / 2.0
            val midAngle = angle + step / 2.0

            val startX = (centerX + outerRadius * cos(angle)).toFloat()
            val startY = (centerY + outerRadius * sin(angle)).toFloat()

            val midX = (centerX + innerRadius * cos(midAngle)).toFloat()
            val midY = (centerY + innerRadius * sin(midAngle)).toFloat()

            val endX = (centerX + outerRadius * cos(nextAngle)).toFloat()
            val endY = (centerY + outerRadius * sin(nextAngle)).toFloat()

            if (i == 0) {
                moveTo(startX, startY)
            }
            quadraticTo(midX, midY, endX, endY)
        }
        close()
    }
}
