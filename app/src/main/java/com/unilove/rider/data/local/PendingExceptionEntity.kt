package com.unilove.rider.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_exceptions")
data class PendingExceptionEntity(
  @PrimaryKey val id: String,
  val orderId: String,
  val riderId: String,
  val reason: String,
  val note: String,
  val createdAtEpochMs: Long,
)
