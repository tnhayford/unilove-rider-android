package com.unilove.rider.domain.repository

import com.unilove.rider.model.IncidentDraft
import com.unilove.rider.model.IncidentRecord
import com.unilove.rider.model.RiderSessionModel
import kotlinx.coroutines.flow.Flow

interface IncidentRepository {
  fun observeIncidents(): Flow<List<IncidentRecord>>
  suspend fun submitIncident(session: RiderSessionModel, draft: IncidentDraft): Result<Unit>
  suspend fun syncPendingIncidents(session: RiderSessionModel): Result<Int>
}
