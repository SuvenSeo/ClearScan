package com.ardeno.clearscan.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.ardeno.clearscan.R

private val dmSansFontName = GoogleFont("DM Sans")

private val fontProvider = androidx.compose.ui.text.googlefonts.GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

@Composable
fun rememberDmSansFontFamily(): FontFamily {
    return FontFamily(
        Font(googleFont = dmSansFontName, fontProvider = fontProvider, weight = FontWeight.Normal),
        Font(googleFont = dmSansFontName, fontProvider = fontProvider, weight = FontWeight.Medium),
        Font(googleFont = dmSansFontName, fontProvider = fontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = dmSansFontName, fontProvider = fontProvider, weight = FontWeight.Bold)
    )
}
