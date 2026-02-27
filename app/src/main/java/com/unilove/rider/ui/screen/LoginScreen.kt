package com.unilove.rider.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.unilove.rider.model.RiderLoginMode
import com.unilove.rider.ui.design.PremiumCard
import com.unilove.rider.ui.design.UniloveWordmark

@Composable
fun LoginScreen(
  riderMode: RiderLoginMode,
  riderId: String,
  guestName: String,
  pin: String,
  loading: Boolean,
  error: String?,
  onModeChange: (RiderLoginMode) -> Unit,
  onRiderIdChange: (String) -> Unit,
  onGuestNameChange: (String) -> Unit,
  onPinChange: (String) -> Unit,
  onLogin: () -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        Brush.verticalGradient(
          listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
          ),
        ),
      ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      UniloveWordmark()

      PremiumCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = if (riderMode == RiderLoginMode.GUEST) "Guest Rider Access" else "Rider Login",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
          )
          Text(
            text = if (riderMode == RiderLoginMode.GUEST) {
              "Guest riders can pick delivery orders and earn commission."
            } else {
              "Sign in with your assigned Rider ID and PIN."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )

          TabRow(selectedTabIndex = if (riderMode == RiderLoginMode.STAFF) 0 else 1) {
            Tab(
              selected = riderMode == RiderLoginMode.STAFF,
              onClick = { onModeChange(RiderLoginMode.STAFF) },
              text = { Text("Staff") },
            )
            Tab(
              selected = riderMode == RiderLoginMode.GUEST,
              onClick = { onModeChange(RiderLoginMode.GUEST) },
              text = { Text("Guest") },
            )
          }

          if (riderMode == RiderLoginMode.STAFF) {
            OutlinedTextField(
              value = riderId,
              onValueChange = onRiderIdChange,
              modifier = Modifier.fillMaxWidth(),
              label = { Text("Rider ID") },
              singleLine = true,
              leadingIcon = {
                androidx.compose.material3.Icon(Icons.Default.Person, contentDescription = "Rider ID")
              },
            )

            OutlinedTextField(
              value = pin,
              onValueChange = onPinChange,
              modifier = Modifier.fillMaxWidth(),
              label = { Text("PIN") },
              singleLine = true,
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
              leadingIcon = {
                androidx.compose.material3.Icon(Icons.Default.Lock, contentDescription = "PIN")
              },
            )
          } else {
            OutlinedTextField(
              value = guestName,
              onValueChange = onGuestNameChange,
              modifier = Modifier.fillMaxWidth(),
              label = { Text("Guest Name") },
              singleLine = true,
              leadingIcon = {
                androidx.compose.material3.Icon(Icons.Default.Person, contentDescription = "Guest name")
              },
            )

            OutlinedTextField(
              value = riderId,
              onValueChange = onRiderIdChange,
              modifier = Modifier.fillMaxWidth(),
              label = { Text("Alias (optional)") },
              singleLine = true,
            )
          }

          if (!error.isNullOrBlank()) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(12.dp),
            ) {
              Text(
                text = error,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
              )
            }
          }

          Button(
            onClick = onLogin,
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp),
            enabled = !loading && (
              (riderMode == RiderLoginMode.STAFF && riderId.isNotBlank() && pin.length >= 4) ||
                (riderMode == RiderLoginMode.GUEST && (guestName.isNotBlank() || riderId.isNotBlank()))
              ),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          ) {
            Text(
              text = if (loading) "Signing in..." else if (riderMode == RiderLoginMode.GUEST) "Continue as Guest" else "Log In",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.ExtraBold,
            )
          }
        }
      }
    }
  }
}
