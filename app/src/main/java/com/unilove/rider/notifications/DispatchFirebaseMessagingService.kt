package com.unilove.rider.notifications

import android.app.PendingIntent
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.unilove.rider.MainActivity
import com.unilove.rider.R
import com.unilove.rider.data.local.SessionStore
import com.unilove.rider.ui.screen.IncomingDispatchActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DispatchFirebaseMessagingService : FirebaseMessagingService() {
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val sessionStore by lazy { SessionStore(applicationContext) }

  override fun onMessageReceived(message: RemoteMessage) {
    val status = message.data["status"].orEmpty().uppercase()
    val orderId = message.data["orderId"].orEmpty()
    val orderNumber = message.data["orderNumber"].orEmpty()
    val title = message.data["title"]
      ?: message.notification?.title
      ?: if (status == "READY_FOR_PICKUP") "Order Ready for Pickup" else "New Delivery Dispatch"
    val body = message.data["body"]
      ?: message.notification?.body
      ?: if (orderNumber.isNotBlank()) {
        "Order $orderNumber needs rider action now."
      } else {
        "You have a new order dispatch."
      }

    val appInForeground = isAppInForeground()
    val ringtone = sessionStore.currentRingtoneOption()
    val toneUri = sessionStore.currentNotificationToneUri()
    if (appInForeground) {
      DispatchAlertPlayer.playOneShot(
        context = applicationContext,
        ringtoneOption = ringtone,
        notificationToneUri = toneUri,
      )
    } else {
      DispatchAlertPlayer.startLoop(
        context = applicationContext,
        ringtoneOption = ringtone,
        notificationToneUri = toneUri,
      )
    }
    showDispatchNotification(
      title = title,
      body = body,
      orderId = orderId,
      status = status,
      useFullScreen = !appInForeground,
    )
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    if (token.isBlank()) return

    serviceScope.launch {
      val app = application as? com.unilove.rider.RiderApplication ?: return@launch
      val session = app.sessionStore.getSessionModel() ?: return@launch
      app.staffRepository.registerDeviceToken(session = session, token = token)
    }
  }

  private fun showDispatchNotification(
    title: String,
    body: String,
    orderId: String,
    status: String,
    useFullScreen: Boolean,
  ) {
    DispatchNotificationChannels.ensure(
      context = this,
      preferredToneUri = sessionStore.currentNotificationToneUri(),
    )

    val alertKey = orderId.ifBlank { "dispatch_${System.currentTimeMillis()}" }
    val notificationId = alertKey.hashCode()

    val openIntent = Intent(this, MainActivity::class.java).apply {
      action = "OPEN_ORDER"
      putExtra("orderId", orderId)
      putExtra("status", status)
      putExtra(DispatchIntentActions.EXTRA_TITLE, title)
      putExtra(DispatchIntentActions.EXTRA_BODY, body)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val openPendingIntent = PendingIntent.getActivity(
      this,
      alertKey.hashCode(),
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val dismissIntent = Intent(this, DispatchNotificationActionReceiver::class.java).apply {
      action = DispatchIntentActions.ACTION_DISMISS_DISPATCH
      putExtra(DispatchIntentActions.EXTRA_ORDER_ID, orderId)
    }
    val dismissPendingIntent = PendingIntent.getBroadcast(
      this,
      "${alertKey}_dismiss".hashCode(),
      dismissIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val fullScreenIntent = Intent(this, IncomingDispatchActivity::class.java).apply {
      putExtra(DispatchIntentActions.EXTRA_ORDER_ID, orderId)
      putExtra(DispatchIntentActions.EXTRA_STATUS, status)
      putExtra(DispatchIntentActions.EXTRA_TITLE, title)
      putExtra(DispatchIntentActions.EXTRA_BODY, body)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val fullScreenPendingIntent = PendingIntent.getActivity(
      this,
      "${alertKey}_fullscreen".hashCode(),
      fullScreenIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val builder = NotificationCompat.Builder(this, DispatchNotificationChannels.DISPATCH_ALERTS)
      .setSmallIcon(R.drawable.ic_stat_unilove)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(NotificationCompat.BigTextStyle().bigText(body))
      .setAutoCancel(!useFullScreen)
      .setOngoing(useFullScreen)
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_CALL)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE)
      .setContentIntent(openPendingIntent)
      .setDeleteIntent(dismissPendingIntent)
      .addAction(android.R.drawable.ic_menu_view, "Open", openPendingIntent)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
    if (useFullScreen) {
      builder.setFullScreenIntent(fullScreenPendingIntent, true)
    }

    val notification = builder.build()

    val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        this,
        POST_NOTIFICATIONS_PERMISSION,
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
    if (hasNotificationPermission) {
      NotificationManagerCompat.from(this).notify(notificationId, notification)
    }
  }

  private fun isAppInForeground(): Boolean {
    val manager = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
    val running = manager.runningAppProcesses ?: return false
    return running.any { process ->
      process.processName == packageName &&
        process.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }
  }

  companion object {
    private const val POST_NOTIFICATIONS_PERMISSION = "android.permission.POST_NOTIFICATIONS"
  }
}
