package com.unilove.rider

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.unilove.rider.data.local.AppDatabase
import com.unilove.rider.data.local.SessionStore
import com.unilove.rider.data.repo.StaffAppRepository
import com.unilove.rider.data.worker.RiderSyncWorker
import com.unilove.rider.notifications.DispatchNotificationChannels
import com.unilove.rider.utils.NetworkMonitor
import java.util.concurrent.TimeUnit

class RiderApplication : Application() {
  lateinit var sessionStore: SessionStore
    private set

  lateinit var staffRepository: StaffAppRepository
    private set

  lateinit var networkMonitor: NetworkMonitor
    private set

  override fun onCreate() {
    super.onCreate()
    initializeFirebaseIfConfigured()
    sessionStore = SessionStore(this)
    ensureNotificationChannels()

    val db = AppDatabase.get(this)
    networkMonitor = NetworkMonitor(this)

    staffRepository = StaffAppRepository(
      sessionStore = sessionStore,
      queueCacheDao = db.queueCacheDao(),
      pendingExceptionDao = db.pendingExceptionDao(),
      incidentLogDao = db.incidentLogDao(),
    )

    scheduleBackgroundSync()
  }

  private fun initializeFirebaseIfConfigured() {
    if (FirebaseApp.getApps(this).isNotEmpty()) return
    if (BuildConfig.FIREBASE_PROJECT_ID.isBlank() ||
      BuildConfig.FIREBASE_APP_ID.isBlank() ||
      BuildConfig.FIREBASE_API_KEY.isBlank() ||
      BuildConfig.FIREBASE_SENDER_ID.isBlank()
    ) {
      return
    }

    val options = FirebaseOptions.Builder()
      .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
      .setApplicationId(BuildConfig.FIREBASE_APP_ID)
      .setApiKey(BuildConfig.FIREBASE_API_KEY)
      .setGcmSenderId(BuildConfig.FIREBASE_SENDER_ID)
      .build()

    FirebaseApp.initializeApp(this, options)
  }

  private fun ensureNotificationChannels() {
    DispatchNotificationChannels.ensure(
      context = this,
      preferredToneUri = sessionStore.currentNotificationToneUri(),
    )
  }

  private fun scheduleBackgroundSync() {
    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build()

    val request = PeriodicWorkRequestBuilder<RiderSyncWorker>(15, TimeUnit.MINUTES)
      .setConstraints(constraints)
      .build()

    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
      "unilove_rider_sync",
      ExistingPeriodicWorkPolicy.UPDATE,
      request,
    )
  }
}
