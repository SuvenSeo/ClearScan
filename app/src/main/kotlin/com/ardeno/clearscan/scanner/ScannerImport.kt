package com.ardeno.clearscan.scanner

import android.net.Uri

data class ScannerImport(
    val pdfUri: Uri?,
    val pageUris: List<Uri>
)
