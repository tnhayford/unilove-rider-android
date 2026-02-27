package com.unilove.rider.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.unilove.rider.ui.screen.LoginScreen
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun loginButtonDisabledWhenInputsAreInvalid() {
    composeRule.setContent {
      LoginScreen(
        riderId = "",
        pin = "",
        loading = false,
        error = null,
        onRiderIdChange = {},
        onPinChange = {},
        onLogin = {},
      )
    }

    composeRule.onNodeWithText("Log In").assertIsNotEnabled()
  }
}
