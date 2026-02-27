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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.unilove.rider.model.RiderLoginMode
import com.unilove.rider.ui.design.PremiumCard
import com.unilove.rider.ui.design.UniloveWordmark
import com.unilove.rider.ui.viewmodel.RiderAuthStep

@Composable
fun LoginScreen(
  authStep: RiderAuthStep,
  riderMode: RiderLoginMode,
  riderPhone: String,
  guestName: String,
  referralCode: String,
  otp: String,
  otpPhoneMasked: String,
  otpExpiresInSeconds: Int,
  loading: Boolean,
  error: String?,
  onModeChange: (RiderLoginMode) -> Unit,
  onPhoneChange: (String) -> Unit,
  onGuestNameChange: (String) -> Unit,
  onReferralCodeChange: (String) -> Unit,
  onOtpChange: (String) -> Unit,
  onRequestOtp: () -> Unit,
  onVerifyOtp: () -> Unit,
  onBackToPhone: () -> Unit,
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
            text = if (authStep == RiderAuthStep.ENTER_PHONE) {
              "Enter your phone number to receive a one-time code (OTP) by SMS."
            } else {
              "Enter the 6-digit OTP sent to ${otpPhoneMasked.ifBlank { riderPhone }}."
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

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
              onClick = { /* static chip */ },
              enabled = false,
              label = { Text(if (authStep == RiderAuthStep.ENTER_PHONE) "1. Phone" else "1. Phone done") },
            )
            AssistChip(
              onClick = { /* static chip */ },
              enabled = false,
              label = { Text("2. OTP verify") },
            )
          }

          if (authStep == RiderAuthStep.ENTER_PHONE) {
            OutlinedTextField(
              value = riderPhone,
              onValueChange = onPhoneChange,
              modifier = Modifier.fillMaxWidth(),
              label = { Text("Phone Number") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              leadingIcon = {
                androidx.compose.material3.Icon(Icons.Default.Phone, contentDescription = "Phone")
              },
            )

            OutlinedTextField(
              value = guestName,
              onValueChange = onGuestNameChange,
              modifier = Modifier.fillMaxWidth(),
              label = { Text(if (riderMode == RiderLoginMode.GUEST) "Guest Rider Name" else "Rider Name") },
              singleLine = true,
              leadingIcon = {
                androidx.compose.material3.Icon(Icons.Default.Person, contentDescription = "Rider name")
              },
            )

            if (riderMode == RiderLoginMode.GUEST) {
              OutlinedTextField(
                value = referralCode,
                onValueChange = onReferralCodeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Referral Code") },
                singleLine = true,
                leadingIcon = {
                  androidx.compose.material3.Icon(Icons.Default.Badge, contentDescription = "Referral code")
                },
              )
            }
          } else {
            OutlinedTextField(
              value = otp,
              onValueChange = onOtpChange,
              modifier = Modifier.fillMaxWidth(),
              label = { Text("OTP Code") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
              leadingIcon = {
                androidx.compose.material3.Icon(Icons.Default.Dialpad, contentDescription = "OTP code")
              },
            )

            Text(
              text = "OTP expires in ${formatOtpCountdown(otpExpiresInSeconds)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
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

          if (authStep == RiderAuthStep.ENTER_PHONE) {
            Button(
              onClick = onRequestOtp,
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
              enabled = !loading && riderPhone.length >= 10 && (riderMode != RiderLoginMode.GUEST || referralCode.isNotBlank()),
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
            ) {
              Text(
                text = if (loading) "Sending OTP..." else "Send OTP",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
              )
            }
          } else {
            Button(
              onClick = onVerifyOtp,
              modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
              enabled = !loading && otp.length == 6,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
              ),
            ) {
              Text(
                text = if (loading) "Verifying..." else "Verify and Continue",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
              )
            }
            OutlinedButton(
              onClick = onBackToPhone,
              modifier = Modifier.fillMaxWidth(),
              enabled = !loading,
            ) {
              Text("Use another phone")
            }
          }
        }
      }
    }
  }
}

private fun formatOtpCountdown(seconds: Int): String {
  if (seconds <= 0) return "0:00"
  val safe = seconds.coerceAtMost(60 * 60)
  val minutes = safe / 60
  val remainder = safe % 60
  return "${minutes}:${remainder.toString().padStart(2, '0')}"
}
