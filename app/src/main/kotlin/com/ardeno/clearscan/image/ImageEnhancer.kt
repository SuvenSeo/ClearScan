package com.ardeno.clearscan.image

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.pow

object ImageEnhancer {
    fun enhance(input: Bitmap): Bitmap {
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
