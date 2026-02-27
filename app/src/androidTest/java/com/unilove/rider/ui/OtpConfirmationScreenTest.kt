package com.unilove.rider.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.unilove.rider.ui.screen.OtpConfirmationScreen
import org.junit.Rule
import org.junit.Test

class OtpConfirmationScreenTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun verifyButtonEnabledOnlyForSixDigits() {
    var otp by mutableStateOf("")
    composeRule.setContent {
      OtpConfirmationScreen(
        orderNumber = "R001",
        otp = otp,
        loading = false,
        message = null,
        error = null,
        onOtpChange = { otp = it },
        onSubmit = {},
        onBackToDispatch = {},
      )
    }

    composeRule.onNodeWithText("Verify OTP").assertIsNotEnabled()

    composeRule.runOnIdle {
      otp = "123456"
    }

    composeRule.onNodeWithText("Verify OTP").assertIsEnabled()
  }
}
