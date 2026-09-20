package fr.martinrocca.resto.domain.model

/** A single optional price tag, stored with the other tags for backup compatibility. */
enum class PriceRange(val label: String) {
    UNDER_15("<15€"),
    FROM_15_TO_40("15€ - 40€"),
    FROM_40_TO_80("40€-80€"),
    OVER_80(">80€"),
}

private fun priceTagKey(name: String): String = normalizeTagName(name)
    .replace("\\s+".toRegex(), "")
    .replace('–', '-').replace('—', '-').replace('‑', '-')

fun priceRangeForTagName(name: String): PriceRange? =
    PriceRange.entries.firstOrNull { priceTagKey(it.label) == priceTagKey(name) }

val Restaurant.priceRange: PriceRange?
    get() = tags.firstNotNullOfOrNull { priceRangeForTagName(it.name) }

fun withPriceRange(tags: List<String>, range: PriceRange?): List<String> =
    tags.filter { priceRangeForTagName(it) == null } + listOfNotNull(range?.label)
