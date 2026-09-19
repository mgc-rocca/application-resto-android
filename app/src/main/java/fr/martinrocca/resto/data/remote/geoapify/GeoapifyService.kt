package fr.martinrocca.resto.data.remote.geoapify

import android.net.Uri
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class GeoapifyService(
    private val apiKey: String,
) {
    val isConfigured: Boolean
        get() = apiKey.isNotBlank()

    suspend fun autocomplete(
        query: String,
        limit: Int = 8,
    ): List<GeoapifySuggestion> = withContext(Dispatchers.IO) {
        require(isConfigured) {
            "Ajoutez GEOAPIFY_API_KEY dans local.properties pour activer la recherche."
        }
        if (query.trim().length < 3) return@withContext emptyList()

        val uri = Uri.parse(AUTOCOMPLETE_URL)
            .buildUpon()
            .appendQueryParameter("text", query.trim())
            .appendQueryParameter("format", "json")
            .appendQueryParameter("lang", "fr")
            .appendQueryParameter("limit", limit.coerceIn(1, 20).toString())
            .appendQueryParameter("apiKey", apiKey)
            .build()

        val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
        }

        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("Geoapify a répondu avec le code $status.")
            }
            val payload = connection.inputStream.bufferedReader().use { it.readText() }
            parseSuggestions(payload)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseSuggestions(payload: String): List<GeoapifySuggestion> {
        val results = JSONObject(payload).optJSONArray("results") ?: return emptyList()
        return buildList {
            for (index in 0 until results.length()) {
                val result = results.optJSONObject(index) ?: continue
                val latitude = result.optDouble("lat", Double.NaN)
                val longitude = result.optDouble("lon", Double.NaN)
                if (!latitude.isFinite() || !longitude.isFinite()) continue

                val formatted = result.optString("formatted").trim()
                val name = result.optString("name").trim()
                    .ifEmpty { result.optString("address_line1").trim() }
                    .ifEmpty { formatted.substringBefore(',').trim() }
                if (formatted.isEmpty() || name.isEmpty()) continue

                val placeId = result.optString("place_id").trim().ifEmpty {
                    "$latitude,$longitude:$formatted"
                }
                add(
                    GeoapifySuggestion(
                        placeId = placeId,
                        name = name,
                        formattedAddress = formatted,
                        latitude = latitude,
                        longitude = longitude,
                    ),
                )
            }
        }.distinctBy(GeoapifySuggestion::placeId)
    }

    private companion object {
        const val AUTOCOMPLETE_URL = "https://api.geoapify.com/v1/geocode/autocomplete"
    }
}
