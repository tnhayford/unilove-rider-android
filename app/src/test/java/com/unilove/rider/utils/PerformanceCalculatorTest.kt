package com.unilove.rider.utils

import com.google.common.truth.Truth.assertThat
import com.unilove.rider.model.DeliveryStatus
import com.unilove.rider.model.DispatchOrder
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class PerformanceCalculatorTest {

  @Test
  fun `builds deterministic metrics from dispatch orders`() {
    val now = Instant.now()
    val orders = listOf(
      DispatchOrder(
        id = "1",
        orderNumber = "R001",
        customerName = "A",
        customerPhone = "0551111111",
        address = "Addr",
        status = DeliveryStatus.DELIVERED,
        createdAt = now.minus(30, ChronoUnit.MINUTES).toString(),
        updatedAt = now.minus(10, ChronoUnit.MINUTES).toString(),
      ),
      DispatchOrder(
        id = "2",
        orderNumber = "R002",
        customerName = "B",
        customerPhone = "0552222222",
        address = "Addr",
        status = DeliveryStatus.DELIVERED,
        createdAt = now.minus(120, ChronoUnit.MINUTES).toString(),
        updatedAt = now.minus(10, ChronoUnit.MINUTES).toString(),
      ),
      DispatchOrder(
        id = "3",
        orderNumber = "R003",
        customerName = "C",
        customerPhone = "0553333333",
        address = "Addr",
        status = DeliveryStatus.OUT_FOR_DELIVERY,
        createdAt = now.minus(25, ChronoUnit.MINUTES).toString(),
        updatedAt = now.toString(),
      ),
    )

    val metrics = PerformanceCalculator.fromOrders(orders)

    assertThat(metrics.deliveriesToday).isEqualTo(2)
    assertThat(metrics.onTimeRatePercent).isEqualTo(50)
    assertThat(metrics.averageMinutes).isEqualTo(52)
    assertThat(metrics.weeklyTrend).hasSize(7)
    assertThat(metrics.weeklyTrend.last()).isEqualTo(2)
  }

  @Test
  fun `weekly trend groups delivered orders by day`() {
    val now = Instant.now()
    val orders = listOf(
      DispatchOrder(
        id = "1",
        orderNumber = "R001",
        customerName = "A",
        customerPhone = "0551111111",
        address = "Addr",
        status = DeliveryStatus.DELIVERED,
        createdAt = now.minus(35, ChronoUnit.MINUTES).toString(),
        updatedAt = now.minus(5, ChronoUnit.MINUTES).toString(),
      ),
      DispatchOrder(
        id = "2",
        orderNumber = "R002",
        customerName = "B",
        customerPhone = "0552222222",
        address = "Addr",
        status = DeliveryStatus.DELIVERED,
        createdAt = now.minus(1, ChronoUnit.DAYS).minus(50, ChronoUnit.MINUTES).toString(),
        updatedAt = now.minus(1, ChronoUnit.DAYS).toString(),
      ),
    )

    val metrics = PerformanceCalculator.fromOrders(orders)

    assertThat(metrics.weeklyTrend).hasSize(7)
    assertThat(metrics.weeklyTrend[5]).isEqualTo(1)
    assertThat(metrics.weeklyTrend[6]).isEqualTo(1)
  }
}
