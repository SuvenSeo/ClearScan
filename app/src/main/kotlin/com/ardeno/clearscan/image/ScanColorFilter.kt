package com.ardeno.clearscan.image

enum class ScanColorFilter {
    Auto,
    Original,
    Grayscale,
    HighContrast,
    MagicColor;

    companion object {
        fun fromName(name: String?): ScanColorFilter =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: Auto
    }
}
