package com.unilove.rider.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat

object LocationSnapshot {
  fun resolveLabel(context: Context): String? {
    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) return null

    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = manager.getProviders(true)
    val location = providers
      .asSequence()
      .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
      .maxByOrNull { it.time }
      ?: return null

    return "Lat ${"%.5f".format(location.latitude)}, Lng ${"%.5f".format(location.longitude)}"
  }
}
