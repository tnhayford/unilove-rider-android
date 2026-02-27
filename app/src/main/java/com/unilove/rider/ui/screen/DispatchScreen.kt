package com.unilove.rider.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.unilove.rider.model.DeliveryStatus
import com.unilove.rider.model.DispatchPaymentMethod
import com.unilove.rider.model.DispatchPaymentStatus
import com.unilove.rider.model.DispatchListTab
import com.unilove.rider.model.DispatchOrder
import com.unilove.rider.model.ShiftStatus
import com.unilove.rider.model.isActive
import com.unilove.rider.ui.design.NotificationPermissionCard
import com.unilove.rider.ui.design.PremiumCard
import com.unilove.rider.ui.design.StatusBadge
import com.unilove.rider.utils.openNotificationSettings
import java.time.Duration
import java.time.Instant

@Composable
fun DispatchScreen(
  orders: List<DispatchOrder>,
  startedOrderIds: Set<String>,
  selectedTab: DispatchListTab,
  shiftStatus: ShiftStatus,
  isSyncingShiftStatus: Boolean,
  queueError: String?,
  onTabChange: (DispatchListTab) -> Unit,
  onStartDelivery: (DispatchOrder) -> Unit,
  onToggleShift: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var notificationsEnabled by remember(context) {
    mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
  }
  var notificationPermissionGranted by remember(context) {
    mutableStateOf(hasPostNotificationsPermission(context))
  }
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    notificationPermissionGranted = granted || hasPostNotificationsPermission(context)
    notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
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

  Column(
    modifier = Modifier
      .navigationBarsPadding()
      .padding(bottom = 10.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    val shiftOnline = shiftStatus == ShiftStatus.ONLINE
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = if (shiftOnline) "You are online for dispatch." else "You are offline.",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = if (shiftOnline) {
            "Tap Go Offline when ending your shift."
          } else {
            "Tap Go Online to start receiving assigned orders."
          },
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
          onClick = onToggleShift,
          enabled = !isSyncingShiftStatus,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            if (isSyncingShiftStatus) {
              "Updating..."
            } else if (shiftOnline) {
              "Go Offline"
            } else {
              "Go Online"
            },
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

    TabRow(selectedTabIndex = if (selectedTab == DispatchListTab.NEW_ORDERS) 0 else 1) {
      Tab(
        selected = selectedTab == DispatchListTab.NEW_ORDERS,
        onClick = { onTabChange(DispatchListTab.NEW_ORDERS) },
        text = { Text("New Orders") },
      )
      Tab(
        selected = selectedTab == DispatchListTab.ACTIVE_DELIVERIES,
        onClick = { onTabChange(DispatchListTab.ACTIVE_DELIVERIES) },
        text = { Text("Active Deliveries") },
      )
    }

    if (!queueError.isNullOrBlank()) {
      PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = queueError,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
        )
      }
    }

    AnimatedContent(targetState = selectedTab, label = "dispatchTabContent") { tab ->
      val tabItems = if (!shiftOnline) {
        emptyList()
      } else {
        when (tab) {
          DispatchListTab.NEW_ORDERS -> orders.filter { it.status.isActive() && it.id !in startedOrderIds }
          DispatchListTab.ACTIVE_DELIVERIES -> orders.filter { it.status.isActive() && it.id in startedOrderIds }
        }
      }
      if (tabItems.isEmpty()) {
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = if (!shiftOnline) {
              "Shift is offline. Go online to load dispatch orders."
            } else if (tab == DispatchListTab.NEW_ORDERS) {
              "No new orders assigned right now."
            } else {
              "No active deliveries right now."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(
          contentPadding = PaddingValues(bottom = 100.dp),
          verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
          items(tabItems, key = { it.id }) { order ->
            OrderCard(
              order = order,
              started = order.id in startedOrderIds,
              onStartDelivery = { onStartDelivery(order) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun OrderCard(
  order: DispatchOrder,
  started: Boolean,
  onStartDelivery: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val urgencyPulse by rememberInfiniteTransition(label = "urgencyPulse").animateFloat(
    initialValue = 0.75f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(900),
      repeatMode = RepeatMode.Reverse,
    ),
    label = "urgencyPulseAlpha",
  )

  PremiumCard(
    modifier = modifier
      .fillMaxWidth()
      .animateContentSize(),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          text = "Order ${order.orderNumber}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
        )
        StatusBadge(order.status)
      }

      if (order.commissionCedis > 0) {
        Text(
          text = "Commission GHS ${formatMoney(order.commissionCedis)} (${formatMoney(order.commissionRatePercent)}%)",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.SemiBold,
        )
      }

      val paymentHint = paymentSummary(order)
      if (paymentHint != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          androidx.compose.material3.Icon(
            Icons.Default.AttachMoney,
            contentDescription = "Payment instruction",
            tint = if (order.requiresCollection) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
          )
          Text(
            text = paymentHint,
            style = MaterialTheme.typography.labelLarge,
            color = if (order.requiresCollection) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      if (order.status == DeliveryStatus.READY_FOR_PICKUP) {
        Text(
          text = "Urgent • pickup ready ${ageLabel(order.createdAt)}",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.error,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.graphicsLayer { alpha = urgencyPulse },
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Icon(
          Icons.Default.Person,
          contentDescription = "Customer name",
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = order.customerName,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Icon(
          Icons.Default.Phone,
          contentDescription = "Customer phone number",
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = order.customerPhone.ifBlank { "Phone unavailable" },
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Icon(
          Icons.Default.Place,
          contentDescription = "Delivery address",
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = order.address,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Button(
        onClick = onStartDelivery,
        modifier = Modifier.fillMaxWidth(),
        enabled = order.status.isActive(),
      ) {
        Text(if (started) "Open Delivery" else "Start Delivery")
      }
    }
  }
}

private fun ageLabel(createdAt: String): String {
  val created = runCatching { Instant.parse(createdAt.trim()) }.getOrNull() ?: return "now"
  val mins = Duration.between(created, Instant.now()).toMinutes().coerceAtLeast(0)
  return if (mins < 60) "$mins min ago" else "${mins / 60}h ago"
}

private fun formatMoney(value: Double): String {
  return String.format("%.2f", value)
}

private fun paymentSummary(order: DispatchOrder): String? {
  if (order.requiresCollection && order.amountDueCedis > 0) {
    return "Collect GHS ${formatMoney(order.amountDueCedis)} before OTP"
  }
  if (order.paymentMethod == DispatchPaymentMethod.CASH_ON_DELIVERY && order.paymentStatus == DispatchPaymentStatus.PAID) {
    return "Cash-on-delivery already collected"
  }
  return null
}

private fun hasPostNotificationsPermission(context: android.content.Context): Boolean {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
  return ContextCompat.checkSelfPermission(
    context,
    Manifest.permission.POST_NOTIFICATIONS,
  ) == PackageManager.PERMISSION_GRANTED
}
