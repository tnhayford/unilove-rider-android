package com.unilove.rider.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "queue_cache")
data class QueueCacheEntity(
  @PrimaryKey val id: String,
  val orderNumber: String,
  val customerName: String,
  val customerPhoneMasked: String,
  val address: String,
  val status: String,
  val paymentMethod: String = "momo",
  val paymentStatus: String = "PENDING",
  val amountDueCedis: Double = 0.0,
  val requiresCollection: Boolean = false,
  val createdAt: String,
  val updatedAt: String,
  val cachedAtEpochMs: Long,
)
