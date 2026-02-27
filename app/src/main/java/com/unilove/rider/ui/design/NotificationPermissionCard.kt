package com.unilove.rider.ui.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun NotificationPermissionCard(
  onOpenSettings: () -> Unit,
  onRequestPermission: (() -> Unit)? = null,
) {
  PremiumCard(modifier = Modifier.fillMaxWidth()) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "Dispatch alerts are disabled",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.error,
        fontWeight = FontWeight.ExtraBold,
      )
      Text(
        text = "Enable notifications so new delivery jobs ring and show full-screen alerts.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      if (onRequestPermission != null) {
        Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
          androidx.compose.material3.Icon(Icons.Default.NotificationsOff, contentDescription = "Enable notifications")
          Text(" Enable Notifications")
        }
        OutlinedButton(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
          Text("Open Notification Settings")
        }
      } else {
        Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
          androidx.compose.material3.Icon(Icons.Default.NotificationsOff, contentDescription = "Enable notifications")
          Text(" Open Notification Settings")
        }
      }
    }
  }
}
