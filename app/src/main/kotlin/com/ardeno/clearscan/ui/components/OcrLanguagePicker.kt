package com.ardeno.clearscan.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ardeno.clearscan.ocr.OcrLanguage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OcrLanguagePicker(
    selectedLanguage: OcrLanguage,
    onLanguageSelected: (OcrLanguage) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "OCR language"
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OcrLanguage.entries.forEach { language ->
                FilterChip(
                    selected = selectedLanguage == language,
                    onClick = { onLanguageSelected(language) },
                    label = { Text(language.label) }
                )
            }
        }
        Text(
            text = when (selectedLanguage) {
                OcrLanguage.Latin -> "Uses on-device ML Kit for Latin script."
                OcrLanguage.Sinhala -> "Uses bundled Tesseract sin traineddata. Fully offline."
                OcrLanguage.Tamil -> "Uses bundled Tesseract tam traineddata. Fully offline."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
