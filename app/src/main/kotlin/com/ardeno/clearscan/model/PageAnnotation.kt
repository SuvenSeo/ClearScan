package com.ardeno.clearscan.model

data class NormalizedPoint(
    val x: Float,
    val y: Float
)

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun normalized(): NormalizedRect {
        val l = minOf(left, right).coerceIn(0f, 1f)
        val t = minOf(top, bottom).coerceIn(0f, 1f)
        val r = maxOf(left, right).coerceIn(0f, 1f)
        val b = maxOf(top, bottom).coerceIn(0f, 1f)
        return NormalizedRect(l, t, r, b)
    }

    fun isLargeEnough(minSize: Float = 0.01f): Boolean {
        val rect = normalized()
        return (rect.right - rect.left) >= minSize && (rect.bottom - rect.top) >= minSize
    }
}

sealed interface PageAnnotation {
    data class FreehandSignature(
        val points: List<NormalizedPoint>,
        val strokeWidthRatio: Float = 0.004f
    ) : PageAnnotation

    data class Highlight(
        val rect: NormalizedRect
    ) : PageAnnotation

    data class StickyNote(
        val anchor: NormalizedPoint,
        val text: String
    ) : PageAnnotation

    data class Redaction(
        val rect: NormalizedRect
    ) : PageAnnotation
}
