package fr.martinrocca.resto.domain.model

import java.text.Normalizer
import java.util.Locale

fun normalizeTagName(value: String): String = Normalizer
    .normalize(value.trim(), Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "")
    .lowercase(Locale.FRENCH)
    .replace("\\s+".toRegex(), " ")

fun tagEquivalenceKey(value: String): String {
    priceRangeForTagName(value)?.let { return normalizeTagName(it.label) }
    restaurantCategory(value)?.let { return normalizeTagName(it.label) }
    val normalized = normalizeTagName(value)
    return when {
        normalized.endsWith("ienne") -> normalized.dropLast(2)
        normalized.endsWith("enne") -> normalized.dropLast(2)
        normalized.endsWith("aise") -> normalized.dropLast(1)
        normalized.endsWith("oise") -> normalized.dropLast(1)
        normalized.endsWith("aine") -> normalized.dropLast(1)
        else -> normalized
    }
}

fun canonicalTagName(
    value: String,
    existingNames: Collection<String>,
): String {
    val cleaned = value.trim().replace("\\s+".toRegex(), " ")
    if (cleaned.isEmpty()) return cleaned
    priceRangeForTagName(cleaned)?.let { return it.label }
    restaurantCategory(cleaned)?.let { return it.label }
    val normalized = normalizeTagName(cleaned)
    val key = tagEquivalenceKey(cleaned)
    existingNames.firstOrNull { normalizeTagName(it) == normalized }?.let { return it }
    existingNames.firstOrNull { tagEquivalenceKey(it) == key }?.let { return it }

    val closeMatch = existingNames
        .map { candidate -> candidate to editDistance(normalized, normalizeTagName(candidate)) }
        .filter { (_, distance) -> normalized.length >= 5 && distance <= 1 }
        .minByOrNull { (_, distance) -> distance }
        ?.first
    return closeMatch ?: cleaned.replaceFirstChar { it.titlecase(Locale.FRENCH) }
}

fun tagSuggestionScore(query: String, candidate: String): Int {
    val normalizedQuery = normalizeTagName(query)
    val normalizedCandidate = normalizeTagName(candidate)
    if (normalizedQuery.isEmpty()) return 3
    return when {
        normalizedCandidate == normalizedQuery -> 0
        tagEquivalenceKey(candidate) == tagEquivalenceKey(query) -> 0
        normalizedCandidate.startsWith(normalizedQuery) -> 1
        normalizedCandidate.contains(normalizedQuery) -> 2
        editDistance(normalizedQuery, normalizedCandidate) <= 2 -> 3
        else -> Int.MAX_VALUE
    }
}

private fun editDistance(first: String, second: String): Int {
    if (first == second) return 0
    if (first.isEmpty()) return second.length
    if (second.isEmpty()) return first.length
    var previous = IntArray(second.length + 1) { it }
    first.forEachIndexed { firstIndex, firstCharacter ->
        val current = IntArray(second.length + 1)
        current[0] = firstIndex + 1
        second.forEachIndexed { secondIndex, secondCharacter ->
            current[secondIndex + 1] = minOf(
                current[secondIndex] + 1,
                previous[secondIndex + 1] + 1,
                previous[secondIndex] + if (firstCharacter == secondCharacter) 0 else 1,
            )
        }
        previous = current
    }
    return previous[second.length]
}
