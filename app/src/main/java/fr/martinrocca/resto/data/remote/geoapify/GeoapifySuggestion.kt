package fr.martinrocca.resto.data.remote.geoapify

data class GeoapifySuggestion(
    val placeId: String,
    val name: String,
    val formattedAddress: String,
    val latitude: Double,
    val longitude: Double,
)
