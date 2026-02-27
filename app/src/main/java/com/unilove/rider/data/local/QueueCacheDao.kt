package com.unilove.rider.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QueueCacheDao {
  @Query("SELECT * FROM queue_cache ORDER BY createdAt ASC")
  fun observeQueue(): Flow<List<QueueCacheEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAll(rows: List<QueueCacheEntity>)

  @Query("DELETE FROM queue_cache WHERE id NOT IN (:ids)")
  suspend fun deleteAllExcept(ids: List<String>)

  @Query("DELETE FROM queue_cache")
  suspend fun clearAll()
}
