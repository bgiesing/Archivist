package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArchivistDao {

    // --- Pinned Apps ---
    @Query("SELECT * FROM pinned_apps ORDER BY pinnedAt DESC")
    fun getAllPinnedApps(): Flow<List<PinnedAppEntity>>

    @Query("SELECT packageName FROM pinned_apps")
    suspend fun getPinnedPackageNames(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM pinned_apps WHERE packageName = :packageName)")
    suspend fun isAppPinned(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPinnedApp(app: PinnedAppEntity)

    @Query("DELETE FROM pinned_apps WHERE packageName = :packageName")
    suspend fun deletePinnedApp(packageName: String)

    // --- Activity Logs ---
    @Query("SELECT * FROM archive_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<ArchiveLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ArchiveLogEntity)

    @Query("DELETE FROM archive_logs")
    suspend fun clearAllLogs()
}
