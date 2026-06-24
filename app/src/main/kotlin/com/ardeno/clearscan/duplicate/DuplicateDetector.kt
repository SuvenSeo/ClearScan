package com.ardeno.clearscan.duplicate

import com.ardeno.clearscan.model.ScanDocument

data class DuplicateGroup(
    val documentIds: List<String>
)

class DuplicateDetector(
    private val similarityThreshold: Int = PerceptualHash.DEFAULT_SIMILARITY_THRESHOLD
) {
    fun findDuplicateGroups(documents: List<ScanDocument>): List<DuplicateGroup> {
        val withHashes = documents.filter { it.pageHashes.isNotEmpty() }
        if (withHashes.size < 2) return emptyList()

        val parent = withHashes.associate { it.id to it.id }.toMutableMap()

        fun find(id: String): String {
            var current = id
            while (parent[current] != current) {
                current = parent[current]!!
            }
            return current
        }

        fun union(firstId: String, secondId: String) {
            val firstRoot = find(firstId)
            val secondRoot = find(secondId)
            if (firstRoot != secondRoot) {
                parent[secondRoot] = firstRoot
            }
        }

        for (index in withHashes.indices) {
            for (otherIndex in index + 1 until withHashes.size) {
                if (documentsAreDuplicates(withHashes[index], withHashes[otherIndex])) {
                    union(withHashes[index].id, withHashes[otherIndex].id)
                }
            }
        }

        return withHashes
            .groupBy { find(it.id) }
            .values
            .filter { group -> group.size > 1 }
            .map { group -> DuplicateGroup(group.map { it.id }) }
    }

    fun duplicateDocumentIds(documents: List<ScanDocument>): Set<String> =
        findDuplicateGroups(documents)
            .flatMap { it.documentIds }
            .toSet()

    private fun documentsAreDuplicates(first: ScanDocument, second: ScanDocument): Boolean {
        val firstHashes = first.pageHashes.mapNotNull { runCatching { PerceptualHash.fromHex(it) }.getOrNull() }
        val secondHashes = second.pageHashes.mapNotNull { runCatching { PerceptualHash.fromHex(it) }.getOrNull() }
        if (firstHashes.isEmpty() || secondHashes.isEmpty()) return false

        return firstHashes.any { firstHash ->
            secondHashes.any { secondHash ->
                PerceptualHash.isSimilar(firstHash, secondHash, similarityThreshold)
            }
        }
    }
}
