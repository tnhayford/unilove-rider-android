package com.unilove.rider.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingExceptionDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(row: PendingExceptionEntity)

  @Query("SELECT * FROM pending_exceptions ORDER BY createdAtEpochMs DESC")
  fun observeAll(): Flow<List<PendingExceptionEntity>>

  @Query("SELECT * FROM pending_exceptions ORDER BY createdAtEpochMs ASC")
  suspend fun listAll(): List<PendingExceptionEntity>

  @Query("DELETE FROM pending_exceptions WHERE id = :id")
  suspend fun deleteById(id: String)
}
