package com.ardeno.clearscan.pdf

enum class PdfCompressQuality(
    val jpegQuality: Int,
    val maxLongEdge: Int?,
    val label: String
) {
    High(jpegQuality = 88, maxLongEdge = null, label = "High quality"),
    Balanced(jpegQuality = 72, maxLongEdge = 2200, label = "Balanced"),
    Small(jpegQuality = 58, maxLongEdge = 1600, label = "Smallest size")
}
