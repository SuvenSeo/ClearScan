package com.ardeno.clearscan.duplicate

import com.ardeno.clearscan.testing.RobolectricUnitTest

import android.graphics.Bitmap
import android.graphics.Color
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ImagePerceptualHasherTest : RobolectricUnitTest() {
    @Test
    fun missingFileReturnsNull() {
        val hasher = ImagePerceptualHasher()
        assertNull(hasher.hashImageFile("/nonexistent/page.jpg"))
    }

    @Test
    fun hashImageFile_producesStableHexForSyntheticBitmap() {
        val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        for (y in 0 until 32) {
            for (x in 0 until 32) {
                bitmap.setPixel(x, y, Color.rgb(x * 8, y * 8, 128))
            }
        }

        val tempFile = File.createTempFile("perceptual-hash", ".jpg")
        tempFile.deleteOnExit()
        tempFile.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        }
        bitmap.recycle()

        val hasher = ImagePerceptualHasher()
        val firstHash = hasher.hashImageFile(tempFile.absolutePath)
        val secondHash = hasher.hashImageFile(tempFile.absolutePath)

        assertNotNull(firstHash)
        assertEquals(firstHash, secondHash)
        assertEquals(16, PerceptualHash.toHex(firstHash!!).length)
    }
}
