package fr.martinrocca.resto.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

// Permission is checked immediately before requesting a single fix. No background tracking.
@SuppressLint("MissingPermission")
suspend fun currentDeviceLocation(context: Context): Location {
    check(context.hasLocationPermission()) { "Autorisez la localisation dans les paramètres de Resto." }
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    check(LocationManagerCompat.isLocationEnabled(manager)) {
        "Activez la localisation dans les réglages du téléphone."
    }
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    check(providers.isNotEmpty()) { "Aucun service de localisation n’est disponible." }
    val signals = providers.map { CancellationSignal() }
    try {
        return withTimeoutOrNull(20_000L) {
            suspendCancellableCoroutine { continuation ->
                var completed = 0
                fun noLocation() {
                    completed++
                    if (completed == providers.size && continuation.isActive) {
                        continuation.resumeWithException(
                            IllegalStateException("Position introuvable. Réessayez à l’extérieur."),
                        )
                    }
                }
                continuation.invokeOnCancellation { signals.forEach(CancellationSignal::cancel) }
                providers.forEachIndexed { index, provider ->
                    if (continuation.isActive) {
                        try {
                            LocationManagerCompat.getCurrentLocation(
                                manager,
                                provider,
                                signals[index],
                                ContextCompat.getMainExecutor(context),
                            ) { location ->
                                if (continuation.isActive) {
                                    if (location != null) continuation.resume(location) else noLocation()
                                }
                            }
                        } catch (_: SecurityException) {
                            noLocation()
                        } catch (_: IllegalArgumentException) {
                            noLocation()
                        }
                    }
                }
            }
        } ?: error("La recherche de position a expiré. Réessayez à l’extérieur.")
    } finally {
        signals.forEach(CancellationSignal::cancel)
    }
}
