package com.ardeno.clearscan.duplicate

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File

class ImagePerceptualHasher {
    fun hashImageFile(path: String): Long? {
        val file = File(path)
        if (!file.exists()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeFile(path, decodeOptions) ?: return null

        return try {
            computeHash(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private fun computeHash(source: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(
            source,
            PerceptualHash.HASH_WIDTH,
            PerceptualHash.HASH_HEIGHT,
            true
        )
        return try {
            val pixels = IntArray(PerceptualHash.HASH_WIDTH * PerceptualHash.HASH_HEIGHT)
            scaled.getPixels(
                pixels,
                0,
                PerceptualHash.HASH_WIDTH,
                0,
                0,
                PerceptualHash.HASH_WIDTH,
                PerceptualHash.HASH_HEIGHT
            )
            val grayscale = pixels.map { pixel ->
                val red = Color.red(pixel)
                val green = Color.green(pixel)
                val blue = Color.blue(pixel)
                ((red * 0.299) + (green * 0.587) + (blue * 0.114)).toInt()
            }.toIntArray()
            PerceptualHash.computeDifferenceHash(
                grayscale,
                PerceptualHash.HASH_WIDTH,
                PerceptualHash.HASH_HEIGHT
            )
        } finally {
            if (scaled !== source) {
                scaled.recycle()
            }
        }
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        val target = PerceptualHash.HASH_WIDTH * 4
        while (width / sampleSize > target || height / sampleSize > target) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
