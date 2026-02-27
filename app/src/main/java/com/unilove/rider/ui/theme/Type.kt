package com.unilove.rider.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val AppDisplay = FontFamily.Serif
private val AppSans = FontFamily.SansSerif

val RiderTypography = Typography(
  headlineLarge = TextStyle(
    fontFamily = AppDisplay,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 32.sp,
    lineHeight = 38.sp,
    letterSpacing = 0.1.sp,
  ),
  headlineMedium = TextStyle(
    fontFamily = AppDisplay,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = 0.1.sp,
  ),
  headlineSmall = TextStyle(
    fontFamily = AppDisplay,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
  ),
  titleLarge = TextStyle(
    fontFamily = AppDisplay,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
  ),
  titleMedium = TextStyle(
    fontFamily = AppSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 17.sp,
    lineHeight = 23.sp,
  ),
  titleSmall = TextStyle(
    fontFamily = AppSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 21.sp,
  ),
  bodyLarge = TextStyle(
    fontFamily = AppSans,
    fontWeight = FontWeight.Medium,
    fontSize = 17.sp,
    lineHeight = 23.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = AppSans,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 21.sp,
  ),
  bodySmall = TextStyle(
    fontFamily = AppSans,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 17.sp,
  ),
  labelLarge = TextStyle(
    fontFamily = AppSans,
    fontWeight = FontWeight.Bold,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.2.sp,
  ),
  labelMedium = TextStyle(
    fontFamily = AppSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
  ),
  labelSmall = TextStyle(
    fontFamily = AppSans,
    fontWeight = FontWeight.SemiBold,
    fontSize = 11.sp,
    lineHeight = 15.sp,
    letterSpacing = 0.2.sp,
  ),
)
