package com.unilove.rider.data.repo

import com.unilove.rider.BuildConfig
import com.unilove.rider.data.api.DeliveryVerifyRequest
import com.unilove.rider.data.api.NetworkModule
import com.unilove.rider.data.api.RegisterDeviceTokenRequest
import com.unilove.rider.data.api.RiderIncidentRequest
import com.unilove.rider.data.api.RiderLoginRequest
import com.unilove.rider.data.api.RiderShiftUpdateRequest
import com.unilove.rider.data.local.IncidentLogDao
import com.unilove.rider.data.local.IncidentLogEntity
import com.unilove.rider.data.local.PendingExceptionDao
import com.unilove.rider.data.local.PendingExceptionEntity
import com.unilove.rider.data.local.QueueCacheDao
import com.unilove.rider.data.local.QueueCacheEntity
import com.unilove.rider.data.local.SessionStore
import com.unilove.rider.domain.repository.AuthRepository
import com.unilove.rider.domain.repository.DispatchRepository
import com.unilove.rider.domain.repository.IncidentRepository
import com.unilove.rider.domain.repository.PerformanceRepository
import com.unilove.rider.domain.repository.SettingsRepository
import com.unilove.rider.model.AppThemeMode
import com.unilove.rider.model.DeliveryMetrics
import com.unilove.rider.model.DeliveryStatus
import com.unilove.rider.model.DispatchOrder
import com.unilove.rider.model.IncidentCategory
import com.unilove.rider.model.IncidentDraft
import com.unilove.rider.model.IncidentRecord
import com.unilove.rider.model.IncidentSyncStatus
import com.unilove.rider.model.RingtoneOption
import com.unilove.rider.model.RiderLoginMode
import com.unilove.rider.model.RiderSessionModel
import com.unilove.rider.model.ShiftStatus
import com.unilove.rider.utils.PerformanceCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.Instant
import java.util.UUID

