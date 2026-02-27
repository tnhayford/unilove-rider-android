package com.unilove.rider.domain.repository

import com.unilove.rider.model.DispatchOrder
import com.unilove.rider.model.RiderSessionModel
import kotlinx.coroutines.flow.Flow

interface DispatchRepository {
  fun observeOrders(): Flow<List<DispatchOrder>>
  suspend fun refreshOrders(session: RiderSessionModel): Result<List<DispatchOrder>>
  suspend fun confirmCashCollection(session: RiderSessionModel, orderId: String): Result<Boolean>
  suspend fun verifyDeliveryOtp(session: RiderSessionModel, orderId: String, otp: String): Result<Boolean>
}
