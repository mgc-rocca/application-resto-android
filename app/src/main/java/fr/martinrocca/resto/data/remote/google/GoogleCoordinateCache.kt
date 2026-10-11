package fr.martinrocca.resto.data.remote.google

/** Google coordinates are deliberately never serialised to Room, SavedState or ZIP. */
data class GoogleCoordinates(val placeId: String, val latitude: Double, val longitude: Double)

/** Process-only cache; monotonic age, at most 12 hours (well below Google's 30-day ceiling). */
class GoogleCoordinateCache(
    private val elapsedMillis: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    private data class Entry(val coordinates: GoogleCoordinates, val savedAt: Long)
    private val entries = linkedMapOf<String, Entry>()

    fun get(placeId: String): GoogleCoordinates? {
        val entry = entries[placeId] ?: return null
        val age = elapsedMillis() - entry.savedAt
        if (age < 0 || age >= MAX_AGE_MILLIS) {
            entries.remove(placeId)
            return null
        }
        return entry.coordinates
    }

    fun put(coordinates: GoogleCoordinates) {
        require(coordinates.placeId.isNotBlank())
        require(coordinates.latitude.isFinite() && coordinates.latitude in -90.0..90.0)
        require(coordinates.longitude.isFinite() && coordinates.longitude in -180.0..180.0)
        entries.remove(coordinates.placeId)
        entries[coordinates.placeId] = Entry(coordinates, elapsedMillis())
        while (entries.size > MAX_ENTRIES) entries.remove(entries.keys.first())
    }

    fun snapshot(): Map<String, GoogleCoordinates> = entries.keys.toList()
        .mapNotNull { id -> get(id)?.let { id to it } }.toMap()

    fun clear() = entries.clear()

    companion object {
        const val MAX_AGE_MILLIS = 12L * 60 * 60 * 1_000
        const val MAX_ENTRIES = 200
    }
}
