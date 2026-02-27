package com.unilove.rider.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unilove.rider.RiderApplication

/**
 * Background sync keeps dispatch cache and pending incidents updated
 * even when the rider app has not been manually refreshed.
 */
class RiderSyncWorker(
  appContext: Context,
  workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val app = applicationContext as? RiderApplication ?: return Result.retry()
    val session = app.sessionStore.getSessionModel() ?: return Result.success()

    return app.staffRepository.refreshOrders(session).fold(
      onSuccess = {
        app.staffRepository.syncPendingIncidents(session)
        Result.success()
      },
      onFailure = {
        Result.retry()
      },
    )
  }
}
