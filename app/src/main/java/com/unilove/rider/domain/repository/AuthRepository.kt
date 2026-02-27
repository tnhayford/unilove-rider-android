package com.unilove.rider.domain.repository

import com.unilove.rider.model.RiderSessionModel
import com.unilove.rider.model.RiderLoginMode

interface AuthRepository {
  suspend fun login(
    riderId: String,
    pin: String,
    mode: RiderLoginMode = RiderLoginMode.STAFF,
    riderName: String? = null,
  ): Result<RiderSessionModel>
  suspend fun loginOffline(
    riderId: String,
    pin: String,
    mode: RiderLoginMode = RiderLoginMode.STAFF,
  ): Result<RiderSessionModel>
  suspend fun logout()
}
