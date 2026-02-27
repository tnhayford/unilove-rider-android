package com.unilove.rider.model

import java.time.Instant

enum class AppThemeMode {
  SYSTEM,
  LIGHT,
  DARK,
}

enum class RingtoneOption(val title: String) {
  PREMIUM_CHIME("Premium Chime"),
  EXECUTIVE_BELL("Executive Bell"),
  CRISP_ALERT("Crisp Alert"),
}

enum class ShiftStatus {
  ONLINE,
  OFFLINE,
}

enum class RiderLoginMode {
  STAFF,
  GUEST,
}

data class RiderSessionModel(
  val riderId: String,
  val riderName: String,
  val authToken: String,
  val authenticatedAtEpochMs: Long,
  val riderMode: RiderLoginMode = RiderLoginMode.STAFF,
)

data class RiderProfile(
  val riderId: String,
  val riderName: String,
  val shiftStatus: ShiftStatus,
)

enum class DeliveryStatus {
  READY_FOR_PICKUP,
  OUT_FOR_DELIVERY,
  DELIVERED,
  OTHER,
}

data class DispatchOrder(
  val id: String,
  val orderNumber: String,
  val customerName: String,
  val customerPhone: String,
  val address: String,
  val status: DeliveryStatus,
  val subtotalCedis: Double = 0.0,
  val commissionRatePercent: Double = 0.0,
  val commissionCedis: Double = 0.0,
  val createdAt: String,
  val updatedAt: String,
)

data class DeliveryMetrics(
  val deliveriesToday: Int,
  val onTimeRatePercent: Int,
  val averageMinutes: Int,
  val weeklyTrend: List<Int>,
)

enum class IncidentCategory(val label: String) {
  MOTOR_BREAKDOWN("Motor Breakdown"),
  ACCIDENT("Accident"),
  BAD_WEATHER("Bad Weather"),
  ROAD_BLOCK("Road Block"),
  MEDICAL("Medical"),
  SECURITY("Security"),
  CUSTOMER_UNREACHABLE("Customer Unreachable"),
  OTHER("Other"),
}

enum class IncidentSyncStatus {
  SYNCED,
  PENDING,
}

data class IncidentRecord(
  val id: String,
  val orderId: String?,
  val category: IncidentCategory,
  val note: String,
  val location: String?,
  val createdAt: Instant,
  val syncStatus: IncidentSyncStatus,
)

data class IncidentDraft(
  val orderId: String? = null,
  val category: IncidentCategory = IncidentCategory.MOTOR_BREAKDOWN,
  val note: String = "",
  val location: String = "",
)

enum class DispatchListTab {
  NEW_ORDERS,
  ACTIVE_DELIVERIES,
}

sealed interface UiEvent {
  data class Message(val value: String) : UiEvent
  data class Error(val value: String) : UiEvent
}

fun DeliveryStatus.isActive(): Boolean {
  return this == DeliveryStatus.READY_FOR_PICKUP || this == DeliveryStatus.OUT_FOR_DELIVERY
}

fun DeliveryStatus.isNewQueueCandidate(): Boolean {
  return this == DeliveryStatus.READY_FOR_PICKUP
}
