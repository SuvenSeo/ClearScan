package com.ardeno.clearscan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// SF Pro–inspired scale (sizes match iOS Human Interface Guidelines)
private val AppleTypeScale = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 13.sp
    )
)

@Composable
fun clearScanTypography(): Typography {
    val dmSansFamily = rememberDmSansFontFamily()
    return AppleTypeScale.copy(
        displaySmall = AppleTypeScale.displaySmall.copy(fontFamily = dmSansFamily),
        headlineLarge = AppleTypeScale.headlineLarge.copy(fontFamily = dmSansFamily),
        headlineMedium = AppleTypeScale.headlineMedium.copy(fontFamily = dmSansFamily),
        headlineSmall = AppleTypeScale.headlineSmall.copy(fontFamily = dmSansFamily),
        titleLarge = AppleTypeScale.titleLarge.copy(fontFamily = dmSansFamily),
        titleMedium = AppleTypeScale.titleMedium.copy(fontFamily = dmSansFamily),
        titleSmall = AppleTypeScale.titleSmall.copy(fontFamily = dmSansFamily),
        bodyLarge = AppleTypeScale.bodyLarge.copy(fontFamily = dmSansFamily),
        bodyMedium = AppleTypeScale.bodyMedium.copy(fontFamily = dmSansFamily),
        bodySmall = AppleTypeScale.bodySmall.copy(fontFamily = dmSansFamily),
        labelLarge = AppleTypeScale.labelLarge.copy(fontFamily = dmSansFamily),
        labelMedium = AppleTypeScale.labelMedium.copy(fontFamily = dmSansFamily),
        labelSmall = AppleTypeScale.labelSmall.copy(fontFamily = dmSansFamily)
    )
}
