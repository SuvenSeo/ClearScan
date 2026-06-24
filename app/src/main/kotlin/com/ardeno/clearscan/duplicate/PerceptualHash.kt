package com.ardeno.clearscan.duplicate

/**
 * Difference hash (dHash) over grayscale pixels.
 * [grayscalePixels] length must be [width] * [height], each value 0..255.
 */
object PerceptualHash {
    const val HASH_WIDTH = 9
    const val HASH_HEIGHT = 8
    const val DEFAULT_SIMILARITY_THRESHOLD = 10

    fun computeDifferenceHash(grayscalePixels: IntArray, width: Int, height: Int): Long {
        require(width == HASH_WIDTH && height == HASH_HEIGHT) {
            "dHash expects ${HASH_WIDTH}x$HASH_HEIGHT pixels, got ${width}x$height"
        }
        require(grayscalePixels.size == width * height) {
            "Pixel count ${grayscalePixels.size} does not match ${width}x$height"
        }

        var hash = 0L
        var bitIndex = 0
        for (row in 0 until height) {
            for (column in 0 until width - 1) {
                val left = grayscalePixels[row * width + column]
                val right = grayscalePixels[row * width + column + 1]
                if (left > right) {
                    hash = hash or (1L shl bitIndex)
                }
                bitIndex += 1
            }
        }
        return hash
    }

    fun hammingDistance(first: Long, second: Long): Int =
        java.lang.Long.bitCount(first xor second)

    fun isSimilar(
        first: Long,
        second: Long,
        threshold: Int = DEFAULT_SIMILARITY_THRESHOLD
    ): Boolean = hammingDistance(first, second) <= threshold

    fun toHex(hash: Long): String = "%016x".format(hash)

    fun fromHex(hex: String): Long = hex.toLong(16)
}
