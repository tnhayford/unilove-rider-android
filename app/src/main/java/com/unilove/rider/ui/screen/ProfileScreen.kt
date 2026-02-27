package com.unilove.rider.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unilove.rider.model.RiderSessionModel
import com.unilove.rider.model.ShiftStatus
import com.unilove.rider.ui.design.PremiumCard

@Composable
fun ProfileScreen(
  session: RiderSessionModel,
  shiftStatus: ShiftStatus,
  isSyncingShiftStatus: Boolean,
  statusMessage: String?,
  errorMessage: String?,
  onToggleShift: () -> Unit,
  onOpenSettings: () -> Unit,
  onLogout: () -> Unit,
) {
  Column(
    modifier = Modifier
      .verticalScroll(rememberScrollState())
      .imePadding()
      .navigationBarsPadding()
      .padding(bottom = 84.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
      Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(session.riderName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
        Text("Rider ID: ${session.riderId}", style = MaterialTheme.typography.bodyMedium)
        if (session.riderPhone.isNotBlank()) {
          Text("Phone: ${session.riderPhone}", style = MaterialTheme.typography.bodyMedium)
        }
        Text(
          "Mode: ${if (session.riderMode.name == "GUEST") "Guest" else "Staff"}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Support: Unilove Operations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.foundation.layout.Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column {
            Text("Shift Status", fontWeight = FontWeight.Bold)
            Text(
              if (shiftStatus == ShiftStatus.ONLINE) "Online" else "Offline",
              color = if (shiftStatus == ShiftStatus.ONLINE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(
            checked = shiftStatus == ShiftStatus.ONLINE,
            onCheckedChange = { onToggleShift() },
            enabled = !isSyncingShiftStatus,
          )
        }

        if (isSyncingShiftStatus) {
          Text(
            text = "Syncing shift status...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        if (!statusMessage.isNullOrBlank()) {
          Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        if (!errorMessage.isNullOrBlank()) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    }

    Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) {
      androidx.compose.material3.Icon(Icons.Default.Settings, contentDescription = "Open settings")
      Text(" App Settings")
    }

    Button(
      onClick = onLogout,
      modifier = Modifier.fillMaxWidth(),
      enabled = !isSyncingShiftStatus,
    ) {
      androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Log out")
      Text(" Log Out")
    }
  }
}
