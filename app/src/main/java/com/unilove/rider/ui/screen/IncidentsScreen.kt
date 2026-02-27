package com.unilove.rider.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.unilove.rider.model.IncidentCategory
import com.unilove.rider.model.IncidentDraft
import com.unilove.rider.model.IncidentRecord
import com.unilove.rider.model.IncidentSyncStatus
import com.unilove.rider.ui.design.PremiumCard
import com.unilove.rider.utils.LocationSnapshot
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun IncidentsScreen(
  draft: IncidentDraft,
  incidents: List<IncidentRecord>,
  submitting: Boolean,
  message: String?,
  error: String?,
  onOrderIdChange: (String) -> Unit,
  onCategoryChange: (IncidentCategory) -> Unit,
  onLocationChange: (String) -> Unit,
  onNoteChange: (String) -> Unit,
  onSubmit: () -> Unit,
) {
  val context = LocalContext.current
  var locationMessage by remember { mutableStateOf<String?>(null) }
  var locationMessageIsError by remember { mutableStateOf(false) }

  fun captureCurrentLocation() {
    val auto = LocationSnapshot.resolveLabel(context)
    if (auto.isNullOrBlank()) {
      locationMessage = "Unable to detect your location. Enter it manually."
      locationMessageIsError = true
      return
    }
    onLocationChange(auto)
    locationMessage = "Location captured from device."
    locationMessageIsError = false
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { result ->
    val granted = result.values.any { it }
    if (granted) {
      captureCurrentLocation()
    } else {
      locationMessage = "Location permission denied. Enter location manually."
      locationMessageIsError = true
    }
  }

  Column(
    modifier = Modifier
      .verticalScroll(rememberScrollState())
      .imePadding()
      .navigationBarsPadding()
      .padding(bottom = 84.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Report New Incident", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)

        OutlinedTextField(
          value = draft.orderId.orEmpty(),
          onValueChange = onOrderIdChange,
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          label = { Text("Order ID (optional)") },
        )

        CategorySelector(selected = draft.category, onCategoryChange = onCategoryChange)

        OutlinedTextField(
          value = draft.location,
          onValueChange = onLocationChange,
          modifier = Modifier.fillMaxWidth(),
          singleLine = true,
          label = { Text("Location") },
          placeholder = { Text("Road, area, landmark") },
        )

        OutlinedButton(
          onClick = {
            locationMessage = null
            val hasFine = ContextCompat.checkSelfPermission(
              context,
              Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(
              context,
              Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            if (hasFine || hasCoarse) {
              captureCurrentLocation()
            } else {
              locationPermissionLauncher.launch(
                arrayOf(
                  Manifest.permission.ACCESS_FINE_LOCATION,
                  Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
              )
            }
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Use Current Location")
        }
        if (!locationMessage.isNullOrBlank()) {
          Text(
            text = locationMessage.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = if (locationMessageIsError) {
              MaterialTheme.colorScheme.error
            } else {
              MaterialTheme.colorScheme.primary
            },
          )
        }

        OutlinedTextField(
          value = draft.note,
          onValueChange = onNoteChange,
          modifier = Modifier.fillMaxWidth(),
          minLines = 3,
          maxLines = 5,
          label = { Text("Incident Note") },
          placeholder = { Text("Describe what happened") },
        )

        Button(
          onClick = onSubmit,
          modifier = Modifier.fillMaxWidth(),
          enabled = !submitting && draft.note.trim().length >= 6,
        ) {
          Text(if (submitting) "Submitting..." else "Submit Incident")
        }

        if (!message.isNullOrBlank()) {
          Text(message, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        if (!error.isNullOrBlank()) {
          Text(error, color = MaterialTheme.colorScheme.error)
        }
      }
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Recent Incidents", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        if (incidents.isEmpty()) {
          Text(
            "No incidents reported yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            incidents.take(10).forEach { incident ->
              IncidentRow(incident)
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelector(
  selected: IncidentCategory,
  onCategoryChange: (IncidentCategory) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text("Category", style = MaterialTheme.typography.labelLarge)
    ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = !expanded },
    ) {
      OutlinedTextField(
        value = selected.label,
        onValueChange = {},
        readOnly = true,
        modifier = Modifier
          .menuAnchor()
          .fillMaxWidth(),
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      )
      DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
      ) {
        IncidentCategory.entries.forEach { category ->
          DropdownMenuItem(
            text = { Text(category.label) },
            onClick = {
              onCategoryChange(category)
              expanded = false
            },
          )
        }
      }
    }
  }
}

@Composable
private fun IncidentRow(record: IncidentRecord) {
  val timeLabel = DateTimeFormatter.ofPattern("EEE HH:mm")
    .format(record.createdAt.atZone(ZoneId.systemDefault()))

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    androidx.compose.material3.Icon(
      imageVector = Icons.Default.Warning,
      contentDescription = "Incident",
      tint = if (record.syncStatus == IncidentSyncStatus.PENDING) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
    )
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(record.category.label, fontWeight = FontWeight.SemiBold)
      Text(record.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(
        "$timeLabel • ${record.syncStatus.name}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
