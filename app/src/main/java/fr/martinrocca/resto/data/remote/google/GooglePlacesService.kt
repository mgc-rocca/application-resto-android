package fr.martinrocca.resto.data.remote.google

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import fr.martinrocca.resto.BuildConfig
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Official SDK only. No names, addresses, ratings, reviews, or photos are fetched here. */
class GooglePlacesService(context: Context) {
    private val context = context.applicationContext
    private val cache = GoogleCoordinateCache()
    private val requestMutex = Mutex()
    private val mutableCoordinates = MutableStateFlow<Map<String, GoogleCoordinates>>(emptyMap())
    val coordinates: StateFlow<Map<String, GoogleCoordinates>> = mutableCoordinates

    fun servicesError(): String? = try {
        val status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
        if (status == ConnectionResult.SUCCESS) null
        else "Services Google indisponibles ou incompatibles. Le Journal et la saisie manuelle restent disponibles."
    } catch (_: Exception) {
        "Services Google indisponibles. Utilisez le Journal et la saisie manuelle."
    }

    fun mapError(): String? = servicesError() ?: runCatching {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        val key = info.metaData?.getString("com.google.android.geo.API_KEY")
        if (key.isNullOrBlank()) "Clé Google Maps absente. Configurez-la pour afficher la carte ; le Journal reste disponible."
        else null
    }.getOrElse { "Configuration Google Maps indisponible. Le Journal reste disponible." }

    fun preparePlaces(): String? {
        servicesError()?.let { return it }
        if (BuildConfig.GOOGLE_PLACES_API_KEY.isBlank()) {
            return "Clé Google Places absente. La saisie manuelle reste disponible."
        }
        return try {
            if (!Places.isInitialized()) {
                Places.initializeWithNewPlacesApiEnabled(context, BuildConfig.GOOGLE_PLACES_API_KEY, Locale.FRENCH)
            }
            null
        } catch (_: Exception) {
            "Google Places n’a pas pu démarrer. La saisie manuelle reste disponible."
        }
    }

    suspend fun coordinatesFor(placeId: String): GoogleCoordinates = requestMutex.withLock {
        cache.get(placeId)?.let { return@withLock it }
        preparePlaces()?.let { error(it) }
        val request = FetchPlaceRequest.builder(placeId, listOf(Place.Field.ID, Place.Field.LOCATION)).build()
        val place = try {
            withTimeout(12_000) { Places.createClient(context).fetchPlace(request).awaitResult().place }
        } catch (_: TimeoutCancellationException) {
            error("Google ne répond pas. Réessayez ou utilisez la saisie manuelle.")
        } catch (error: ApiException) {
            throw IllegalStateException(googleErrorMessage(error), error)
        }
        val location = requireNotNull(place.location) { "Google ne fournit pas de coordonnées pour cet établissement." }
        val result = GoogleCoordinates(placeId, location.latitude, location.longitude)
        cache.put(result)
        mutableCoordinates.value = cache.snapshot()
        result
    }

    /** Called while the map is visible; removes aged coordinates without a network request. */
    fun cachedCoordinates(placeId: String): GoogleCoordinates? = cache.get(placeId)
    fun expireCoordinates() { mutableCoordinates.value = cache.snapshot() }
    fun clearCoordinates() {
        cache.clear()
        mutableCoordinates.value = emptyMap()
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}

fun googleErrorMessage(error: Throwable): String = when ((error as? ApiException)?.statusCode) {
    7 -> "Connexion impossible à Google. Réessayez ou utilisez la saisie manuelle."
    9010 -> "Quota Google atteint. Réessayez plus tard ou utilisez la saisie manuelle."
    9011, 9012 -> "Google a refusé la requête. Vérifiez la clé, les API et les restrictions SHA-1."
    else -> "Recherche Google indisponible. Vérifiez la connexion et la configuration ; la saisie manuelle reste disponible."
}
