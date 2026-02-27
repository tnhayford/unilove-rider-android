package com.unilove.rider.navigation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.unilove.rider.ui.design.PremiumCard
import com.unilove.rider.ui.screen.ActiveDeliveryScreen
import com.unilove.rider.ui.screen.DispatchScreen
import com.unilove.rider.ui.screen.IncidentsScreen
import com.unilove.rider.ui.screen.LoginScreen
import com.unilove.rider.ui.screen.OtpConfirmationScreen
import com.unilove.rider.ui.screen.PerformanceScreen
import com.unilove.rider.ui.screen.ProfileScreen
import com.unilove.rider.ui.screen.SettingsScreen
import com.unilove.rider.ui.viewmodel.RiderAppViewModel

sealed class RiderRoute(val route: String) {
  data object Login : RiderRoute("login")
  data object Dispatch : RiderRoute("dispatch")
  data object Performance : RiderRoute("performance")
  data object Incidents : RiderRoute("incidents")
  data object Profile : RiderRoute("profile")
  data object Settings : RiderRoute("settings")
  data object ActiveDelivery : RiderRoute("active/{orderId}") {
    fun create(orderId: String): String = "active/$orderId"
  }

  data object Otp : RiderRoute("otp/{orderId}") {
    fun create(orderId: String): String = "otp/$orderId"
  }
}

