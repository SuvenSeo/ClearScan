package com.ardeno.clearscan.ocr

enum class BenchmarkLanguage(
    val label: String,
    val unicodeBlockHint: String
) {
    Sinhala("Sinhala", "U+0D80-U+0DFF"),
    Tamil("Tamil", "U+0B80-U+0BFF")
}

data class OcrBenchmarkCase(
    val language: BenchmarkLanguage,
    val sampleName: String,
    val expectedText: String,
    val actualText: String
)

data class OcrBenchmarkMetrics(
    val language: BenchmarkLanguage,
    val sampleName: String,
    val characterErrorRate: Double,
    val wordErrorRate: Double,
    val characterDistance: Int,
    val wordDistance: Int,
    val expectedCharacters: Int,
    val expectedWords: Int
)

object OcrBenchmark {
    fun evaluate(sample: OcrBenchmarkCase): OcrBenchmarkMetrics {
        val expected = sample.expectedText.normalizedForOcr()
        val actual = sample.actualText.normalizedForOcr()
        val expectedWords = expected.words()
        val actualWords = actual.words()
        val characterDistance = levenshtein(expected.toList(), actual.toList())
        val wordDistance = levenshtein(expectedWords, actualWords)

        return OcrBenchmarkMetrics(
            language = sample.language,
            sampleName = sample.sampleName,
            characterErrorRate = rate(characterDistance, expected.length),
            wordErrorRate = rate(wordDistance, expectedWords.size),
            characterDistance = characterDistance,
            wordDistance = wordDistance,
            expectedCharacters = expected.length,
            expectedWords = expectedWords.size
        )
    }

    fun evaluate(samples: List<OcrBenchmarkCase>): List<OcrBenchmarkMetrics> = samples.map(::evaluate)

    fun summary(metrics: List<OcrBenchmarkMetrics>): String {
        if (metrics.isEmpty()) return "No benchmark samples."
        return metrics
            .groupBy { it.language }
            .entries
            .joinToString(separator = "\n") { (language, languageMetrics) ->
                val cer = languageMetrics.map { it.characterErrorRate }.average()
                val wer = languageMetrics.map { it.wordErrorRate }.average()
                "${language.label}: CER ${cer.percent()}, WER ${wer.percent()} across ${languageMetrics.size} samples"
            }
    }

    private fun rate(
        distance: Int,
        expectedSize: Int
    ): Double = if (expectedSize == 0) {
        if (distance == 0) 0.0 else 1.0
    } else {
        distance.toDouble() / expectedSize.toDouble()
    }

    private fun String.normalizedForOcr(): String = trim()
        .replace(Regex("\\s+"), " ")

    private fun String.words(): List<String> =
        if (isBlank()) emptyList() else split(" ")

    private fun Double.percent(): String = "${(this * 100).coerceAtLeast(0.0).coerceAtMost(999.0).toInt()}%"

    private fun <T> levenshtein(
        left: List<T>,
        right: List<T>
    ): Int {
        if (left.isEmpty()) return right.size
        if (right.isEmpty()) return left.size

        var previous = IntArray(right.size + 1) { it }
        var current = IntArray(right.size + 1)

        for (leftIndex in left.indices) {
            current[0] = leftIndex + 1
            for (rightIndex in right.indices) {
                val substitutionCost = if (left[leftIndex] == right[rightIndex]) 0 else 1
                current[rightIndex + 1] = minOf(
                    current[rightIndex] + 1,
                    previous[rightIndex + 1] + 1,
                    previous[rightIndex] + substitutionCost
                )
            }
            val swap = previous
            previous = current
            current = swap
        }

        return previous[right.size]
    }
}
