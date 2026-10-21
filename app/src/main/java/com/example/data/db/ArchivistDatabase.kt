package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PinnedAppEntity::class, ArchiveLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ArchivistDatabase : RoomDatabase() {

    abstract fun dao(): ArchivistDao

    companion object {
        @Volatile
        private var INSTANCE: ArchivistDatabase? = null

        fun getInstance(context: Context): ArchivistDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ArchivistDatabase::class.java,
                    "archivist_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
