package com.unilove.rider.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
  val data: T? = null,
  val error: String? = null,
)

@Serializable
data class QueueOrderDto(
  val id: String,
  @SerialName("orderNumber") val orderNumber: String,
  @SerialName("customerName") val customerName: String,
  @SerialName("customerPhone") val customerPhone: String? = null,
  @SerialName("customerPhoneMasked") val customerPhoneMasked: String? = null,
  val address: String,
  val status: String,
  @SerialName("subtotalCedis") val subtotalCedis: Double? = null,
  @SerialName("commissionRatePercent") val commissionRatePercent: Double? = null,
  @SerialName("commissionCedis") val commissionCedis: Double? = null,
  @SerialName("createdAt") val createdAt: String,
  @SerialName("updatedAt") val updatedAt: String,
)

@Serializable
data class DeliveryVerifyRequest(
  val orderId: String,
  val code: String,
)

@Serializable
data class DeliveryVerifyResponse(
  val success: Boolean,
  val attempts: Int,
  val attemptsRemaining: Int,
)

@Serializable
data class RiderLoginRequest(
  val mode: String = "staff",
  val riderId: String,
  val riderName: String? = null,
  val pin: String? = null,
  val fcmToken: String? = null,
  val deviceId: String? = null,
  val platform: String = "android",
)

@Serializable
data class RiderProfileDto(
  val id: String,
  @SerialName("fullName") val fullName: String,
  val mode: String? = null,
)

@Serializable
data class RiderLoginResponse(
  val token: String,
  @SerialName("expiresInSeconds") val expiresInSeconds: Int,
  val rider: RiderProfileDto,
)

@Serializable
data class RegisterDeviceTokenRequest(
  val fcmToken: String,
  val deviceId: String? = null,
  val platform: String = "android",
)

@Serializable
data class RiderIncidentRequest(
  val orderId: String? = null,
  val reason: String,
  val note: String,
  val location: String? = null,
  val severity: String? = null,
)

@Serializable
data class RiderIncidentResponse(
  val incidentId: String,
  val status: String,
  val severity: String,
)

@Serializable
data class GenericSuccessResponse(
  val ok: Boolean = true,
)

@Serializable
data class RiderShiftUpdateRequest(
  val shiftStatus: String,
  val note: String? = null,
)

@Serializable
data class RiderShiftUpdateResponse(
  val riderId: String,
  val mode: String? = null,
  val shiftStatus: String,
  val updatedAt: String? = null,
)
