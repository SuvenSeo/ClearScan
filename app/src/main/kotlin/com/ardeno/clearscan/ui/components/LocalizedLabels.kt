package com.ardeno.clearscan.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ardeno.clearscan.R
import com.ardeno.clearscan.ocr.OcrLanguage
import com.ardeno.clearscan.pdf.PdfCompressQuality

@StringRes
fun OcrLanguage.labelRes(): Int = when (this) {
    OcrLanguage.Latin -> R.string.ocr_language_latin
    OcrLanguage.Sinhala -> R.string.ocr_language_sinhala
    OcrLanguage.Tamil -> R.string.ocr_language_tamil
}

@Composable
fun OcrLanguage.displayLabel(): String = stringResource(labelRes())

@StringRes
fun OcrLanguage.hintRes(): Int = when (this) {
    OcrLanguage.Latin -> R.string.ocr_language_latin_hint
    OcrLanguage.Sinhala -> R.string.ocr_language_sinhala_hint
    OcrLanguage.Tamil -> R.string.ocr_language_tamil_hint
}

@Composable
fun OcrLanguage.displayHint(): String = stringResource(hintRes())

@StringRes
fun PdfCompressQuality.labelRes(): Int = when (this) {
    PdfCompressQuality.High -> R.string.pdf_compress_high
    PdfCompressQuality.Balanced -> R.string.pdf_compress_balanced
    PdfCompressQuality.Small -> R.string.pdf_compress_small
}

@Composable
fun PdfCompressQuality.displayLabel(): String = stringResource(labelRes())
