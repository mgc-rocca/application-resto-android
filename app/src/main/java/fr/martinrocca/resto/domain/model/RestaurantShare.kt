package fr.martinrocca.resto.domain.model

import java.net.URLEncoder

fun Restaurant.shareText(): String = buildString {
    appendLine(name)
    appendLine(address)
    if (tags.isNotEmpty()) appendLine("Cuisine : ${tags.joinToString(" · ") { it.name }}")
    if (michelinStatus != MichelinStatus.ABSENT) appendLine(michelinStatus.label)
    latestVisit?.let { appendLine("Ma note : ${it.overallRating}/10") }
    appendLine()
    if (latitude != null && longitude != null) {
        append("https://www.openstreetmap.org/?mlat=$latitude&mlon=$longitude#map=17/$latitude/$longitude")
    } else {
        val query = URLEncoder.encode("$name $address", "UTF-8")
        append("https://www.openstreetmap.org/search?query=$query")
    }
}
