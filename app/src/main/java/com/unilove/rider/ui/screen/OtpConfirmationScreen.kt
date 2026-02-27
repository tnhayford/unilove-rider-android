package com.unilove.rider.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.unilove.rider.ui.design.PremiumCard
import kotlinx.coroutines.delay

@Composable
fun OtpConfirmationScreen(
  orderNumber: String,
  otp: String,
  loading: Boolean,
  message: String?,
  error: String?,
  onOtpChange: (String) -> Unit,
  onSubmit: () -> Unit,
  onBackToDispatch: () -> Unit,
) {
  LaunchedEffect(message, error, loading) {
    if (!loading && !message.isNullOrBlank() && error.isNullOrBlank()) {
      delay(1600)
      onBackToDispatch()
    }
  }

  PremiumCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .imePadding()
        .navigationBarsPadding()
        .padding(bottom = 84.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(
        text = "OTP Confirmation",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.ExtraBold,
      )
      Text(
        text = "Order $orderNumber",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      OutlinedTextField(
        value = otp,
        onValueChange = onOtpChange,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.NumberPassword,
          imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
          onDone = {
            if (otp.length == 6 && !loading) {
              onSubmit()
            }
          },
        ),
        singleLine = true,
        enabled = !loading,
        label = { Text("6-digit OTP") },
        placeholder = { Text("123456") },
        supportingText = {
          Text(
            text = "${otp.length}/6 digits",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        },
      )

      Button(
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        enabled = otp.length == 6 && !loading,
      ) {
        Text(if (loading) "Verifying..." else "Verify OTP")
      }

      if (!message.isNullOrBlank()) {
        Text(message, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        OutlinedButton(onClick = onBackToDispatch, modifier = Modifier.fillMaxWidth()) {
          Text("Back to Dispatch")
        }
      }
      if (!error.isNullOrBlank()) {
        Text(error, color = MaterialTheme.colorScheme.error)
      }
    }
  }
}
