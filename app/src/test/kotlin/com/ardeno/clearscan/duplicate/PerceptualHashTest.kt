package com.ardeno.clearscan.duplicate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerceptualHashTest {
    @Test
    fun identicalPixelsProduceZeroHammingDistance() {
        val pixels = IntArray(PerceptualHash.HASH_WIDTH * PerceptualHash.HASH_HEIGHT) { 128 }
        val first = PerceptualHash.computeDifferenceHash(
            pixels,
            PerceptualHash.HASH_WIDTH,
            PerceptualHash.HASH_HEIGHT
        )
        val second = PerceptualHash.computeDifferenceHash(
            pixels,
            PerceptualHash.HASH_WIDTH,
            PerceptualHash.HASH_HEIGHT
        )

        assertEquals(0, PerceptualHash.hammingDistance(first, second))
        assertTrue(PerceptualHash.isSimilar(first, second))
    }

    @Test
    fun differentGradientsAreDetectedAsDifferent() {
        val leftDark = IntArray(PerceptualHash.HASH_WIDTH * PerceptualHash.HASH_HEIGHT) { index ->
            val column = index % PerceptualHash.HASH_WIDTH
            if (column < PerceptualHash.HASH_WIDTH / 2) 20 else 220
        }
        val rightDark = IntArray(PerceptualHash.HASH_WIDTH * PerceptualHash.HASH_HEIGHT) { index ->
            val column = index % PerceptualHash.HASH_WIDTH
            if (column < PerceptualHash.HASH_WIDTH / 2) 220 else 20
        }

        val first = PerceptualHash.computeDifferenceHash(
            leftDark,
            PerceptualHash.HASH_WIDTH,
            PerceptualHash.HASH_HEIGHT
        )
        val second = PerceptualHash.computeDifferenceHash(
            rightDark,
            PerceptualHash.HASH_WIDTH,
            PerceptualHash.HASH_HEIGHT
        )

        assertTrue(PerceptualHash.hammingDistance(first, second) > PerceptualHash.DEFAULT_SIMILARITY_THRESHOLD)
        assertFalse(PerceptualHash.isSimilar(first, second))
    }

    @Test
    fun hexRoundTripPreservesHash() {
        val pixels = IntArray(PerceptualHash.HASH_WIDTH * PerceptualHash.HASH_HEIGHT) { it % 255 }
        val hash = PerceptualHash.computeDifferenceHash(
            pixels,
            PerceptualHash.HASH_WIDTH,
            PerceptualHash.HASH_HEIGHT
        )

        assertEquals(hash, PerceptualHash.fromHex(PerceptualHash.toHex(hash)))
    }
}
