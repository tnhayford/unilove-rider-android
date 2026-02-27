package com.unilove.rider.domain.repository

import com.unilove.rider.model.DeliveryMetrics
import kotlinx.coroutines.flow.Flow

interface PerformanceRepository {
  fun observeMetrics(): Flow<DeliveryMetrics>
}
