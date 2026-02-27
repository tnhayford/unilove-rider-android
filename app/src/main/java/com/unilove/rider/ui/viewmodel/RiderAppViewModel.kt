package com.unilove.rider.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.unilove.rider.data.repo.StaffAppRepository
import com.unilove.rider.domain.usecase.LoginRiderUseCase
import com.unilove.rider.domain.usecase.RefreshQueueUseCase
import com.unilove.rider.model.AppThemeMode
import com.unilove.rider.model.DeliveryMetrics
import com.unilove.rider.model.DispatchListTab
import com.unilove.rider.model.DispatchOrder
import com.unilove.rider.model.DispatchPaymentStatus
import com.unilove.rider.model.IncidentCategory
import com.unilove.rider.model.IncidentDraft
import com.unilove.rider.model.IncidentRecord
import com.unilove.rider.model.DeliveryStatus
import com.unilove.rider.model.RingtoneOption
import com.unilove.rider.model.RiderLoginMode
import com.unilove.rider.model.RiderSessionModel
import com.unilove.rider.model.ShiftStatus
import com.unilove.rider.model.isActive
import com.unilove.rider.utils.NetworkMonitor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class RiderAppUiState(
  val showSplash: Boolean = true,
  val isOnline: Boolean = true,
  val isAuthenticating: Boolean = false,
  val riderMode: RiderLoginMode = RiderLoginMode.STAFF,
  val riderIdInput: String = "",
  val guestNameInput: String = "",
  val pinInput: String = "",
  val authError: String? = null,
  val session: RiderSessionModel? = null,
  val dispatchTab: DispatchListTab = DispatchListTab.NEW_ORDERS,
  val orders: List<DispatchOrder> = emptyList(),
  val startedOrderIds: Set<String> = emptySet(),
  val isRefreshingOrders: Boolean = false,
  val queueError: String? = null,
  val selectedOrderId: String? = null,
  val arrivedOrderIds: Set<String> = emptySet(),
  val otpCode: String = "",
  val otpMessage: String? = null,
  val otpError: String? = null,
  val isVerifyingOtp: Boolean = false,
  val isConfirmingCollection: Boolean = false,
  val collectionMessage: String? = null,
  val collectionError: String? = null,
  val metrics: DeliveryMetrics = DeliveryMetrics(0, 0, 0, List(7) { 0 }),
  val incidents: List<IncidentRecord> = emptyList(),
  val incidentDraft: IncidentDraft = IncidentDraft(),
  val incidentMessage: String? = null,
  val incidentError: String? = null,
  val isSubmittingIncident: Boolean = false,
  val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
  val ringtone: RingtoneOption = RingtoneOption.PREMIUM_CHIME,
  val notificationToneUri: String? = null,
  val shiftStatus: ShiftStatus = ShiftStatus.OFFLINE,
  val otpCompletedOrderId: String? = null,
  val pendingNavigationOrderId: String? = null,
  val pendingNavigationToActive: Boolean = false,
  val isSyncingShiftStatus: Boolean = false,
  val profileStatusMessage: String? = null,
  val profileError: String? = null,
)

