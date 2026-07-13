package com.ardeno.clearscan.ocr

import android.graphics.Bitmap
import android.graphics.Color

object OcrPreprocessor {
    fun enhanceForOcr(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        applyContrastStretch(pixels)
        val threshold = otsuThreshold(pixels)
        binarize(pixels, threshold)

        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        output.setPixels(pixels, 0, w, 0, 0, w, h)
        return output
    }

    private fun applyContrastStretch(pixels: IntArray) {
        var min = 255
        var max = 0
        for (i in pixels.indices) {
            val luma = luminance(pixels[i]).toInt().coerceIn(0, 255)
            if (luma < min) min = luma
            if (luma > max) max = luma
        }
        val range = (max - min).coerceAtLeast(1)
        for (i in pixels.indices) {
            val luma = luminance(pixels[i]).toInt().coerceIn(0, 255)
            val stretched = ((luma - min) * 255 / range).coerceIn(0, 255)
            val alpha = Color.alpha(pixels[i])
            pixels[i] = Color.argb(alpha, stretched, stretched, stretched)
        }
    }

    private fun otsuThreshold(pixels: IntArray): Int {
        val histogram = IntArray(256)
        for (i in pixels.indices) {
            val luma = luminance(pixels[i]).toInt().coerceIn(0, 255)
            histogram[luma]++
        }
        val total = pixels.size
        var sum = 0L
        for (i in 0..255) sum += i.toLong() * histogram[i]

        var sumB = 0L
        var wB = 0
        var wF: Int
        var maxVariance = 0.0
        var threshold = 128

        for (i in 0..255) {
            wB += histogram[i]
            if (wB == 0) continue
            wF = total - wB
            if (wF == 0) break
            sumB += i.toLong() * histogram[i]
            val mB = sumB.toDouble() / wB
            val mF = (sum - sumB).toDouble() / wF
            val variance = wB.toDouble() * wF.toDouble() * (mB - mF) * (mB - mF)
            if (variance > maxVariance) {
                maxVariance = variance
                threshold = i
            }
        }
        return threshold
    }

    private fun binarize(pixels: IntArray, threshold: Int) {
        for (i in pixels.indices) {
            val luma = luminance(pixels[i]).toInt().coerceIn(0, 255)
            val binary = if (luma >= threshold) 255 else 0
            val alpha = Color.alpha(pixels[i])
            pixels[i] = Color.argb(alpha, binary, binary, binary)
        }
    }

    private fun luminance(color: Int): Float =
        0.299f * Color.red(color) + 0.587f * Color.green(color) + 0.114f * Color.blue(color)
}
