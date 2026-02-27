package com.unilove.rider.ui.screen

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.unilove.rider.MainActivity
import com.unilove.rider.data.local.SessionStore
import com.unilove.rider.notifications.DispatchAlertPlayer
import com.unilove.rider.notifications.DispatchIntentActions
import com.unilove.rider.ui.theme.RiderTheme

class IncomingDispatchActivity : ComponentActivity() {
  private var orderId: String = ""
  private var status: String = ""

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableScreenWakeMode()

    orderId = intent?.getStringExtra(DispatchIntentActions.EXTRA_ORDER_ID).orEmpty()
    status = intent?.getStringExtra(DispatchIntentActions.EXTRA_STATUS).orEmpty()
    val title = intent?.getStringExtra(DispatchIntentActions.EXTRA_TITLE)
      ?.takeIf { it.isNotBlank() }
      ?: "New Dispatch Assigned"
    val body = intent?.getStringExtra(DispatchIntentActions.EXTRA_BODY)
      ?.takeIf { it.isNotBlank() }
      ?: "A new delivery order is waiting for immediate action."

    val toneUri = SessionStore(this).currentNotificationToneUri()
    DispatchAlertPlayer.startLoop(
      context = this,
      notificationToneUri = toneUri,
    )

    setContent {
      RiderTheme {
        IncomingDispatchScreen(
          title = title,
          body = body,
          orderId = orderId,
          onOpenDispatch = { openDispatch() },
          onDismiss = { dismissAlert() },
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  override fun onDestroy() {
    DispatchAlertPlayer.stop()
    super.onDestroy()
  }

  private fun enableScreenWakeMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
      setShowWhenLocked(true)
      setTurnScreenOn(true)
    } else {
      @Suppress("DEPRECATION")
      window.addFlags(
        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
          WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
          WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
      )
    }
  }

  private fun dismissAlert() {
    DispatchAlertPlayer.stop()
    NotificationManagerCompat.from(this).cancel(notificationIdFor(orderId))
    finish()
  }

  private fun openDispatch() {
    DispatchAlertPlayer.stop()
    NotificationManagerCompat.from(this).cancel(notificationIdFor(orderId))
    startActivity(
      Intent(this, MainActivity::class.java).apply {
        action = "OPEN_ORDER"
        putExtra("orderId", orderId)
        putExtra("status", status)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
      },
    )
    finish()
  }

  private fun notificationIdFor(value: String): Int {
    return value.ifBlank { "dispatch_alert" }.hashCode()
  }
}

@Composable
private fun IncomingDispatchScreen(
  title: String,
  body: String,
  orderId: String,
  onOpenDispatch: () -> Unit,
  onDismiss: () -> Unit,
) {
  val gradient = Brush.verticalGradient(
    listOf(
      MaterialTheme.colorScheme.background,
      MaterialTheme.colorScheme.surface,
      MaterialTheme.colorScheme.surfaceVariant,
    ),
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(gradient)
      .padding(18.dp),
    contentAlignment = Alignment.Center,
  ) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(28.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Icon(
          imageVector = Icons.Default.NotificationsActive,
          contentDescription = "Dispatch alert",
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = title,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.ExtraBold,
          textAlign = TextAlign.Center,
        )
        Text(
          text = body,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        if (orderId.isNotBlank()) {
          Text(
            text = "Order: $orderId",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
          )
        }

        Button(
          onClick = onOpenDispatch,
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
          ),
        ) {
          Icon(imageVector = Icons.Default.DeliveryDining, contentDescription = "Open dispatch")
          Text(" Open Dispatch")
        }

        OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
          Text("Dismiss")
        }
      }
    }
  }
}