private data class BottomItem(
  val route: RiderRoute,
  val label: String,
  val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
fun RiderApp(vm: RiderAppViewModel) {
  val ui by vm.ui.collectAsStateWithLifecycle()
  val navController = rememberNavController()

  LaunchedEffect(ui.session != null) {
    if (ui.session == null) {
      navController.navigate(RiderRoute.Login.route) {
        popUpTo(RiderRoute.Login.route) { inclusive = true }
      }
    } else {
      navController.navigate(RiderRoute.Dispatch.route) {
        popUpTo(RiderRoute.Login.route) { inclusive = true }
      }
    }
  }

  LaunchedEffect(ui.pendingNavigationOrderId, ui.pendingNavigationToActive) {
    val orderId = ui.pendingNavigationOrderId
    if (!orderId.isNullOrBlank()) {
      if (ui.pendingNavigationToActive) {
        navController.navigate(RiderRoute.ActiveDelivery.create(orderId))
      } else {
        navController.navigate(RiderRoute.Dispatch.route) {
          launchSingleTop = true
          restoreState = true
        }
      }
      vm.consumePendingNavigationOrder()
    }
  }

  LaunchedEffect(ui.otpCompletedOrderId) {
    if (!ui.otpCompletedOrderId.isNullOrBlank()) {
      val poppedToDispatch = navController.popBackStack(RiderRoute.Dispatch.route, inclusive = false)
      if (!poppedToDispatch) {
        navController.navigate(RiderRoute.Dispatch.route) {
          popUpTo(navController.graph.id) { inclusive = false }
          launchSingleTop = true
          restoreState = true
        }
      }
      vm.consumeOtpCompletion()
    }
  }

  val bottomItems = listOf(
    BottomItem(RiderRoute.Dispatch, "Dispatch", Icons.AutoMirrored.Filled.DirectionsBike),
    BottomItem(RiderRoute.Performance, "Performance", Icons.Default.Assessment),
    BottomItem(RiderRoute.Incidents, "Incidents", Icons.Default.Error),
    BottomItem(RiderRoute.Profile, "Profile", Icons.Default.Person),
  )

  val rootRoutes = setOf(
    RiderRoute.Dispatch.route,
    RiderRoute.Performance.route,
    RiderRoute.Incidents.route,
    RiderRoute.Profile.route,
  )

  val backStack by navController.currentBackStackEntryAsState()
  val destination = backStack?.destination
  val route = destination?.route.orEmpty()
  val showBottomBar = ui.session != null && route in rootRoutes
  val showHeader = ui.session != null && route != RiderRoute.Login.route

  Scaffold(
    bottomBar = {
      if (showBottomBar) {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
          bottomItems.forEach { item ->
            NavigationBarItem(
              selected = destination.inHierarchy(item.route.route),
              onClick = {
                navController.navigate(item.route.route) {
                  popUpTo(RiderRoute.Dispatch.route) { saveState = true }
                  launchSingleTop = true
                  restoreState = true
                }
              },
              icon = { Icon(item.icon, contentDescription = item.label) },
              label = { Text(item.label) },
            )
          }
        }
      }
    },
    containerColor = androidx.compose.ui.graphics.Color.Transparent,
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            listOf(
              MaterialTheme.colorScheme.background,
              MaterialTheme.colorScheme.surface,
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
            ),
          ),
        )
        .padding(innerPadding),
    ) {
      if (showHeader) {
        TopHeader(
          title = routeTitle(route),
          online = ui.isOnline,
          queueSize = ui.orders.size,
          isRefreshing = ui.isRefreshingOrders,
          showBack = route !in rootRoutes,
          showRefresh = route == RiderRoute.Dispatch.route,
          onBack = { navController.popBackStack() },
          onRefresh = { vm.refreshOrders(silent = false) },
          onOpenSettings = { navController.navigate(RiderRoute.Settings.route) },
        )
      }

      NavHost(
        navController = navController,
        startDestination = RiderRoute.Login.route,
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 12.dp, vertical = 10.dp),
      ) {
        composable(RiderRoute.Login.route) {
          LoginScreen(
            riderMode = ui.riderMode,
            riderId = ui.riderIdInput,
            guestName = ui.guestNameInput,
            pin = ui.pinInput,
            loading = ui.isAuthenticating,
            error = ui.authError,
            onModeChange = vm::setRiderMode,
            onRiderIdChange = vm::setRiderIdInput,
            onGuestNameChange = vm::setGuestNameInput,
            onPinChange = vm::setPinInput,
            onLogin = vm::login,
          )
        }

        composable(RiderRoute.Dispatch.route) {
          DispatchScreen(
            orders = ui.orders,
            startedOrderIds = ui.startedOrderIds,
            selectedTab = ui.dispatchTab,
            queueError = ui.queueError,
            onTabChange = vm::setDispatchTab,
            onStartDelivery = { order ->
              vm.startDelivery(order.id)
              navController.navigate(RiderRoute.ActiveDelivery.create(order.id))
            },
          )
        }

        composable(
          route = RiderRoute.ActiveDelivery.route,
          arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
          deepLinks = listOf(
            navDeepLink { uriPattern = "unilove://order/{orderId}" },
            navDeepLink { uriPattern = "https://unilove.iderwell.com/rider/order/{orderId}" },
          ),
        ) { entry ->
          val orderId = entry.arguments?.getString("orderId").orEmpty()
          val order = ui.orders.firstOrNull { it.id == orderId }
          val arrived = ui.arrivedOrderIds.contains(orderId)
          ActiveDeliveryScreen(
            order = order,
            arrived = arrived,
            onArrived = { vm.markArrived(orderId) },
            onMarkDelivered = { navController.navigate(RiderRoute.Otp.create(orderId)) },
          )
        }

        composable(
          route = RiderRoute.Otp.route,
          arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) { entry ->
          val orderId = entry.arguments?.getString("orderId").orEmpty()
          val order = ui.orders.firstOrNull { it.id == orderId }
          OtpConfirmationScreen(
            orderNumber = order?.orderNumber ?: orderId,
            otp = ui.otpCode,
            loading = ui.isVerifyingOtp,
            message = ui.otpMessage,
            error = ui.otpError,
            onOtpChange = vm::setOtpCode,
            onSubmit = { vm.verifyOtp(orderId) },
            onBackToDispatch = {
              if (!navController.popBackStack(RiderRoute.Dispatch.route, inclusive = false)) {
                navController.navigate(RiderRoute.Dispatch.route) {
                  popUpTo(navController.graph.id) { inclusive = false }
                  launchSingleTop = true
                  restoreState = true
                }
              }
            },
          )
        }

        composable(RiderRoute.Performance.route) {
          PerformanceScreen(metrics = ui.metrics)
        }

        composable(RiderRoute.Incidents.route) {
          IncidentsScreen(
            draft = ui.incidentDraft.copy(orderId = ui.incidentDraft.orderId ?: ui.selectedOrderId),
            incidents = ui.incidents,
            submitting = ui.isSubmittingIncident,
            message = ui.incidentMessage,
            error = ui.incidentError,
            onOrderIdChange = vm::setIncidentOrderId,
            onCategoryChange = vm::setIncidentCategory,
            onLocationChange = vm::setIncidentLocation,
            onNoteChange = vm::setIncidentNote,
            onSubmit = vm::submitIncident,
          )
        }

        composable(RiderRoute.Profile.route) {
          val session = ui.session
          if (session == null) {
            Box(modifier = Modifier.fillMaxSize())
          } else {
            ProfileScreen(
              session = session,
              shiftStatus = ui.shiftStatus,
              isSyncingShiftStatus = ui.isSyncingShiftStatus,
              statusMessage = ui.profileStatusMessage,
              errorMessage = ui.profileError,
              onToggleShift = vm::toggleShiftStatus,
              onOpenSettings = { navController.navigate(RiderRoute.Settings.route) },
              onLogout = vm::logout,
            )
          }
        }

        composable(RiderRoute.Settings.route) {
          SettingsScreen(
            themeMode = ui.themeMode,
            ringtone = ui.ringtone,
            notificationToneUri = ui.notificationToneUri,
            onThemeChange = vm::setTheme,
            onRingtoneChange = vm::setRingtone,
            onNotificationTonePicked = vm::setNotificationToneUri,
            onBack = { navController.popBackStack() },
          )
        }
      }
    }
  }
}

