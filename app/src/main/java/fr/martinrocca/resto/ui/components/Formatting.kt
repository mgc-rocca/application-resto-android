package fr.martinrocca.resto.ui.components

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val frenchDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH)

fun LocalDate.toFrenchDate(): String = format(frenchDateFormatter)
