package com.unilove.rider.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class DispatchNotificationActionReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != DispatchIntentActions.ACTION_DISMISS_DISPATCH) return

    val orderId = intent.getStringExtra(DispatchIntentActions.EXTRA_ORDER_ID).orEmpty()
    val notificationId = orderId.ifBlank { "dispatch_alert" }.hashCode()

    DispatchAlertPlayer.stop()
    NotificationManagerCompat.from(context).cancel(notificationId)
  }
}
