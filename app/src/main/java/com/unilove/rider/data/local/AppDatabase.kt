package com.unilove.rider.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [QueueCacheEntity::class, PendingExceptionEntity::class, IncidentLogEntity::class],
  version = 2,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun queueCacheDao(): QueueCacheDao
  abstract fun pendingExceptionDao(): PendingExceptionDao
  abstract fun incidentLogDao(): IncidentLogDao

  companion object {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase {
      return instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "rider_app.db",
        ).fallbackToDestructiveMigration().build().also { instance = it }
      }
    }
  }
}
