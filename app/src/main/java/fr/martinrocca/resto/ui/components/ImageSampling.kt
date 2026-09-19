package fr.martinrocca.resto.ui.components

internal fun calculateInSampleSize(
    width: Int,
    height: Int,
    requestedSize: Int,
): Int {
    require(width > 0 && height > 0 && requestedSize > 0)
    val longestSide = maxOf(width, height)
    var sampleSize = 1
    while (longestSide / (sampleSize * 2) >= requestedSize) {
        sampleSize *= 2
    }
    return sampleSize
}
