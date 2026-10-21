package com.example.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1.0 -> String.format(Locale.getDefault(), "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.getDefault(), "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.getDefault(), "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun formatRelativeTime(timestamp: Long): String {
        if (timestamp <= 0L) return "Never used"
        val now = System.currentTimeMillis()
        val diff = (now - timestamp).coerceAtLeast(0L)

        val minutes = diff / (60 * 1000L)
        val hours = diff / (60 * 60 * 1000L)
        val days = diff / (24 * 60 * 60 * 1000L)
        val weeks = days / 7
        val months = days / 30
        val years = days / 365

        return when {
            minutes < 5 -> "Just now"
            hours < 1 -> "$minutes min ago"
            hours < 24 -> "$hours hr ago"
            days == 1L -> "Yesterday"
            days < 7 -> "$days days ago"
            weeks < 4 -> "$weeks wk ago"
            months < 12 -> "$months mo ago"
            else -> "$years yr ago"
        }
    }

    fun formatFullDate(timestamp: Long): String {
        if (timestamp <= 0L) return "Unknown"
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
