package com.unilove.rider.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
  primary = BrandRed,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFFFE4EA),
  onPrimaryContainer = BrandRedDeep,
  secondary = AccentBlue,
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFE1EEFF),
  onSecondaryContainer = Color(0xFF06396F),
  tertiary = AccentGreen,
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFE2F8EF),
  onTertiaryContainer = Color(0xFF0F6A45),
  background = LightBackground,
  onBackground = InkLight,
  surface = LightSurface,
  onSurface = InkLight,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = Color(0xFF5D6883),
  outline = LightOutline,
  outlineVariant = Color(0xFFDDE2EE),
  error = Danger,
  onError = Color.White,
  errorContainer = Color(0xFFFFE5E8),
  onErrorContainer = Color(0xFF7D1E28),
)

private val DarkScheme = darkColorScheme(
  primary = BrandRose,
  onPrimary = Color(0xFF29030A),
  primaryContainer = Color(0xFF5A0F23),
  onPrimaryContainer = Color(0xFFFFDCE3),
  secondary = AccentBlue,
  onSecondary = Color(0xFF072443),
  secondaryContainer = Color(0xFF12365F),
  onSecondaryContainer = Color(0xFFD5E8FF),
  tertiary = AccentGreen,
  onTertiary = Color(0xFF03281A),
  tertiaryContainer = Color(0xFF114733),
  onTertiaryContainer = Color(0xFFCFFDE9),
  background = DarkBackground,
  onBackground = InkDark,
  surface = DarkSurface,
  onSurface = InkDark,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = Color(0xFFB2BEDB),
  outline = DarkOutline,
  outlineVariant = Color(0xFF2E3A5A),
  error = Color(0xFFFF778A),
  onError = Color(0xFF44000C),
  errorContainer = Color(0xFF6D1D2A),
  onErrorContainer = Color(0xFFFFDADF),
)

@Composable
fun RiderTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = if (darkTheme) DarkScheme else LightScheme,
    typography = RiderTypography,
    content = content,
  )
}
