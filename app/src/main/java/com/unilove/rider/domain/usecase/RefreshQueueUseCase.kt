package com.unilove.rider.domain.usecase

import com.unilove.rider.domain.repository.DispatchRepository
import com.unilove.rider.model.RiderSessionModel

class RefreshQueueUseCase(
  private val dispatchRepository: DispatchRepository,
) {
  suspend operator fun invoke(session: RiderSessionModel) = dispatchRepository.refreshOrders(session)
}
