package com.unilove.rider.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NetworkMonitor(context: Context) {
  private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  fun observeOnlineStatus(): Flow<Boolean> = callbackFlow {
    val callback = object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        trySend(true)
      }

      override fun onLost(network: Network) {
        trySend(isConnectedNow())
      }

      override fun onUnavailable() {
        trySend(false)
      }
    }

    trySend(isConnectedNow())

    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()

    connectivityManager.registerNetworkCallback(request, callback)

    awaitClose {
      runCatching { connectivityManager.unregisterNetworkCallback(callback) }
    }
  }

  fun isConnectedNow(): Boolean {
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }
}
