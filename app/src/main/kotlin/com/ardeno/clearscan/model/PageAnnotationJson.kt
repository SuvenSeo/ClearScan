package com.ardeno.clearscan.model

import org.json.JSONArray
import org.json.JSONObject

object PageAnnotationJson {
  fun encodePages(pages: List<List<PageAnnotation>>): String {
    val array = JSONArray()
    pages.forEach { page ->
      val pageArray = JSONArray()
      page.forEach { annotation -> pageArray.put(encode(annotation)) }
      array.put(pageArray)
    }
    return array.toString()
  }

  fun decodePages(raw: String?): List<List<PageAnnotation>> {
    if (raw.isNullOrBlank()) return emptyList()
    val array = JSONArray(raw)
    return buildList {
      for (pageIndex in 0 until array.length()) {
        val pageArray = array.optJSONArray(pageIndex) ?: JSONArray()
        add(decodePage(pageArray))
      }
    }
  }

  private fun decodePage(array: JSONArray): List<PageAnnotation> = buildList {
    for (index in 0 until array.length()) {
      val item = array.optJSONObject(index) ?: continue
      decode(item)?.let(::add)
    }
  }

  private fun encode(annotation: PageAnnotation): JSONObject = when (annotation) {
    is PageAnnotation.FreehandSignature -> JSONObject()
      .put("type", "signature")
      .put("points", encodePoints(annotation.points))
      .put("strokeWidthRatio", annotation.strokeWidthRatio.toDouble())
    is PageAnnotation.Highlight -> JSONObject()
      .put("type", "highlight")
      .put("rect", encodeRect(annotation.rect))
    is PageAnnotation.StickyNote -> JSONObject()
      .put("type", "note")
      .put("anchor", encodePoint(annotation.anchor))
      .put("text", annotation.text)
    is PageAnnotation.Redaction -> JSONObject()
      .put("type", "redaction")
      .put("rect", encodeRect(annotation.rect))
  }

  private fun decode(item: JSONObject): PageAnnotation? = when (item.optString("type")) {
    "signature" -> PageAnnotation.FreehandSignature(
      points = decodePoints(item.optJSONArray("points")),
      strokeWidthRatio = item.optDouble("strokeWidthRatio", 0.004).toFloat()
    )
    "highlight" -> item.optJSONObject("rect")?.let { PageAnnotation.Highlight(decodeRect(it)) }
    "note" -> item.optJSONObject("anchor")?.let { anchor ->
      PageAnnotation.StickyNote(anchor = decodePoint(anchor), text = item.optString("text"))
    }
    "redaction" -> item.optJSONObject("rect")?.let { PageAnnotation.Redaction(decodeRect(it)) }
    else -> null
  }

  private fun encodePoints(points: List<NormalizedPoint>): JSONArray {
    val array = JSONArray()
    points.forEach { array.put(encodePoint(it)) }
    return array
  }

  private fun decodePoints(array: JSONArray?): List<NormalizedPoint> {
    if (array == null) return emptyList()
    return buildList {
      for (index in 0 until array.length()) {
        array.optJSONObject(index)?.let { add(decodePoint(it)) }
      }
    }
  }

  private fun encodePoint(point: NormalizedPoint) = JSONObject()
    .put("x", point.x.toDouble())
    .put("y", point.y.toDouble())

  private fun decodePoint(item: JSONObject) = NormalizedPoint(
    x = item.optDouble("x").toFloat(),
    y = item.optDouble("y").toFloat()
  )

  private fun encodeRect(rect: NormalizedRect) = JSONObject()
    .put("left", rect.left.toDouble())
    .put("top", rect.top.toDouble())
    .put("right", rect.right.toDouble())
    .put("bottom", rect.bottom.toDouble())

  private fun decodeRect(item: JSONObject) = NormalizedRect(
    left = item.optDouble("left").toFloat(),
    top = item.optDouble("top").toFloat(),
    right = item.optDouble("right").toFloat(),
    bottom = item.optDouble("bottom").toFloat()
  )
}
