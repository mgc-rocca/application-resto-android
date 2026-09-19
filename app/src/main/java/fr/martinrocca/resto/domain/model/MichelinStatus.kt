package fr.martinrocca.resto.domain.model

enum class MichelinStatus(
    val label: String,
    val compactLabel: String,
) {
    ABSENT("Absent", "Absent"),
    PRESENT("Présent au Guide Michelin", "Michelin"),
    ONE_STAR("1 étoile Michelin", "1 ★"),
    TWO_STARS("2 étoiles Michelin", "2 ★"),
    THREE_STARS("3 étoiles Michelin", "3 ★"),
}
