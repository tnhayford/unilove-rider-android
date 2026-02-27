package com.unilove.rider.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unilove.rider.model.DeliveryMetrics
import com.unilove.rider.ui.design.PremiumCard
import com.unilove.rider.ui.design.WeeklyBarChart

@Composable
fun PerformanceScreen(metrics: DeliveryMetrics) {
  Column(
    modifier = Modifier
      .verticalScroll(rememberScrollState())
      .imePadding()
      .navigationBarsPadding()
      .padding(bottom = 84.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      MetricTile(title = "Today", value = metrics.deliveriesToday.toString(), modifier = Modifier.weight(1f))
      MetricTile(title = "On-time", value = "${metrics.onTimeRatePercent}%", modifier = Modifier.weight(1f))
      MetricTile(title = "Avg", value = "${metrics.averageMinutes}m", modifier = Modifier.weight(1f))
    }

    PremiumCard(modifier = Modifier.fillMaxWidth()) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Weekly Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        WeeklyBarChart(values = metrics.weeklyTrend)
        Text(
          text = "Daily completed deliveries for the last 7 days.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun MetricTile(title: String, value: String, modifier: Modifier = Modifier) {
  PremiumCard(modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
    }
  }
}
