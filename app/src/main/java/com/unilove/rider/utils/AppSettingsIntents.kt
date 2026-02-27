package com.unilove.rider.utils

import android.content.Context
import android.content.Intent
import android.provider.Settings

fun openNotificationSettings(context: Context) {
  val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
  }
  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  context.startActivity(intent)
}
