package com.unilove.rider

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unilove.rider.model.AppThemeMode
import com.unilove.rider.navigation.RiderApp
import com.unilove.rider.notifications.DispatchAlertPlayer
import com.unilove.rider.ui.theme.RiderTheme
import com.unilove.rider.ui.viewmodel.RiderAppViewModel

class MainActivity : ComponentActivity() {

  private val vm: RiderAppViewModel by viewModels {
    val app = application as RiderApplication
    RiderAppViewModel.factory(
      repository = app.staffRepository,
      networkMonitor = app.networkMonitor,
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      val ui by vm.ui.collectAsStateWithLifecycle()
      val systemDark = isSystemInDarkTheme()
      val darkTheme = when (ui.themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
      }

      RiderTheme(darkTheme = darkTheme) {
        RiderApp(vm = vm)
      }
    }

    handleIntent(intent)
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  private fun handleIntent(intent: android.content.Intent?) {
    val orderIdFromExtra = intent?.getStringExtra("orderId")
    val deepLinkOrderId = intent?.data?.pathSegments?.lastOrNull()
    val orderId = orderIdFromExtra ?: deepLinkOrderId
    val status = intent?.getStringExtra("status")
    if (!orderId.isNullOrBlank()) {
      vm.openOrderFromPush(orderId = orderId, rawStatus = status)
    }
  }

  override fun onDestroy() {
    DispatchAlertPlayer.stop()
    super.onDestroy()
  }
}
