package com.unilove.rider.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.unilove.rider.model.AppThemeMode
import com.unilove.rider.model.RingtoneOption
import com.unilove.rider.model.RiderLoginMode
import com.unilove.rider.model.RiderSessionModel
import com.unilove.rider.model.ShiftStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

class SessionStore(context: Context) {
  private val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .build()

  private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
    context,
    PREFS_NAME,
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
  )

  private val sessionFlow = MutableStateFlow(readSessionModel())
  private val themeFlow = MutableStateFlow(readTheme())
  private val ringtoneFlow = MutableStateFlow(readRingtone())
  private val notificationToneUriFlow = MutableStateFlow(readNotificationToneUri())
  private val shiftFlow = MutableStateFlow(readShiftStatus())
  private val startedOrderIdsFlow = MutableStateFlow(readDeliveryIdSet(KEY_STARTED_ORDER_IDS))
  private val arrivedOrderIdsFlow = MutableStateFlow(readDeliveryIdSet(KEY_ARRIVED_ORDER_IDS))

  private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
    sessionFlow.value = readSessionModel()
    themeFlow.value = readTheme()
    ringtoneFlow.value = readRingtone()
    notificationToneUriFlow.value = readNotificationToneUri()
    shiftFlow.value = readShiftStatus()
    startedOrderIdsFlow.value = readDeliveryIdSet(KEY_STARTED_ORDER_IDS)
    arrivedOrderIdsFlow.value = readDeliveryIdSet(KEY_ARRIVED_ORDER_IDS)
  }

  init {
    prefs.registerOnSharedPreferenceChangeListener(listener)
  }

  fun observeSessionModel(): Flow<RiderSessionModel?> = sessionFlow.asStateFlow()

  suspend fun saveSession(
    riderId: String,
    riderName: String,
    riderPhone: String = "",
    authToken: String,
    riderMode: RiderLoginMode = RiderLoginMode.STAFF,
  ) {
    prefs.edit()
      .putString(KEY_RIDER_ID, riderId)
      .putString(KEY_RIDER_NAME, riderName)
      .putString(KEY_RIDER_PHONE, riderPhone.trim())
      .putString(KEY_AUTH_TOKEN, authToken)
      .putString(KEY_RIDER_MODE, riderMode.name)
      .putLong(KEY_AUTH_AT, System.currentTimeMillis())
      .apply()
  }

  suspend fun saveOfflinePin(riderId: String, pin: String) {
    prefs.edit()
      .putString(KEY_PIN_HASH, hashPin(riderId, pin))
      .apply()
  }

  fun canLoginOffline(riderId: String, pin: String): Boolean {
    val storedRiderId = prefs.getString(KEY_RIDER_ID, "").orEmpty()
    if (!storedRiderId.equals(riderId.trim(), ignoreCase = true)) return false
    val storedHash = prefs.getString(KEY_PIN_HASH, "").orEmpty()
    if (storedHash.isBlank()) return false
    return storedHash == hashPin(storedRiderId, pin)
  }

  fun hasSession(): Boolean = sessionFlow.value != null

  fun getSessionModel(): RiderSessionModel? = sessionFlow.value

  suspend fun clear() {
    val existingTheme = readTheme().name
    val existingRingtone = readRingtone().name
    val existingNotificationToneUri = readNotificationToneUri()
    prefs.edit().clear()
      .putString(KEY_THEME, existingTheme)
      .putString(KEY_RINGTONE, existingRingtone)
      .putString(KEY_NOTIFICATION_TONE_URI, existingNotificationToneUri)
      .putString(KEY_SHIFT_STATUS, ShiftStatus.OFFLINE.name)
      .apply()
  }

  fun observeAppTheme(): Flow<AppThemeMode> = themeFlow.asStateFlow()

  suspend fun saveAppTheme(mode: AppThemeMode) {
    prefs.edit().putString(KEY_THEME, mode.name).apply()
  }

  fun observeRingtoneOption(): Flow<RingtoneOption> = ringtoneFlow.asStateFlow()

  suspend fun saveRingtoneOption(option: RingtoneOption) {
    prefs.edit().putString(KEY_RINGTONE, option.name).apply()
  }

  fun observeNotificationToneUri(): Flow<String?> = notificationToneUriFlow.asStateFlow()

  suspend fun saveNotificationToneUri(value: String?) {
    prefs.edit().putString(KEY_NOTIFICATION_TONE_URI, value?.trim()?.takeIf { it.isNotBlank() }).apply()
  }

  fun observeShiftStatus(): Flow<ShiftStatus> = shiftFlow.asStateFlow()

  suspend fun saveShiftStatus(status: ShiftStatus) {
    prefs.edit().putString(KEY_SHIFT_STATUS, status.name).apply()
  }

  fun observeStartedOrderIds(): Flow<Set<String>> = startedOrderIdsFlow.asStateFlow()

  suspend fun saveStartedOrderIds(orderIds: Set<String>) {
    prefs.edit()
      .putStringSet(KEY_STARTED_ORDER_IDS, orderIds.filter { it.isNotBlank() }.toSet())
      .apply()
  }

  fun observeArrivedOrderIds(): Flow<Set<String>> = arrivedOrderIdsFlow.asStateFlow()

  suspend fun saveArrivedOrderIds(orderIds: Set<String>) {
    prefs.edit()
      .putStringSet(KEY_ARRIVED_ORDER_IDS, orderIds.filter { it.isNotBlank() }.toSet())
      .apply()
  }

  fun currentRingtoneOption(): RingtoneOption = readRingtone()

  fun currentNotificationToneUri(): String? = readNotificationToneUri()

  private fun readSessionModel(): RiderSessionModel? {
    val riderId = prefs.getString(KEY_RIDER_ID, "").orEmpty()
    val riderName = prefs.getString(KEY_RIDER_NAME, "").orEmpty()
    val riderPhone = prefs.getString(KEY_RIDER_PHONE, "").orEmpty()
    val authToken = prefs.getString(KEY_AUTH_TOKEN, "").orEmpty()
    val riderModeRaw = prefs.getString(KEY_RIDER_MODE, RiderLoginMode.STAFF.name).orEmpty()
    val riderMode = runCatching { RiderLoginMode.valueOf(riderModeRaw) }.getOrElse { RiderLoginMode.STAFF }
    val authAt = prefs.getLong(KEY_AUTH_AT, 0L)
    if (riderId.isBlank() || authToken.isBlank()) return null

    return RiderSessionModel(
      riderId = riderId,
      riderName = riderName,
      riderPhone = riderPhone,
      authToken = authToken,
      authenticatedAtEpochMs = authAt,
      riderMode = riderMode,
    )
  }

  private fun readTheme(): AppThemeMode {
    val raw = prefs.getString(KEY_THEME, AppThemeMode.SYSTEM.name).orEmpty()
    return runCatching { AppThemeMode.valueOf(raw) }.getOrElse { AppThemeMode.SYSTEM }
  }

  private fun readRingtone(): RingtoneOption {
    val raw = prefs.getString(KEY_RINGTONE, RingtoneOption.PREMIUM_CHIME.name).orEmpty()
    return runCatching { RingtoneOption.valueOf(raw) }.getOrElse { RingtoneOption.PREMIUM_CHIME }
  }

  private fun readNotificationToneUri(): String? {
    return prefs.getString(KEY_NOTIFICATION_TONE_URI, null)?.takeIf { it.isNotBlank() }
  }

  private fun readShiftStatus(): ShiftStatus {
    val raw = prefs.getString(KEY_SHIFT_STATUS, ShiftStatus.ONLINE.name).orEmpty()
    return runCatching { ShiftStatus.valueOf(raw) }.getOrElse { ShiftStatus.ONLINE }
  }

  private fun readDeliveryIdSet(key: String): Set<String> {
    return prefs.getStringSet(key, emptySet())
      .orEmpty()
      .filter { it.isNotBlank() }
      .toSet()
  }

  private fun hashPin(riderId: String, pin: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val value = "${riderId.trim().lowercase()}::${pin.trim()}::$PIN_SALT"
    return digest.digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
  }

  companion object {
    const val PREFS_NAME = "rider_secure_prefs"

    private const val KEY_RIDER_ID = "rider_id"
    private const val KEY_RIDER_NAME = "rider_name"
    private const val KEY_RIDER_PHONE = "rider_phone"
    private const val KEY_AUTH_TOKEN = "auth_token"
    private const val KEY_RIDER_MODE = "rider_mode"
    private const val KEY_AUTH_AT = "auth_at"
    private const val KEY_PIN_HASH = "pin_hash"
    private const val KEY_THEME = "theme_mode"
    private const val KEY_RINGTONE = "ringtone_option"
    private const val KEY_NOTIFICATION_TONE_URI = "notification_tone_uri"
    private const val KEY_SHIFT_STATUS = "shift_status"
    private const val KEY_STARTED_ORDER_IDS = "started_order_ids"
    private const val KEY_ARRIVED_ORDER_IDS = "arrived_order_ids"

    private const val PIN_SALT = "unilove_rider_offline"
  }
}
