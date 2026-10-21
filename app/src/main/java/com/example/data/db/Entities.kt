package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_apps")
data class PinnedAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val pinnedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "archive_logs")
data class ArchiveLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val actionType: String, // "ARCHIVE", "UNARCHIVE", "AUTO_ARCHIVE"
    val bytesReclaimed: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean,
    val message: String? = null
)