@Composable
private fun TopHeader(
  title: String,
  online: Boolean,
  queueSize: Int,
  isRefreshing: Boolean,
  showBack: Boolean,
  showRefresh: Boolean,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  val statusColor by animateColorAsState(
    targetValue = if (online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
    label = "networkStatusColor",
  )
  val refreshRotation by if (isRefreshing) {
    rememberInfiniteTransition(label = "refreshSpin").animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(
        animation = tween(durationMillis = 900, easing = LinearEasing),
        repeatMode = RepeatMode.Restart,
      ),
      label = "refreshRotation",
    )
  } else {
    androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
  }

  PremiumCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
      androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
          if (showBack) {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
            }
          }
          Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(top = 9.dp),
          )
        }

        if (showRefresh) {
          IconButton(onClick = onRefresh) {
            Icon(
              Icons.Default.Refresh,
              contentDescription = "Refresh queue",
              modifier = Modifier.graphicsLayer { rotationZ = refreshRotation },
            )
          }
        } else {
          IconButton(onClick = onOpenSettings) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Open app settings",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(20.dp),
            )
          }
        }
      }

      Text(
        text = if (online) "Connected" else "Offline mode: showing cached data",
        style = MaterialTheme.typography.bodySmall,
        color = statusColor,
        fontWeight = FontWeight.SemiBold,
      )

      if (showRefresh) {
        Surface(
          shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Text(
            text = "Assigned orders: $queueSize",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
          )
        }
      }
    }
  }
}

private fun routeTitle(route: String): String {
  return when (route) {
    RiderRoute.Dispatch.route -> "Dispatch Queue"
    RiderRoute.Performance.route -> "Performance"
    RiderRoute.Incidents.route -> "Incidents"
    RiderRoute.Profile.route -> "Profile"
    RiderRoute.Settings.route -> "App Settings"
    RiderRoute.ActiveDelivery.route -> "Active Delivery"
    RiderRoute.Otp.route -> "Delivery OTP"
    else -> "Unilove Rider"
  }
}

private fun NavDestination?.inHierarchy(route: String): Boolean {
  return this?.hierarchy?.any { it.route == route } == true
}
