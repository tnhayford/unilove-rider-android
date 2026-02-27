package com.unilove.rider.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes

object DispatchNotificationChannels {
  const val DISPATCH_ALERTS = "dispatch_alerts"

  fun ensure(context: Context, preferredToneUri: String?) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return

    val desiredSound = DispatchToneDefaults.resolvePreferredOrDefault(context, preferredToneUri)
      ?: DispatchToneDefaults.systemFallbackUri()

    val existing = manager.getNotificationChannel(DISPATCH_ALERTS)
    val needsRecreate = existing == null || existing.sound?.toString() != desiredSound?.toString()
    if (existing != null && needsRecreate) {
      manager.deleteNotificationChannel(DISPATCH_ALERTS)
    }
    if (!needsRecreate) return

    val attrs = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()

    val channel = NotificationChannel(
      DISPATCH_ALERTS,
      "Dispatch Alerts",
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = "Urgent rider order dispatch alerts"
      enableVibration(true)
      setSound(desiredSound, attrs)
      lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    }

    manager.createNotificationChannel(channel)
  }
}
