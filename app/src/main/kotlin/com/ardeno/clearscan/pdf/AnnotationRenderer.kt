package com.ardeno.clearscan.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.ardeno.clearscan.model.NormalizedPoint
import com.ardeno.clearscan.model.NormalizedRect
import com.ardeno.clearscan.model.PageAnnotation

object AnnotationRenderer {
    fun render(
        canvas: Canvas,
        annotations: List<PageAnnotation>,
        bitmapWidth: Int,
        bitmapHeight: Int
    ) {
        annotations.forEach { annotation ->
            when (annotation) {
                is PageAnnotation.FreehandSignature -> drawFreehand(
                    canvas = canvas,
                    points = annotation.points,
                    strokeWidthRatio = annotation.strokeWidthRatio,
                    bitmapWidth = bitmapWidth,
                    bitmapHeight = bitmapHeight
                )
                is PageAnnotation.Highlight -> drawHighlight(
                    canvas = canvas,
                    rect = annotation.rect,
                    bitmapWidth = bitmapWidth,
                    bitmapHeight = bitmapHeight
                )
                is PageAnnotation.StickyNote -> drawStickyNote(
                    canvas = canvas,
                    anchor = annotation.anchor,
                    text = annotation.text,
                    bitmapWidth = bitmapWidth,
                    bitmapHeight = bitmapHeight
                )
                is PageAnnotation.Redaction -> drawRedaction(
                    canvas = canvas,
                    rect = annotation.rect,
                    bitmapWidth = bitmapWidth,
                    bitmapHeight = bitmapHeight
                )
            }
        }
    }

    private fun drawFreehand(
        canvas: Canvas,
        points: List<NormalizedPoint>,
        strokeWidthRatio: Float,
        bitmapWidth: Int,
        bitmapHeight: Int
    ) {
        if (points.size < 2) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(32, 48, 72)
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = minOf(bitmapWidth, bitmapHeight) * strokeWidthRatio
        }
        val path = Path().apply {
            val first = points.first()
            moveTo(first.x * bitmapWidth, first.y * bitmapHeight)
            points.drop(1).forEach { point ->
                lineTo(point.x * bitmapWidth, point.y * bitmapHeight)
            }
        }
        canvas.drawPath(path, paint)
    }

    private fun drawHighlight(
        canvas: Canvas,
        rect: NormalizedRect,
        bitmapWidth: Int,
        bitmapHeight: Int
    ) {
        val normalized = rect.normalized()
        if (!normalized.isLargeEnough()) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(96, 255, 235, 59)
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            normalized.left * bitmapWidth,
            normalized.top * bitmapHeight,
            normalized.right * bitmapWidth,
            normalized.bottom * bitmapHeight,
            paint
        )
    }

    private fun drawRedaction(
        canvas: Canvas,
        rect: NormalizedRect,
        bitmapWidth: Int,
        bitmapHeight: Int
    ) {
        val normalized = rect.normalized()
        if (!normalized.isLargeEnough()) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.drawRect(
            normalized.left * bitmapWidth,
            normalized.top * bitmapHeight,
            normalized.right * bitmapWidth,
            normalized.bottom * bitmapHeight,
            paint
        )
    }

    private fun drawStickyNote(
        canvas: Canvas,
        anchor: NormalizedPoint,
        text: String,
        bitmapWidth: Int,
        bitmapHeight: Int
    ) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        val shortEdge = minOf(bitmapWidth, bitmapHeight).toFloat()
        val noteWidth = shortEdge * 0.34f
        val padding = shortEdge * 0.03f
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(48, 40, 20)
            textSize = shortEdge * 0.038f
            typeface = Typeface.SANS_SERIF
        }
        val layout = StaticLayout.Builder
            .obtain(cleanText, 0, cleanText.length, textPaint, (noteWidth - padding * 2).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()

        val noteHeight = layout.height + padding * 2
        val anchorX = anchor.x.coerceIn(0f, 1f) * bitmapWidth
        val anchorY = anchor.y.coerceIn(0f, 1f) * bitmapHeight
        var left = anchorX - noteWidth / 2f
        var top = anchorY - noteHeight / 2f
        if (left < padding) left = padding
        if (top < padding) top = padding
        if (left + noteWidth > bitmapWidth - padding) left = bitmapWidth - padding - noteWidth
        if (top + noteHeight > bitmapHeight - padding) top = bitmapHeight - padding - noteHeight

        val noteRect = RectF(left, top, left + noteWidth, top + noteHeight)
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 244, 163)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(214, 188, 72)
            style = Paint.Style.STROKE
            strokeWidth = shortEdge * 0.004f
        }
        canvas.drawRoundRect(noteRect, shortEdge * 0.02f, shortEdge * 0.02f, backgroundPaint)
        canvas.drawRoundRect(noteRect, shortEdge * 0.02f, shortEdge * 0.02f, borderPaint)
        canvas.save()
        canvas.translate(left + padding, top + padding)
        layout.draw(canvas)
        canvas.restore()
    }
}
