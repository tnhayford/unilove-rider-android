package com.unilove.rider.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "incident_logs")
data class IncidentLogEntity(
  @PrimaryKey val id: String,
  val orderId: String?,
  val category: String,
  val note: String,
  val location: String?,
  val syncStatus: String,
  val createdAtEpochMs: Long,
  val syncedAtEpochMs: Long?,
)
