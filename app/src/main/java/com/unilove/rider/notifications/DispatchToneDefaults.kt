package com.unilove.rider.notifications

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import com.unilove.rider.R

object DispatchToneDefaults {
  fun resolvePreferredOrDefault(context: Context, preferredToneUri: String?): Uri? {
    val preferred = preferredToneUri
      ?.takeIf { it.isNotBlank() }
      ?.let { runCatching { Uri.parse(it) }.getOrNull() }
    if (preferred != null) return preferred
    return bundledRiderToneUri(context)
  }

  fun systemFallbackUri(): Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

  private fun bundledRiderToneUri(context: Context): Uri {
    return Uri.parse("android.resource://${context.packageName}/${R.raw.rider_alert_default}")
  }
}
