package com.unilove.rider.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unilove.rider.model.DeliveryStatus

@Composable
fun UniloveWordmark(modifier: Modifier = Modifier) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(
            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
          ),
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Default.DeliveryDining,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onPrimary,
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
      Text(
        text = "Unilove",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.ExtraBold,
      )
      Text(
        text = "RIDER STAFF",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun PremiumCard(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
  ) {
    Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      content()
    }
  }
}

@Composable
fun StatusBadge(status: DeliveryStatus) {
  val (bg, fg) = when (status) {
    DeliveryStatus.READY_FOR_PICKUP -> Color(0xFFFFE5EA) to Color(0xFFB8002C)
    DeliveryStatus.OUT_FOR_DELIVERY -> Color(0xFFE7F1FF) to Color(0xFF0B4F99)
    DeliveryStatus.DELIVERED -> Color(0xFFE6F8EF) to Color(0xFF0A6E41)
    DeliveryStatus.OTHER -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
  }
  Surface(shape = RoundedCornerShape(999.dp), color = bg) {
    Text(
      text = status.name.replace('_', ' '),
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      style = MaterialTheme.typography.labelSmall,
      color = fg,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
fun WeeklyBarChart(values: List<Int>, modifier: Modifier = Modifier) {
  val max = values.maxOrNull() ?: 0
  Row(
    modifier = modifier.height(76.dp).fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.Bottom,
  ) {
    values.take(7).forEach { value ->
      val normalized = if (max <= 0) 0f else (value.toFloat() / max.toFloat()).coerceIn(0f, 1f)
      val rawHeight = 72f * normalized
      val finalHeight = when {
        value <= 0 -> 0.dp
        rawHeight < 6f -> 6.dp
        else -> rawHeight.dp
      }
      Box(
        modifier = Modifier
          .weight(1f)
          .height(finalHeight)
          .clip(RoundedCornerShape(8.dp))
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)),
      )
    }
  }
}
