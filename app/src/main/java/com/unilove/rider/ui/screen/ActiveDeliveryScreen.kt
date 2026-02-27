package com.unilove.rider.ui.screen

import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unilove.rider.model.DispatchPaymentStatus
import com.unilove.rider.model.DispatchOrder
import com.unilove.rider.ui.design.PremiumCard
import com.unilove.rider.ui.design.StatusBadge

@Composable
fun ActiveDeliveryScreen(
  order: DispatchOrder?,
  arrived: Boolean,
  isConfirmingCollection: Boolean,
  collectionMessage: String?,
  collectionError: String?,
  onArrived: () -> Unit,
  onConfirmCollection: () -> Unit,
  onMarkDelivered: () -> Unit,
) {
  val context = LocalContext.current

  if (order == null) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "No active order selected. Open one from Dispatch.",
        style = MaterialTheme.typography.bodyMedium,
      )
    }
    return
  }

  fun openDial() {
    val number = order.customerPhone.filter { it.isDigit() || it == '+' }
    if (number.isBlank()) {
      Toast.makeText(context, "Customer phone unavailable", Toast.LENGTH_SHORT).show()
      return
    }
    try {
      context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    } catch (_: ActivityNotFoundException) {
      Toast.makeText(context, "No phone app available", Toast.LENGTH_SHORT).show()
    }
  }

  fun openMap() {
    if (order.address.isBlank()) {
      Toast.makeText(context, "Address unavailable", Toast.LENGTH_SHORT).show()
      return
    }
    try {
      context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(order.address)}")))
    } catch (_: ActivityNotFoundException) {
      Toast.makeText(context, "No map app available", Toast.LENGTH_SHORT).show()
    }
  }

  val collectionCompleted = !order.requiresCollection ||
    order.paymentStatus == DispatchPaymentStatus.PAID ||
    order.amountDueCedis <= 0

  Column(
    modifier = Modifier
      .imePadding()
      .navigationBarsPadding()
      .padding(bottom = 84.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    PremiumCard(modifier = Modifier.fillMaxWidth()) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Order ${order.orderNumber}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
          StatusBadge(order.status)
        }
        Text(order.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(order.customerPhone, style = MaterialTheme.typography.bodyMedium)
        Text(order.address, style = MaterialTheme.typography.bodyMedium)
        if (order.commissionCedis > 0) {
          Text(
            text = "Commission: GHS ${formatMoney(order.commissionCedis)} (${formatMoney(order.commissionRatePercent)}%)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedButton(onClick = { openDial() }, modifier = Modifier.weight(1f)) {
            androidx.compose.material3.Icon(Icons.Default.Call, contentDescription = "Call customer")
            Text(" Call")
          }
          OutlinedButton(onClick = { openMap() }, modifier = Modifier.weight(1f)) {
            androidx.compose.material3.Icon(Icons.Default.Map, contentDescription = "Open map")
            Text(" Map")
          }
        }
      }
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (order.requiresCollection && !collectionCompleted) {
          Text(
            text = "Collect GHS ${formatMoney(order.amountDueCedis)} from customer before OTP verification.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
          )
          Button(
            onClick = onConfirmCollection,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isConfirmingCollection,
          ) {
            androidx.compose.material3.Icon(Icons.Default.Payments, contentDescription = "Confirm collection")
            Text(if (isConfirmingCollection) " Confirming payment..." else " I Have Collected Payment")
          }
        } else if (order.requiresCollection) {
          Text(
            text = "Payment confirmed. Proceed with OTP handover.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.SemiBold,
          )
        } else {
          Text(
            text = "No cash collection required for this order.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        if (!collectionMessage.isNullOrBlank()) {
          Text(
            text = collectionMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.SemiBold,
          )
        }

        if (!collectionError.isNullOrBlank()) {
          Text(
            text = collectionError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }

        Button(
          onClick = onArrived,
          modifier = Modifier.fillMaxWidth(),
        ) {
          androidx.compose.material3.Icon(Icons.Default.PinDrop, contentDescription = "Mark arrival")
          Text(if (arrived) " Arrived at Location" else " Mark Arrived at Location")
        }

        Button(
          onClick = onMarkDelivered,
          modifier = Modifier.fillMaxWidth(),
          enabled = arrived && collectionCompleted,
        ) {
          androidx.compose.material3.Icon(Icons.Default.TaskAlt, contentDescription = "Mark delivered")
          Text(" Proceed to OTP")
        }

        if (!arrived) {
          Text(
            text = "Mark arrival first before OTP delivery confirmation.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (arrived && !collectionCompleted) {
          Text(
            text = "Confirm payment collection before moving to OTP.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(2.dp))
  }
}

private fun formatMoney(value: Double): String {
  return String.format("%.2f", value)
}
