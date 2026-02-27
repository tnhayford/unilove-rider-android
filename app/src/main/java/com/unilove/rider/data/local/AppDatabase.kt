package com.unilove.rider.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [QueueCacheEntity::class, PendingExceptionEntity::class, IncidentLogEntity::class],
  version = 3,
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
        )
          .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
          .fallbackToDestructiveMigrationOnDowngrade()
          .build()
          .also { instance = it }
      }
    }

    // Preserve existing rider cache/offline data across v1 -> v2 upgrades.
    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `incident_logs` (
            `id` TEXT NOT NULL,
            `orderId` TEXT,
            `category` TEXT NOT NULL,
            `note` TEXT NOT NULL,
            `location` TEXT,
            `syncStatus` TEXT NOT NULL,
            `createdAtEpochMs` INTEGER NOT NULL,
            `syncedAtEpochMs` INTEGER,
            PRIMARY KEY(`id`)
          )
          """.trimIndent(),
        )
      }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          "ALTER TABLE queue_cache ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'momo'",
        )
        db.execSQL(
          "ALTER TABLE queue_cache ADD COLUMN paymentStatus TEXT NOT NULL DEFAULT 'PENDING'",
        )
        db.execSQL(
          "ALTER TABLE queue_cache ADD COLUMN amountDueCedis REAL NOT NULL DEFAULT 0",
        )
        db.execSQL(
          "ALTER TABLE queue_cache ADD COLUMN requiresCollection INTEGER NOT NULL DEFAULT 0",
        )
      }
    }
  }
}
