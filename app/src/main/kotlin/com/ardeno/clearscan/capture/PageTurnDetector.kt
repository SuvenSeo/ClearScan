package com.ardeno.clearscan.capture

enum class PageTurnEvent {
    None,
    Motion,
    Settling,
    PageSettled
}

class PageTurnDetector(
    private val stableFrameThreshold: Int = 8,
    private val motionThreshold: Double = 14.0,
    private val stableThreshold: Double = 5.0,
    private val cooldownFrames: Int = 18
) {
    private var previousFrame: ByteArray? = null
    private var stableFrameCount = 0
    private var sawMotion = false
    private var cooldownRemaining = 0

    fun analyze(
        yBuffer: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int
    ): PageTurnEvent {
        val downsampled = downsampleY(yBuffer, width, height, rowStride, targetWidth = 48, targetHeight = 36)

        if (cooldownRemaining > 0) {
            cooldownRemaining -= 1
            previousFrame = downsampled
            return PageTurnEvent.None
        }

        val previous = previousFrame
        previousFrame = downsampled
        if (previous == null || previous.size != downsampled.size) {
            return PageTurnEvent.None
        }

        val difference = meanAbsoluteDiff(previous, downsampled)

        return when {
            difference > motionThreshold -> {
                sawMotion = true
                stableFrameCount = 0
                PageTurnEvent.Motion
            }
            sawMotion && difference < stableThreshold -> {
                stableFrameCount += 1
                if (stableFrameCount >= stableFrameThreshold) {
                    sawMotion = false
                    stableFrameCount = 0
                    cooldownRemaining = cooldownFrames
                    PageTurnEvent.PageSettled
                } else {
                    PageTurnEvent.Settling
                }
            }
            else -> {
                stableFrameCount = 0
                PageTurnEvent.None
            }
        }
    }

    fun reset() {
        previousFrame = null
        stableFrameCount = 0
        sawMotion = false
        cooldownRemaining = 0
    }

    private fun downsampleY(
        yBuffer: ByteArray,
        width: Int,
        height: Int,
        rowStride: Int,
        targetWidth: Int,
        targetHeight: Int
    ): ByteArray {
        val output = ByteArray(targetWidth * targetHeight)
        val xRatio = width.toFloat() / targetWidth
        val yRatio = height.toFloat() / targetHeight

        for (y in 0 until targetHeight) {
            val sourceY = (y * yRatio).toInt().coerceIn(0, height - 1)
            val rowOffset = sourceY * rowStride
            for (x in 0 until targetWidth) {
                val sourceX = (x * xRatio).toInt().coerceIn(0, width - 1)
                output[(y * targetWidth) + x] = yBuffer[rowOffset + sourceX]
            }
        }

        return output
    }

    private fun meanAbsoluteDiff(left: ByteArray, right: ByteArray): Double {
        var total = 0.0
        for (index in left.indices) {
            total += kotlin.math.abs(left[index].toInt() - right[index].toInt())
        }
        return total / left.size
    }
}
