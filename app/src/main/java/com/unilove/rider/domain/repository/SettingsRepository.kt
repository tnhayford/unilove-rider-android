package com.unilove.rider.domain.repository

import com.unilove.rider.model.AppThemeMode
import com.unilove.rider.model.RingtoneOption
import com.unilove.rider.model.RiderSessionModel
import com.unilove.rider.model.ShiftStatus
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
  fun observeSession(): Flow<RiderSessionModel?>
  fun observeTheme(): Flow<AppThemeMode>
  fun observeRingtone(): Flow<RingtoneOption>
  fun observeNotificationToneUri(): Flow<String?>
  fun observeShiftStatus(): Flow<ShiftStatus>

  suspend fun saveTheme(mode: AppThemeMode)
  suspend fun saveRingtone(option: RingtoneOption)
  suspend fun saveNotificationToneUri(value: String?)
  suspend fun saveShiftStatus(status: ShiftStatus)
}