class RiderAppViewModel(
  private val repository: StaffAppRepository,
  private val networkMonitor: NetworkMonitor,
) : ViewModel() {

  private val loginUseCase = LoginRiderUseCase(authRepository = repository)
  private val refreshQueueUseCase = RefreshQueueUseCase(dispatchRepository = repository)

  private val _ui = MutableStateFlow(RiderAppUiState())
  val ui: StateFlow<RiderAppUiState> = _ui.asStateFlow()

  private var autoRefreshJob: Job? = null

  init {
    viewModelScope.launch {
      delay(1100)
      _ui.value = _ui.value.copy(showSplash = false)
    }
    observeAppState()
  }

  private fun observeAppState() {
    viewModelScope.launch {
      networkMonitor.observeOnlineStatus().collect { online ->
        _ui.value = _ui.value.copy(isOnline = online)
      }
    }

    viewModelScope.launch {
      repository.observeSession().collect { session ->
        _ui.value = _ui.value.copy(
          session = session,
          startedOrderIds = if (session == null) emptySet() else _ui.value.startedOrderIds,
          arrivedOrderIds = if (session == null) emptySet() else _ui.value.arrivedOrderIds,
        )
        if (session != null) {
          startAutoRefresh()
          syncPendingIncidents()
          registerFcmToken(session)
        } else {
          autoRefreshJob?.cancel()
          autoRefreshJob = null
        }
      }
    }

    viewModelScope.launch {
      repository.observeOrders().collect { orders ->
        val current = _ui.value
        val visibleOrderIds = orders.map { it.id }.toSet()
        val activeOrderIds = orders.filter { it.status.isActive() }.map { it.id }.toSet()
        val started = current.startedOrderIds
          .intersect(visibleOrderIds)
          .intersect(activeOrderIds)
        val arrived = current.arrivedOrderIds.intersect(started)
        val shouldPersistDeliveryState = started != current.startedOrderIds || arrived != current.arrivedOrderIds
        _ui.value = _ui.value.copy(
          orders = orders,
          startedOrderIds = started,
          arrivedOrderIds = arrived,
          selectedOrderId = if (!current.selectedOrderId.isNullOrBlank() && visibleOrderIds.contains(current.selectedOrderId)) {
            current.selectedOrderId
          } else {
            orders.firstOrNull()?.id
          },
        )
        if (shouldPersistDeliveryState) {
          persistDeliveryState(started, arrived)
        }
      }
    }

    viewModelScope.launch {
      repository.observeIncidents().collect { incidents ->
        _ui.value = _ui.value.copy(incidents = incidents)
      }
    }

    viewModelScope.launch {
      repository.observeMetrics().collect { metrics ->
        _ui.value = _ui.value.copy(metrics = metrics)
      }
    }

    viewModelScope.launch {
      repository.observeTheme().collect { theme ->
        _ui.value = _ui.value.copy(themeMode = theme)
      }
    }

    viewModelScope.launch {
      repository.observeRingtone().collect { ringtone ->
        _ui.value = _ui.value.copy(ringtone = ringtone)
      }
    }

    viewModelScope.launch {
      repository.observeNotificationToneUri().collect { value ->
        _ui.value = _ui.value.copy(notificationToneUri = value)
      }
    }

    viewModelScope.launch {
      repository.observeShiftStatus().collect { status ->
        _ui.value = _ui.value.copy(shiftStatus = status)
      }
    }

    viewModelScope.launch {
      repository.observeStartedOrderIds().collect { started ->
        val current = _ui.value
        val arrived = current.arrivedOrderIds.intersect(started)
        _ui.value = current.copy(
          startedOrderIds = started,
          arrivedOrderIds = arrived,
        )
        if (arrived != current.arrivedOrderIds) {
          persistDeliveryState(started, arrived)
        }
      }
    }

    viewModelScope.launch {
      repository.observeArrivedOrderIds().collect { arrivedRaw ->
        val current = _ui.value
        val arrived = arrivedRaw.intersect(current.startedOrderIds)
        _ui.value = current.copy(arrivedOrderIds = arrived)
        if (arrived != arrivedRaw) {
          persistDeliveryState(current.startedOrderIds, arrived)
        }
      }
    }
  }

  private fun startAutoRefresh() {
    if (autoRefreshJob != null) return
    autoRefreshJob = viewModelScope.launch {
      while (true) {
        if (_ui.value.session != null && _ui.value.shiftStatus == ShiftStatus.ONLINE) {
          refreshOrders(silent = true)
        }
        delay(15000)
      }
    }
  }

  fun setRiderIdInput(value: String) {
    _ui.value = _ui.value.copy(riderIdInput = value, authError = null)
  }

  fun setGuestNameInput(value: String) {
    _ui.value = _ui.value.copy(guestNameInput = value, authError = null)
  }

  fun setRiderMode(mode: RiderLoginMode) {
    _ui.value = _ui.value.copy(
      riderMode = mode,
      authError = null,
      pinInput = if (mode == RiderLoginMode.GUEST) "" else _ui.value.pinInput,
    )
  }

  fun setPinInput(value: String) {
    _ui.value = _ui.value.copy(pinInput = value.take(8), authError = null)
  }

  fun login() {
    val riderMode = _ui.value.riderMode
    val riderId = _ui.value.riderIdInput.trim()
    val guestName = _ui.value.guestNameInput.trim()
    val pin = _ui.value.pinInput.trim()

    if (riderMode == RiderLoginMode.STAFF) {
      if (riderId.isBlank()) {
        _ui.value = _ui.value.copy(authError = "Rider ID is required")
        return
      }
      if (pin.length < 4) {
        _ui.value = _ui.value.copy(authError = "Enter a valid PIN")
        return
      }
    } else if (guestName.isBlank() && riderId.isBlank()) {
      _ui.value = _ui.value.copy(authError = "Enter guest name or alias")
      return
    }

    viewModelScope.launch {
      _ui.value = _ui.value.copy(isAuthenticating = true, authError = null)
      loginUseCase(
        riderId = riderId,
        pin = pin,
        mode = riderMode,
        riderName = guestName.ifBlank { null },
        offlineAllowed = riderMode == RiderLoginMode.STAFF,
      )
        .onSuccess {
          _ui.value = _ui.value.copy(
            isAuthenticating = false,
            pinInput = "",
            authError = null,
            shiftStatus = ShiftStatus.OFFLINE,
            profileStatusMessage = null,
            profileError = null,
          )
          repository.updateShiftStatus(
            session = it,
            status = ShiftStatus.OFFLINE,
            note = "Shift starts offline until rider goes online",
          )
        }
        .onFailure { err ->
          _ui.value = _ui.value.copy(
            isAuthenticating = false,
            authError = err.message ?: "Unable to sign in",
          )
        }
    }
  }

  fun logout() {
    val session = _ui.value.session ?: return
    if (_ui.value.isSyncingShiftStatus) return
    viewModelScope.launch {
      _ui.value = _ui.value.copy(
        isSyncingShiftStatus = true,
        profileStatusMessage = null,
        profileError = null,
      )
      repository.logout(session)
        .onSuccess {
          _ui.value = _ui.value.copy(
            isSyncingShiftStatus = false,
            pinInput = "",
            dispatchTab = DispatchListTab.NEW_ORDERS,
            selectedOrderId = null,
            startedOrderIds = emptySet(),
            arrivedOrderIds = emptySet(),
            otpCode = "",
            otpError = null,
            otpMessage = null,
            isConfirmingCollection = false,
            collectionMessage = null,
            collectionError = null,
            otpCompletedOrderId = null,
            pendingNavigationOrderId = null,
            pendingNavigationToActive = false,
            profileStatusMessage = null,
            profileError = null,
          )
          persistDeliveryState(emptySet(), emptySet())
        }
        .onFailure { err ->
          _ui.value = _ui.value.copy(
            isSyncingShiftStatus = false,
            profileStatusMessage = null,
            profileError = err.message ?: "Unable to log out. Please retry.",
          )
        }
    }
  }

  fun setDispatchTab(tab: DispatchListTab) {
    _ui.value = _ui.value.copy(dispatchTab = tab)
  }

  fun refreshOrders(silent: Boolean = false) {
    val session = _ui.value.session ?: return
    viewModelScope.launch {
      if (!silent) {
        _ui.value = _ui.value.copy(isRefreshingOrders = true, queueError = null)
      }
      refreshQueueUseCase(session)
        .onSuccess {
          _ui.value = _ui.value.copy(
            isRefreshingOrders = false,
            queueError = null,
          )
        }
        .onFailure { err ->
          _ui.value = _ui.value.copy(
            isRefreshingOrders = false,
            queueError = err.message ?: "Unable to refresh orders",
          )
        }
    }
  }

  fun openOrder(orderId: String) {
    _ui.value = _ui.value.copy(
      selectedOrderId = orderId,
      collectionMessage = null,
      collectionError = null,
    )
  }

  fun startDelivery(orderId: String) {
    val nextStarted = _ui.value.startedOrderIds + orderId
    val nextArrived = _ui.value.arrivedOrderIds.intersect(nextStarted)
    _ui.value = _ui.value.copy(
      selectedOrderId = orderId,
      startedOrderIds = nextStarted,
      arrivedOrderIds = nextArrived,
      dispatchTab = DispatchListTab.ACTIVE_DELIVERIES,
      collectionMessage = null,
      collectionError = null,
    )
    persistDeliveryState(nextStarted, nextArrived)
  }

  fun markArrived(orderId: String) {
    val nextStarted = _ui.value.startedOrderIds + orderId
    val nextArrived = _ui.value.arrivedOrderIds + orderId
    _ui.value = _ui.value.copy(
      startedOrderIds = nextStarted,
      arrivedOrderIds = nextArrived,
      collectionError = null,
    )
    persistDeliveryState(nextStarted, nextArrived)
  }

  fun confirmCashCollection(orderId: String) {
    val session = _ui.value.session ?: return
    if (_ui.value.isConfirmingCollection) return
    viewModelScope.launch {
      _ui.value = _ui.value.copy(
        isConfirmingCollection = true,
        collectionMessage = null,
        collectionError = null,
      )
      repository.confirmCashCollection(
        session = session,
        orderId = orderId,
      ).onSuccess { ok ->
        if (ok) {
          _ui.value = _ui.value.copy(
            isConfirmingCollection = false,
            collectionMessage = "Payment received. Continue to OTP confirmation.",
            collectionError = null,
          )
          refreshOrders(silent = true)
        } else {
          _ui.value = _ui.value.copy(
            isConfirmingCollection = false,
            collectionMessage = null,
            collectionError = "Unable to confirm payment. Please retry.",
          )
        }
      }.onFailure { err ->
        _ui.value = _ui.value.copy(
          isConfirmingCollection = false,
          collectionMessage = null,
          collectionError = err.message ?: "Unable to confirm payment collection.",
        )
      }
    }
  }

  fun setOtpCode(value: String) {
    _ui.value = _ui.value.copy(
      otpCode = value.filter { it.isDigit() }.take(6),
      otpMessage = null,
      otpError = null,
    )
  }

  fun verifyOtp(orderId: String) {
    val session = _ui.value.session ?: return
    val otp = _ui.value.otpCode
    val order = _ui.value.orders.firstOrNull { it.id == orderId }
    if (
      order != null &&
      order.requiresCollection &&
      order.amountDueCedis > 0 &&
      order.paymentStatus != DispatchPaymentStatus.PAID
    ) {
      _ui.value = _ui.value.copy(
        otpError = "Confirm payment collection before OTP verification.",
      )
      return
    }
    if (otp.length != 6) {
      _ui.value = _ui.value.copy(otpError = "Enter the 6-digit OTP")
      return
    }

    viewModelScope.launch {
      _ui.value = _ui.value.copy(isVerifyingOtp = true, otpError = null, otpMessage = null)
      repository.verifyDeliveryOtp(
        session = session,
        orderId = orderId,
        otp = otp,
      ).onSuccess { ok ->
        if (ok) {
          _ui.value = _ui.value.copy(
            isVerifyingOtp = false,
            dispatchTab = DispatchListTab.NEW_ORDERS,
            startedOrderIds = _ui.value.startedOrderIds - orderId,
            arrivedOrderIds = _ui.value.arrivedOrderIds - orderId,
            otpCode = "",
            otpMessage = "Delivery marked successfully.",
            otpError = null,
            collectionMessage = null,
            collectionError = null,
            otpCompletedOrderId = orderId,
          )
          persistDeliveryState(
            startedOrderIds = _ui.value.startedOrderIds,
            arrivedOrderIds = _ui.value.arrivedOrderIds,
          )
          refreshOrders(silent = true)
        } else {
          _ui.value = _ui.value.copy(
            isVerifyingOtp = false,
            otpMessage = null,
            otpError = "Invalid OTP. Please recheck with customer.",
          )
        }
      }.onFailure { err ->
        _ui.value = _ui.value.copy(
          isVerifyingOtp = false,
          otpMessage = null,
          otpError = err.message ?: "Unable to verify OTP",
        )
      }
    }
  }

  fun setIncidentOrderId(value: String) {
    _ui.value = _ui.value.copy(
      incidentDraft = _ui.value.incidentDraft.copy(orderId = value.ifBlank { null }),
      incidentError = null,
      incidentMessage = null,
    )
  }

  fun setIncidentCategory(value: IncidentCategory) {
    _ui.value = _ui.value.copy(
      incidentDraft = _ui.value.incidentDraft.copy(category = value),
      incidentError = null,
      incidentMessage = null,
    )
  }

  fun setIncidentNote(value: String) {
    _ui.value = _ui.value.copy(
      incidentDraft = _ui.value.incidentDraft.copy(note = value),
      incidentError = null,
      incidentMessage = null,
    )
  }

  fun setIncidentLocation(value: String) {
    _ui.value = _ui.value.copy(
      incidentDraft = _ui.value.incidentDraft.copy(location = value),
      incidentError = null,
      incidentMessage = null,
    )
  }

  fun submitIncident() {
    val session = _ui.value.session ?: return
    val draft = _ui.value.incidentDraft

    if (draft.note.trim().length < 6) {
      _ui.value = _ui.value.copy(incidentError = "Please add enough incident details")
      return
    }

    viewModelScope.launch {
      _ui.value = _ui.value.copy(isSubmittingIncident = true, incidentError = null, incidentMessage = null)
      repository.submitIncident(session, draft)
        .onSuccess {
          _ui.value = _ui.value.copy(
            isSubmittingIncident = false,
            incidentDraft = IncidentDraft(orderId = _ui.value.selectedOrderId),
            incidentMessage = "Incident submitted.",
            incidentError = null,
          )
          syncPendingIncidents()
        }
        .onFailure { err ->
          _ui.value = _ui.value.copy(
            isSubmittingIncident = false,
            incidentMessage = null,
            incidentError = err.message ?: "Incident saved for retry.",
          )
        }
    }
  }

  private fun syncPendingIncidents() {
    val session = _ui.value.session ?: return
    viewModelScope.launch {
      repository.syncPendingIncidents(session)
    }
  }

  fun setTheme(mode: AppThemeMode) {
    viewModelScope.launch {
      repository.saveTheme(mode)
    }
  }

  fun setRingtone(option: RingtoneOption) {
    viewModelScope.launch {
      repository.saveRingtone(option)
    }
  }

  fun setNotificationToneUri(value: String?) {
    viewModelScope.launch {
      repository.saveNotificationToneUri(value)
    }
  }

  fun toggleShiftStatus() {
    val session = _ui.value.session ?: return
    if (_ui.value.isSyncingShiftStatus) return
    viewModelScope.launch {
      val next = if (_ui.value.shiftStatus == ShiftStatus.ONLINE) ShiftStatus.OFFLINE else ShiftStatus.ONLINE
      _ui.value = _ui.value.copy(
        isSyncingShiftStatus = true,
        profileStatusMessage = null,
        profileError = null,
      )
      repository.updateShiftStatus(
        session = session,
        status = next,
      ).onSuccess { updatedStatus ->
        _ui.value = _ui.value.copy(
          isSyncingShiftStatus = false,
          shiftStatus = updatedStatus,
          profileStatusMessage = if (updatedStatus == ShiftStatus.ONLINE) "Shift is online." else "Shift is offline.",
          profileError = null,
        )
        if (updatedStatus == ShiftStatus.ONLINE) {
          refreshOrders(silent = true)
        }
      }.onFailure { err ->
        _ui.value = _ui.value.copy(
          isSyncingShiftStatus = false,
          profileStatusMessage = null,
          profileError = err.message ?: "Unable to update shift status.",
        )
      }
    }
  }

  fun openOrderFromPush(orderId: String, rawStatus: String?) {
    val order = _ui.value.orders.firstOrNull { it.id == orderId }
    val shouldOpenActive = isActivePushStatus(rawStatus) ||
      orderId in _ui.value.startedOrderIds ||
      order?.status == DeliveryStatus.OUT_FOR_DELIVERY
    _ui.value = _ui.value.copy(
      selectedOrderId = orderId,
      dispatchTab = if (shouldOpenActive) DispatchListTab.ACTIVE_DELIVERIES else DispatchListTab.NEW_ORDERS,
      pendingNavigationOrderId = orderId,
      pendingNavigationToActive = shouldOpenActive,
    )
    refreshOrders(silent = true)
  }

  fun consumePendingNavigationOrder() {
    _ui.value = _ui.value.copy(
      pendingNavigationOrderId = null,
      pendingNavigationToActive = false,
    )
  }

  fun consumeOtpCompletion() {
    _ui.value = _ui.value.copy(otpCompletedOrderId = null)
  }

  private fun registerFcmToken(session: RiderSessionModel) {
    viewModelScope.launch {
      val token = runCatching { awaitTask(FirebaseMessaging.getInstance().token) }.getOrNull() ?: return@launch
      repository.registerDeviceToken(session, token)
    }
  }

  private fun persistDeliveryState(startedOrderIds: Set<String>, arrivedOrderIds: Set<String>) {
    viewModelScope.launch {
      repository.saveStartedOrderIds(startedOrderIds)
      repository.saveArrivedOrderIds(arrivedOrderIds.intersect(startedOrderIds))
    }
  }

  private suspend fun <T> awaitTask(task: Task<T>): T {
    return suspendCancellableCoroutine { cont ->
      task.addOnCompleteListener { completed ->
        if (!cont.isActive) return@addOnCompleteListener
        val error = completed.exception
        if (error != null) {
          cont.resumeWith(Result.failure(error))
        } else {
          @Suppress("UNCHECKED_CAST")
          cont.resume(completed.result as T)
        }
      }
    }
  }

  private fun isActivePushStatus(rawStatus: String?): Boolean {
    return when (rawStatus?.trim()?.uppercase()) {
      "OUT_FOR_DELIVERY",
      "ON_THE_WAY",
      "DISPATCHED",
      "IN_TRANSIT" -> true
      else -> false
    }
  }

  companion object {
    fun factory(
      repository: StaffAppRepository,
      networkMonitor: NetworkMonitor,
    ): ViewModelProvider.Factory {
      return object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
          if (modelClass.isAssignableFrom(RiderAppViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RiderAppViewModel(
              repository = repository,
              networkMonitor = networkMonitor,
            ) as T
          }
          throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
      }
    }
  }
}
