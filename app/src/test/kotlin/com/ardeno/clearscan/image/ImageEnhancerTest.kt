package com.ardeno.clearscan.image

import android.graphics.Bitmap
import android.graphics.Color
import com.ardeno.clearscan.testing.RobolectricUnitTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageEnhancerTest : RobolectricUnitTest() {
    @Test
    fun grayscaleAndHighContrastProduceDifferentPixels() {
        val source = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.rgb(120, 80, 200))

        val grayscale = ImageEnhancer.apply(ScanColorFilter.Grayscale, source)
        val highContrast = ImageEnhancer.apply(ScanColorFilter.HighContrast, source)

        var sameCount = 0
        var diffCount = 0
        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                val gray = grayscale.getPixel(x, y)
                val contrast = highContrast.getPixel(x, y)
                if (gray == contrast) sameCount++ else diffCount++
                assertTrue(Color.red(gray) == Color.green(gray) && Color.green(gray) == Color.blue(gray))
                assertTrue(
                    Color.red(contrast) == 0 || Color.red(contrast) == 255
                )
                assertTrue(Color.red(contrast) == Color.green(contrast) && Color.green(contrast) == Color.blue(contrast))
            }
        }

        assertTrue(diffCount > 0)
        assertFalse(sameCount == source.width * source.height)
        assertNotEquals(grayscale.getPixel(0, 0), highContrast.getPixel(0, 0))

        grayscale.recycle()
        highContrast.recycle()
        source.recycle()
    }

    @Test
    fun originalReturnsCopyWithoutToneChangeIntent() {
        val source = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        source.setPixel(0, 0, Color.rgb(10, 20, 30))
        source.setPixel(1, 0, Color.rgb(200, 100, 50))
        source.setPixel(0, 1, Color.rgb(0, 255, 128))
        source.setPixel(1, 1, Color.rgb(255, 255, 255))

        val original = ImageEnhancer.apply(ScanColorFilter.Original, source)
        assertNotEquals(source, original)
        for (y in 0 until 2) {
            for (x in 0 until 2) {
                org.junit.Assert.assertEquals(source.getPixel(x, y), original.getPixel(x, y))
            }
        }
        original.recycle()
        source.recycle()
    }
}