class StaffAppRepository(
  private val sessionStore: SessionStore,
  private val queueCacheDao: QueueCacheDao,
  private val pendingExceptionDao: PendingExceptionDao,
  private val incidentLogDao: IncidentLogDao,
  private val baseUrl: String = BuildConfig.BASE_URL,
) :
  AuthRepository,
  DispatchRepository,
  IncidentRepository,
  PerformanceRepository,
  SettingsRepository {

  private fun maskPhone(value: String?): String {
    val digitsOnly = value.orEmpty().filter { it.isDigit() }
    if (digitsOnly.length < 4) return "****"
    return "${digitsOnly.take(2)}******${digitsOnly.takeLast(2)}"
  }

  override suspend fun login(
    riderId: String,
    pin: String,
    mode: RiderLoginMode,
    riderName: String?,
  ): Result<RiderSessionModel> {
    return runCatching {
      val normalizedRiderId = riderId.trim()
      val normalizedPin = pin.trim()
      val modeValue = if (mode == RiderLoginMode.GUEST) "guest" else "staff"
      val api = NetworkModule.riderApi(baseUrl)
      val response = api.login(
        RiderLoginRequest(
          mode = modeValue,
          riderId = normalizedRiderId.ifBlank { "guest" },
          riderName = riderName?.trim()?.takeIf { it.isNotBlank() },
          pin = if (mode == RiderLoginMode.GUEST) null else normalizedPin,
          platform = "android",
        ),
      )
      val data = response.data ?: throw IllegalStateException(response.error ?: "Unable to sign in")
      val resolvedMode = data.rider.mode.toRiderLoginMode(defaultMode = mode)
      val model = RiderSessionModel(
        riderId = data.rider.id,
        riderName = data.rider.fullName,
        authToken = data.token,
        authenticatedAtEpochMs = System.currentTimeMillis(),
        riderMode = resolvedMode,
      )
      sessionStore.saveSession(
        riderId = model.riderId,
        riderName = model.riderName,
        authToken = model.authToken,
        riderMode = model.riderMode,
      )
      if (resolvedMode == RiderLoginMode.STAFF && normalizedRiderId.isNotBlank() && normalizedPin.isNotBlank()) {
        sessionStore.saveOfflinePin(riderId = normalizedRiderId, pin = normalizedPin)
      }
      sessionStore.saveShiftStatus(ShiftStatus.OFFLINE)
      model
    }.mapKnownErrors(defaultMessage = "Unable to sign in. Please try again.")
  }

  override suspend fun loginOffline(
    riderId: String,
    pin: String,
    mode: RiderLoginMode,
  ): Result<RiderSessionModel> {
    return runCatching {
      if (mode == RiderLoginMode.GUEST) {
        throw IllegalStateException("Guest login requires internet connection.")
      }
      val cached = sessionStore.getSessionModel()
        ?: throw IllegalStateException("Offline login unavailable. Connect once to authenticate.")
      if (cached.riderMode == RiderLoginMode.GUEST) {
        throw IllegalStateException("Offline login is unavailable for guest rider mode.")
      }
      if (!sessionStore.canLoginOffline(riderId, pin)) {
        throw IllegalStateException("Invalid offline PIN for this rider.")
      }
      sessionStore.saveShiftStatus(ShiftStatus.OFFLINE)
      cached
    }
  }

  override suspend fun logout() {
    val session = sessionStore.getSessionModel()
    if (session == null) {
      sessionStore.saveShiftStatus(ShiftStatus.OFFLINE)
      sessionStore.clear()
      return
    }
    logout(session).getOrElse { throw it }
  }

  override fun observeOrders(): Flow<List<DispatchOrder>> {
    return queueCacheDao.observeQueue().map { rows -> rows.map { it.toDispatchOrder() } }
  }

  override suspend fun refreshOrders(session: RiderSessionModel): Result<List<DispatchOrder>> {
    return runCatching {
      val api = NetworkModule.riderApi(baseUrl)
      val response = api.getQueue(
        authorization = "Bearer ${session.authToken}",
        limit = 120,
      )
      val data = response.data ?: throw IllegalStateException(response.error ?: "Unable to load dispatch queue")
      val mapped = data.map { dto ->
        val phone = (dto.customerPhoneMasked ?: maskPhone(dto.customerPhone)).trim()
        DispatchOrder(
          id = dto.id,
          orderNumber = dto.orderNumber,
          customerName = dto.customerName,
          customerPhone = phone,
          address = dto.address,
          status = dto.status.toDeliveryStatus(),
          subtotalCedis = dto.subtotalCedis ?: 0.0,
          commissionRatePercent = dto.commissionRatePercent ?: 0.0,
          commissionCedis = dto.commissionCedis ?: 0.0,
          createdAt = dto.createdAt,
          updatedAt = dto.updatedAt,
        )
      }

      val now = System.currentTimeMillis()
      val cacheRows = mapped.map { order ->
        QueueCacheEntity(
          id = order.id,
          orderNumber = order.orderNumber,
          customerName = order.customerName,
          customerPhoneMasked = maskPhone(order.customerPhone),
          address = order.address,
          status = order.status.name,
          createdAt = order.createdAt,
          updatedAt = order.updatedAt,
          cachedAtEpochMs = now,
        )
      }
      if (cacheRows.isEmpty()) {
        queueCacheDao.clearAll()
      } else {
        queueCacheDao.deleteAllExcept(cacheRows.map { it.id })
        queueCacheDao.upsertAll(cacheRows)
      }
      mapped
    }.mapKnownErrors(defaultMessage = "Unable to refresh dispatch queue.")
  }

  override suspend fun verifyDeliveryOtp(
    session: RiderSessionModel,
    orderId: String,
    otp: String,
  ): Result<Boolean> {
    return runCatching {
      val api = NetworkModule.riderApi(baseUrl)
      val response = api.verifyDelivery(
        authorization = "Bearer ${session.authToken}",
        payload = DeliveryVerifyRequest(orderId = orderId, code = otp),
      )
      val data = response.data ?: throw IllegalStateException(response.error ?: "OTP verification failed")
      data.success
    }.mapKnownErrors(defaultMessage = "Unable to verify OTP right now.")
  }

  override fun observeIncidents(): Flow<List<IncidentRecord>> {
    return incidentLogDao.observeAll().map { rows -> rows.map { it.toIncidentRecord() } }
  }

  override suspend fun submitIncident(session: RiderSessionModel, draft: IncidentDraft): Result<Unit> {
    return runCatching {
      val incidentId = UUID.randomUUID().toString()
      val now = Instant.now()
      incidentLogDao.upsert(
        IncidentLogEntity(
          id = incidentId,
          orderId = draft.orderId,
          category = draft.category.name,
          note = draft.note.trim(),
          location = draft.location.trim().takeIf { it.isNotBlank() },
          syncStatus = IncidentSyncStatus.PENDING.name,
          createdAtEpochMs = now.toEpochMilli(),
          syncedAtEpochMs = null,
        ),
      )

      val api = NetworkModule.riderApi(baseUrl)
      val response = api.reportIncident(
        authorization = "Bearer ${session.authToken}",
        payload = RiderIncidentRequest(
          orderId = draft.orderId?.trim()?.takeIf { it.isNotBlank() },
          reason = draft.category.toServerReason(),
          note = draft.note.trim(),
          location = draft.location.trim().takeIf { it.isNotBlank() },
          severity = draft.category.toSeverity(),
        ),
      )

      if (response.data == null && response.error != null) {
        throw IllegalStateException(response.error)
      }

      incidentLogDao.markSynced(
        id = incidentId,
        syncStatus = IncidentSyncStatus.SYNCED.name,
        syncedAt = System.currentTimeMillis(),
      )
    }.recoverCatching { error ->
      val pending = PendingExceptionEntity(
        id = UUID.randomUUID().toString(),
        orderId = draft.orderId.orEmpty(),
        riderId = session.riderId,
        reason = draft.category.name,
        note = draft.note.trim(),
        createdAtEpochMs = System.currentTimeMillis(),
      )
      pendingExceptionDao.upsert(pending)
      throw normalizeError(error, defaultMessage = "Incident saved offline and will sync automatically.")
    }
  }

  override suspend fun syncPendingIncidents(session: RiderSessionModel): Result<Int> {
    return runCatching {
      val pending = pendingExceptionDao.listAll()
      if (pending.isEmpty()) return@runCatching 0

      val api = NetworkModule.riderApi(baseUrl)
      var synced = 0
      pending.forEach { row ->
        val category = runCatching { IncidentCategory.valueOf(row.reason) }.getOrElse { IncidentCategory.OTHER }
        val response = api.reportIncident(
          authorization = "Bearer ${session.authToken}",
          payload = RiderIncidentRequest(
            orderId = row.orderId.takeIf { it.isNotBlank() },
            reason = category.toServerReason(),
            note = row.note,
            location = null,
            severity = category.toSeverity(),
          ),
        )
        if (response.data != null || response.error == null) {
          pendingExceptionDao.deleteById(row.id)
          synced += 1
        }
      }
      synced
    }.mapKnownErrors(defaultMessage = "Unable to sync pending incidents.")
  }

  override fun observeMetrics(): Flow<DeliveryMetrics> {
    return observeOrders().map { orders -> PerformanceCalculator.fromOrders(orders) }
  }

  override fun observeSession(): Flow<RiderSessionModel?> = sessionStore.observeSessionModel()

  override fun observeTheme(): Flow<AppThemeMode> = sessionStore.observeAppTheme()

  override fun observeRingtone(): Flow<RingtoneOption> = sessionStore.observeRingtoneOption()

  override fun observeNotificationToneUri(): Flow<String?> = sessionStore.observeNotificationToneUri()

  override fun observeShiftStatus(): Flow<ShiftStatus> = sessionStore.observeShiftStatus()

  fun observeStartedOrderIds(): Flow<Set<String>> = sessionStore.observeStartedOrderIds()

  fun observeArrivedOrderIds(): Flow<Set<String>> = sessionStore.observeArrivedOrderIds()

  override suspend fun saveTheme(mode: AppThemeMode) {
    sessionStore.saveAppTheme(mode)
  }

  override suspend fun saveRingtone(option: RingtoneOption) {
    sessionStore.saveRingtoneOption(option)
  }

  override suspend fun saveNotificationToneUri(value: String?) {
    sessionStore.saveNotificationToneUri(value)
  }

  override suspend fun saveShiftStatus(status: ShiftStatus) {
    sessionStore.saveShiftStatus(status)
  }

  suspend fun updateShiftStatus(
    session: RiderSessionModel,
    status: ShiftStatus,
    note: String? = null,
  ): Result<ShiftStatus> {
    return runCatching {
      val api = NetworkModule.riderApi(baseUrl)
      val response = api.updateShiftStatus(
        authorization = "Bearer ${session.authToken}",
        payload = RiderShiftUpdateRequest(
          shiftStatus = status.toApiValue(),
          note = note?.trim()?.takeIf { it.isNotBlank() },
        ),
      )
      val data = response.data ?: throw IllegalStateException(response.error ?: "Unable to update shift status.")
      val resolvedStatus = data.shiftStatus.toShiftStatus(defaultStatus = status)
      sessionStore.saveShiftStatus(resolvedStatus)
      resolvedStatus
    }.mapKnownErrors(defaultMessage = "Unable to update shift status.")
  }

  suspend fun logout(session: RiderSessionModel): Result<Unit> {
    return runCatching {
      val api = NetworkModule.riderApi(baseUrl)
      val response = api.logout(
        authorization = "Bearer ${session.authToken}",
      )
      if (response.data?.ok != true && response.error != null) {
        throw IllegalStateException(response.error)
      }
      sessionStore.saveShiftStatus(ShiftStatus.OFFLINE)
      sessionStore.clear()
    }.mapKnownErrors(defaultMessage = "Unable to log out right now.")
  }

  suspend fun saveStartedOrderIds(orderIds: Set<String>) {
    sessionStore.saveStartedOrderIds(orderIds)
  }

  suspend fun saveArrivedOrderIds(orderIds: Set<String>) {
    sessionStore.saveArrivedOrderIds(orderIds)
  }

  suspend fun registerDeviceToken(session: RiderSessionModel, token: String): Result<Unit> {
    return runCatching {
      if (session.riderMode == RiderLoginMode.GUEST) return@runCatching
      if (token.isBlank()) return@runCatching
      val api = NetworkModule.riderApi(baseUrl)
      val response = api.registerDeviceToken(
        authorization = "Bearer ${session.authToken}",
        payload = RegisterDeviceTokenRequest(
          fcmToken = token.trim(),
          platform = "android",
        ),
      )
      if (response.data?.ok != true && response.error != null) {
        throw IllegalStateException(response.error)
      }
    }.mapKnownErrors(defaultMessage = "Unable to register notification token.")
  }

  private fun QueueCacheEntity.toDispatchOrder(): DispatchOrder {
    return DispatchOrder(
      id = id,
      orderNumber = orderNumber,
      customerName = customerName,
      customerPhone = customerPhoneMasked,
      address = address,
      status = status.toDeliveryStatus(),
      createdAt = createdAt,
      updatedAt = updatedAt,
    )
  }

  private fun String?.toRiderLoginMode(defaultMode: RiderLoginMode): RiderLoginMode {
    return when (this?.trim()?.lowercase()) {
      "guest" -> RiderLoginMode.GUEST
      "staff" -> RiderLoginMode.STAFF
      else -> defaultMode
    }
  }

  private fun IncidentLogEntity.toIncidentRecord(): IncidentRecord {
    val category = runCatching { IncidentCategory.valueOf(category) }.getOrElse { IncidentCategory.OTHER }
    val sync = runCatching { IncidentSyncStatus.valueOf(syncStatus) }.getOrElse { IncidentSyncStatus.PENDING }
    return IncidentRecord(
      id = id,
      orderId = orderId,
      category = category,
      note = note,
      location = location,
      createdAt = Instant.ofEpochMilli(createdAtEpochMs),
      syncStatus = sync,
    )
  }

  private fun String.toDeliveryStatus(): DeliveryStatus {
    return when (trim().uppercase()) {
      "READY_FOR_PICKUP",
      "READY",
      "PICKUP_READY",
      "ASSIGNED",
      "PENDING_PICKUP",
      "ACCEPTED" -> DeliveryStatus.READY_FOR_PICKUP

      "OUT_FOR_DELIVERY",
      "ON_THE_WAY",
      "DISPATCHED",
      "IN_TRANSIT" -> DeliveryStatus.OUT_FOR_DELIVERY

      "DELIVERED",
      "COMPLETED" -> DeliveryStatus.DELIVERED
      else -> DeliveryStatus.OTHER
    }
  }

  private fun IncidentCategory.toServerReason(): String = when (this) {
    IncidentCategory.MOTOR_BREAKDOWN -> "MOTOR_BREAKDOWN"
    IncidentCategory.ACCIDENT -> "ACCIDENT"
    IncidentCategory.BAD_WEATHER -> "BAD_WEATHER"
    IncidentCategory.ROAD_BLOCK -> "ROAD_BLOCK"
    IncidentCategory.MEDICAL -> "MEDICAL_EMERGENCY"
    IncidentCategory.SECURITY -> "SECURITY_THREAT"
    IncidentCategory.CUSTOMER_UNREACHABLE -> "CUSTOMER_UNREACHABLE"
    IncidentCategory.OTHER -> "OTHER"
  }

  private fun IncidentCategory.toSeverity(): String = when (this) {
    IncidentCategory.MOTOR_BREAKDOWN,
    IncidentCategory.ACCIDENT,
    IncidentCategory.MEDICAL,
    IncidentCategory.SECURITY -> "high"

    IncidentCategory.BAD_WEATHER,
    IncidentCategory.ROAD_BLOCK,
    IncidentCategory.CUSTOMER_UNREACHABLE -> "medium"
    IncidentCategory.OTHER -> "low"
  }

  private fun ShiftStatus.toApiValue(): String = when (this) {
    ShiftStatus.ONLINE -> "online"
    ShiftStatus.OFFLINE -> "offline"
  }

  private fun String?.toShiftStatus(defaultStatus: ShiftStatus): ShiftStatus {
    return when (this?.trim()?.lowercase()) {
      "online" -> ShiftStatus.ONLINE
      "offline" -> ShiftStatus.OFFLINE
      else -> defaultStatus
    }
  }

  private fun <T> Result<T>.mapKnownErrors(defaultMessage: String): Result<T> {
    return fold(
      onSuccess = { Result.success(it) },
      onFailure = { Result.failure(normalizeError(it, defaultMessage)) },
    )
  }

  private fun normalizeError(error: Throwable, defaultMessage: String): Throwable {
    val root = rootCause(error)
    val httpMessage = if (error is HttpException) extractApiErrorMessage(error) else null
    val message = when {
      root is UnknownHostException -> "Cannot reach server. Check internet and retry."
      root is SocketTimeoutException -> "Request timed out. Please retry."
      root is ConnectException -> "Server connection failed."
      error is HttpException && error.code() == 401 -> "Session expired. Sign in again."
      error is HttpException && error.code() == 429 -> "Too many requests. Please wait and retry."
      !httpMessage.isNullOrBlank() -> httpMessage
      !error.message.isNullOrBlank() -> error.message.orEmpty()
      else -> defaultMessage
    }
    return IllegalStateException(message, error)
  }

  private fun rootCause(error: Throwable): Throwable {
    var current: Throwable = error
    while (current.cause != null && current.cause !== current) {
      current = current.cause!!
    }
    return current
  }

  private fun extractApiErrorMessage(error: HttpException): String? {
    val body = runCatching { error.response()?.errorBody()?.string() }.getOrNull()?.trim().orEmpty()
    if (body.isBlank()) return null
    val regex = Regex("\"error\"\\s*:\\s*\"([^\"]+)\"")
    val raw = regex.find(body)?.groupValues?.getOrNull(1)?.trim().orEmpty()
    if (raw.isBlank()) return null
    return raw
      .replace("\\n", " ")
      .replace("\\\"", "\"")
      .trim()
      .takeIf { it.isNotBlank() }
  }
}
