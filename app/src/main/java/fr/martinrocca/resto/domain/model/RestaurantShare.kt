package fr.martinrocca.resto.domain.model

import java.net.URLEncoder
import java.text.Normalizer
import java.util.Locale

private fun sharedHeadingKey(value: String): String = Normalizer
    .normalize(value, Normalizer.Form.NFC)
    .replace('\u00a0', ' ')
    .trim()
    .replace("\\s+".toRegex(), " ")
    .lowercase(Locale.ROOT)

fun Restaurant.shareText(): String {
    val cleanName = name.trim()
    val cleanAddress = address.trim()
    val firstAddressLine = cleanAddress.lineSequence().firstOrNull().orEmpty()
    val nameKey = sharedHeadingKey(cleanName)
    val firstLineKey = sharedHeadingKey(firstAddressLine)
    // Legacy and user addresses may be comma-separated rather than multiline.
    val addressIncludesName = nameKey.isNotEmpty() &&
        (firstLineKey == nameKey || firstLineKey.startsWith("$nameKey,"))
    val identity = listOfNotNull(cleanName.takeUnless { addressIncludesName }, cleanAddress)
        .filter(String::isNotBlank).joinToString("\n")
    val tags = buildList {
        if (cuisineTags.isNotEmpty()) add("Cuisine : ${cuisineTags.joinToString(" · ") { it.name }}")
        priceRange?.let { add("Prix : ${it.label}") }
        if (categories.isNotEmpty()) add("Tags : ${categories.joinToString(" · ") { it.label }}")
        if (michelinStatus != MichelinStatus.ABSENT) add(michelinStatus.label)
    }.joinToString("\n")
    val destination = if (latitude != null && longitude != null) {
        "$latitude,$longitude"
    } else identity.lineSequence().joinToString(" ") { it.trim() }
    val directionsUrl = "https://www.google.com/maps/dir/?api=1&destination=" +
        URLEncoder.encode(destination, "UTF-8") +
        (googlePlaceId?.let { "&destination_place_id=" + URLEncoder.encode(it, "UTF-8") } ?: "")
    return listOf(identity, tags, directionsUrl).filter(String::isNotBlank).joinToString("\n\n")
}
