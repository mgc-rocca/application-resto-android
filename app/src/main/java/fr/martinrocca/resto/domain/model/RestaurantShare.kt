package fr.martinrocca.resto.domain.model

import java.net.URLEncoder

fun Restaurant.shareText(): String = buildString {
    appendLine(name)
    appendLine(address)
    if (cuisineTags.isNotEmpty()) appendLine("Cuisine : ${cuisineTags.joinToString(" · ") { it.name }}")
    if (categories.isNotEmpty()) appendLine("Tags : ${categories.joinToString(" · ") { it.label }}")
    if (michelinStatus != MichelinStatus.ABSENT) appendLine(michelinStatus.label)
    priceRange?.let { appendLine("Prix : ${it.label}") }
    appendLine()
    if (latitude != null && longitude != null) {
        append("https://www.openstreetmap.org/?mlat=$latitude&mlon=$longitude#map=17/$latitude/$longitude")
    } else {
        val query = URLEncoder.encode("$name $address", "UTF-8")
        append("https://www.openstreetmap.org/search?query=$query")
    }
}
