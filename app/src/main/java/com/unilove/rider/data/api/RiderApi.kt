package com.unilove.rider.data.api

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface RiderApi {
  @POST("api/rider/auth/login")
  suspend fun login(
    @Body payload: RiderLoginRequest,
  ): ApiEnvelope<RiderLoginResponse>

  @GET("api/rider/queue")
  suspend fun getQueue(
    @Header("Authorization") authorization: String,
    @Query("limit") limit: Int = 120,
  ): ApiEnvelope<List<QueueOrderDto>>

  @POST("api/delivery/verify")
  suspend fun verifyDelivery(
    @Header("Authorization") authorization: String,
    @Body payload: DeliveryVerifyRequest,
  ): ApiEnvelope<DeliveryVerifyResponse>

  @POST("api/rider/devices/token")
  suspend fun registerDeviceToken(
    @Header("Authorization") authorization: String,
    @Body payload: RegisterDeviceTokenRequest,
  ): ApiEnvelope<GenericSuccessResponse>

  @POST("api/rider/incidents")
  suspend fun reportIncident(
    @Header("Authorization") authorization: String,
    @Body payload: RiderIncidentRequest,
  ): ApiEnvelope<RiderIncidentResponse>

  @PATCH("api/rider/shift")
  suspend fun updateShiftStatus(
    @Header("Authorization") authorization: String,
    @Body payload: RiderShiftUpdateRequest,
  ): ApiEnvelope<RiderShiftUpdateResponse>

  @POST("api/rider/auth/logout")
  suspend fun logout(
    @Header("Authorization") authorization: String,
  ): ApiEnvelope<GenericSuccessResponse>
}
