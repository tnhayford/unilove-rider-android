package com.unilove.rider.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentLogDao {
  @Query("SELECT * FROM incident_logs ORDER BY createdAtEpochMs DESC")
  fun observeAll(): Flow<List<IncidentLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsert(entry: IncidentLogEntity)

  @Query("UPDATE incident_logs SET syncStatus = :syncStatus, syncedAtEpochMs = :syncedAt WHERE id = :id")
  suspend fun markSynced(id: String, syncStatus: String, syncedAt: Long)
}
