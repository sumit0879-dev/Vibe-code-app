package com.vibecode.ide.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MonoFontFamily = FontFamily.Monospace
val UiFontFamily = FontFamily.SansSerif

/** Slightly tightened, confident type scale — feels closer to a modern CLI/AI
 *  product than the default Material spec. */
val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineLarge = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.Bold, fontSize = 25.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.5.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.5.sp),
    labelMedium = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = UiFontFamily, fontWeight = FontWeight.Medium, fontSize = 10.5.sp, letterSpacing = 0.2.sp),
)

/** Editor / code specific text style, configurable font size lives in settings. */
fun codeTextStyle(fontSizeSp: Int) = TextStyle(
    fontFamily = MonoFontFamily,
    fontSize = fontSizeSp.sp,
    lineHeight = (fontSizeSp * 1.55).sp,
)
