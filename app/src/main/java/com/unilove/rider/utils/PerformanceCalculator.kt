package com.unilove.rider.utils

import com.unilove.rider.model.DeliveryMetrics
import com.unilove.rider.model.DispatchOrder
import com.unilove.rider.model.DeliveryStatus
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

object PerformanceCalculator {
  fun fromOrders(orders: List<DispatchOrder>): DeliveryMetrics {
    val now = Instant.now()
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    val thresholdMinutes = 45L

    val deliveredOrders = orders.filter { it.status == DeliveryStatus.DELIVERED }
    val deliveredToday = deliveredOrders.filter { order ->
      parseInstant(order.updatedAt)
        ?.atZone(zoneId)
        ?.toLocalDate() == today
    }
    val deliveriesToday = deliveredToday.size

    val deliveredDurations = deliveredToday.mapNotNull { order ->
      deliveryDurationMinutes(
        createdAt = parseInstant(order.createdAt),
        endedAt = parseInstant(order.updatedAt),
      )
    }
    val onTimeRatePercent = if (deliveredDurations.isEmpty()) {
      0
    } else {
      val onTimeCount = deliveredDurations.count { it <= thresholdMinutes }
      ((onTimeCount * 100f) / deliveredDurations.size).roundToInt()
    }

    val averageSamples = orders.mapNotNull { order ->
      val createdAt = parseInstant(order.createdAt) ?: return@mapNotNull null
      when (order.status) {
        DeliveryStatus.DELIVERED -> {
          deliveryDurationMinutes(createdAt = createdAt, endedAt = parseInstant(order.updatedAt))
        }
        DeliveryStatus.OUT_FOR_DELIVERY -> {
          deliveryDurationMinutes(createdAt = createdAt, endedAt = now)
        }
        else -> null
      }
    }
    val averageMinutes = if (averageSamples.isEmpty()) {
      0
    } else {
      averageSamples.average().roundToInt()
    }

    val weeklyBuckets = MutableList(7) { 0 }
    deliveredOrders.forEach { order ->
      val completedAt = parseInstant(order.updatedAt) ?: return@forEach
      val day = completedAt.atZone(zoneId).toLocalDate()
      val daysAgo = Duration.between(day.atStartOfDay(zoneId), today.atStartOfDay(zoneId)).toDays().toInt()
      if (daysAgo in 0..6) {
        val index = 6 - daysAgo
        weeklyBuckets[index] = weeklyBuckets[index] + 1
      }
    }

    return DeliveryMetrics(
      deliveriesToday = deliveriesToday,
      onTimeRatePercent = onTimeRatePercent,
      averageMinutes = averageMinutes,
      weeklyTrend = weeklyBuckets,
    )
  }

  private fun parseInstant(raw: String): Instant? {
    return runCatching { Instant.parse(raw.trim()) }.getOrNull()
  }

  private fun deliveryDurationMinutes(createdAt: Instant?, endedAt: Instant?): Int? {
    if (createdAt == null || endedAt == null) return null
    val minutes = Duration.between(createdAt, endedAt).toMinutes()
    if (minutes < 0) return null
    return minutes.toInt()
  }
}
