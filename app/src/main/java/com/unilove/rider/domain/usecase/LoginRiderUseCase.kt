package com.unilove.rider.domain.usecase

import com.unilove.rider.domain.repository.AuthRepository
import com.unilove.rider.model.RiderLoginMode
import com.unilove.rider.model.RiderSessionModel

class LoginRiderUseCase(
  private val authRepository: AuthRepository,
) {
  suspend operator fun invoke(
    riderId: String,
    pin: String,
    mode: RiderLoginMode = RiderLoginMode.STAFF,
    riderName: String? = null,
    offlineAllowed: Boolean = true,
  ): Result<RiderSessionModel> {
    return authRepository.login(
      riderId = riderId,
      pin = pin,
      mode = mode,
      riderName = riderName,
    ).recoverCatching {
      if (!offlineAllowed) throw it
      authRepository.loginOffline(
        riderId = riderId,
        pin = pin,
        mode = mode,
      ).getOrThrow()
    }
  }
}
