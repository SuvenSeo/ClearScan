package com.ardeno.clearscan.image

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.pow

object ImageEnhancer {
    fun enhance(input: Bitmap): Bitmap = apply(ScanColorFilter.Auto, input)

    fun apply(filter: ScanColorFilter, input: Bitmap): Bitmap =
        when (filter) {
            ScanColorFilter.Auto -> enhanceAuto(input)
            ScanColorFilter.Original -> input.copy(Bitmap.Config.ARGB_8888, false)
            ScanColorFilter.Grayscale -> applyGrayscale(input)
            ScanColorFilter.HighContrast -> applyHighContrast(input)
            ScanColorFilter.MagicColor -> applyMagicColor(input)
        }

    private fun enhanceAuto(input: Bitmap): Bitmap {
        val width = input.width
        val height = input.height
        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)

        val meanLuminance = pixels
            .map { pixel -> luminance(pixel) }
            .average()
            .toFloat()

        val gamma = when {
            meanLuminance < 95f -> 0.82f
            meanLuminance > 185f -> 1.12f
            else -> 1.0f
        }

        val shadowLift = if (meanLuminance < 110f) 18 else 0
        val glareCap = if (meanLuminance > 175f) 235 else 255

        for (index in pixels.indices) {
            pixels[index] = adjustPixel(
                pixel = pixels[index],
                gamma = gamma,
                shadowLift = shadowLift,
                glareCap = glareCap
            )
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun applyGrayscale(input: Bitmap): Bitmap {
        val width = input.width
        val height = input.height
        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)
        for (index in pixels.indices) {
            val pixel = pixels[index]
            val gray = luminance(pixel)
            pixels[index] = Color.argb(Color.alpha(pixel), gray, gray, gray)
        }
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun applyHighContrast(input: Bitmap): Bitmap {
        val width = input.width
        val height = input.height
        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)
        for (index in pixels.indices) {
            val pixel = pixels[index]
            val gray = luminance(pixel)
            val bw = if (gray >= 140) 255 else 0
            pixels[index] = Color.argb(Color.alpha(pixel), bw, bw, bw)
        }
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun applyMagicColor(input: Bitmap): Bitmap {
        val width = input.width
        val height = input.height
        val output = input.copy(Bitmap.Config.ARGB_8888, true)
        val pixels = IntArray(width * height)
        output.getPixels(pixels, 0, width, 0, 0, width, height)
        for (index in pixels.indices) {
            val pixel = pixels[index]
            val alpha = Color.alpha(pixel)
            val red = boostChannel(Color.red(pixel))
            val green = boostChannel(Color.green(pixel))
            val blue = boostChannel(Color.blue(pixel))
            pixels[index] = Color.argb(alpha, red, green, blue)
        }
        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }

    private fun boostChannel(channel: Int): Int {
        val centered = (channel - 128) * 1.25f + 128f
        return centered.toInt().coerceIn(0, 255)
    }

    private fun luminance(pixel: Int): Int {
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        return ((red * 0.299) + (green * 0.587) + (blue * 0.114)).toInt()
    }

    private fun adjustPixel(
        pixel: Int,
        gamma: Float,
        shadowLift: Int,
        glareCap: Int
    ): Int {
        val alpha = Color.alpha(pixel)
        val red = applyTone(Color.red(pixel), gamma, shadowLift, glareCap)
        val green = applyTone(Color.green(pixel), gamma, shadowLift, glareCap)
        val blue = applyTone(Color.blue(pixel), gamma, shadowLift, glareCap)
        return Color.argb(alpha, red, green, blue)
    }

    private fun applyTone(
        channel: Int,
        gamma: Float,
        shadowLift: Int,
        glareCap: Int
    ): Int {
        val normalized = (channel / 255f).pow(gamma)
        val adjusted = (normalized * 255f).toInt() + shadowLift
        return adjusted.coerceIn(0, glareCap)
    }
}
