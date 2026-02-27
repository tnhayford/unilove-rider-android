package com.unilove.rider.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.app.NotificationManagerCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unilove.rider.BuildConfig
import com.unilove.rider.model.AppThemeMode
import com.unilove.rider.model.RingtoneOption
import com.unilove.rider.notifications.DispatchAlertPlayer
import com.unilove.rider.notifications.DispatchNotificationChannels
import com.unilove.rider.notifications.DispatchToneDefaults
import com.unilove.rider.ui.design.NotificationPermissionCard
import com.unilove.rider.ui.design.PremiumCard
import com.unilove.rider.utils.openNotificationSettings
import androidx.core.content.ContextCompat

@Composable
fun SettingsScreen(
  themeMode: AppThemeMode,
  ringtone: RingtoneOption,
  notificationToneUri: String?,
  onThemeChange: (AppThemeMode) -> Unit,
  onRingtoneChange: (RingtoneOption) -> Unit,
  onNotificationTonePicked: (String?) -> Unit,
  onBack: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val realtimeAlertsReady = BuildConfig.FIREBASE_PROJECT_ID.isNotBlank() &&
    BuildConfig.FIREBASE_APP_ID.isNotBlank() &&
    BuildConfig.FIREBASE_API_KEY.isNotBlank() &&
    BuildConfig.FIREBASE_SENDER_ID.isNotBlank()
  var notificationsEnabled by remember(context) {
    mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
  }
  var notificationPermissionGranted by remember(context) {
    mutableStateOf(hasPostNotificationsPermission(context))
  }

  DisposableEffect(lifecycleOwner, context) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        notificationPermissionGranted = hasPostNotificationsPermission(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  val pickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult(),
  ) { result ->
    if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
    val data = result.data ?: return@rememberLauncherForActivityResult
    val picked: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
    } else {
      @Suppress("DEPRECATION")
      data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
    }
    val value = picked?.toString()
    onNotificationTonePicked(value)
    DispatchNotificationChannels.ensure(context, value)
  }
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    notificationPermissionGranted = granted || hasPostNotificationsPermission(context)
    notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
  }

  Column(
    modifier = Modifier
      .verticalScroll(rememberScrollState())
      .imePadding()
      .navigationBarsPadding()
      .padding(bottom = 84.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    if (!realtimeAlertsReady) {
      PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
          Text(
            text = "Realtime dispatch alerts are unavailable",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.ExtraBold,
          )
          Text(
            text = "This app build cannot receive background push alerts. Contact support for an updated rider build.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    val notificationsReady = notificationsEnabled && notificationPermissionGranted
    val shouldRequestPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermissionGranted
    if (!notificationsReady) {
      NotificationPermissionCard(
        onOpenSettings = { openNotificationSettings(context) },
        onRequestPermission = if (shouldRequestPermission) {
          { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        } else {
          null
        },
      )
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Theme", fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          AppThemeMode.entries.forEach { mode ->
            AssistChip(
              onClick = { onThemeChange(mode) },
              label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
              modifier = Modifier.weight(1f),
              colors = if (themeMode == mode) {
                AssistChipDefaults.assistChipColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  labelColor = MaterialTheme.colorScheme.onPrimary,
                )
              } else {
                AssistChipDefaults.assistChipColors()
              },
            )
          }
        }
      }
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Dispatch Alert Tone", fontWeight = FontWeight.Bold)
        Text(
          text = "Selected: ${notificationToneLabel(context, notificationToneUri)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
          onClick = {
            val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
              putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
              putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choose dispatch tone")
              putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
              putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
              putExtra(
                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                notificationToneUri?.let { Uri.parse(it) }
                  ?: DispatchToneDefaults.resolvePreferredOrDefault(context, null),
              )
            }
            pickerLauncher.launch(pickerIntent)
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Choose from phone notification tones")
        }

        OutlinedButton(
          onClick = {
            DispatchAlertPlayer.playOneShot(
              context = context,
              ringtoneOption = ringtone,
              notificationToneUri = notificationToneUri,
            )
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Preview selected tone")
        }

        OutlinedButton(
          onClick = {
            onNotificationTonePicked(null)
            DispatchNotificationChannels.ensure(context, null)
          },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text("Use rider default dispatch tone")
        }
      }
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Fallback App Tone", fontWeight = FontWeight.Bold)
        RingtoneOption.entries.forEach { option ->
          OutlinedButton(
            onClick = {
              onRingtoneChange(option)
              DispatchAlertPlayer.playOneShot(
                context = context,
                ringtoneOption = option,
                notificationToneUri = notificationToneUri,
              )
            },
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(if (ringtone == option) "${option.title} (Selected)" else option.title)
          }
        }
      }
    }

    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
      Text("Done")
    }
  }
}

private fun notificationToneLabel(context: android.content.Context, toneUri: String?): String {
  if (toneUri.isNullOrBlank()) return "Rider default tone"
  val uri = runCatching { Uri.parse(toneUri) }.getOrNull()
  val title = uri?.let { runCatching { RingtoneManager.getRingtone(context, it)?.getTitle(context) }.getOrNull() }
  return title?.takeIf { it.isNotBlank() } ?: "Custom tone"
}

private fun hasPostNotificationsPermission(context: android.content.Context): Boolean {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
  return ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.POST_NOTIFICATIONS,
  ) == PackageManager.PERMISSION_GRANTED
}
