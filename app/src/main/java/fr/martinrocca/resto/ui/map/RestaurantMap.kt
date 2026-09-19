package fr.martinrocca.resto.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import fr.martinrocca.resto.domain.model.RatingBand
import fr.martinrocca.resto.domain.model.Restaurant
import fr.martinrocca.resto.domain.model.ratingBand
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.layers.Property.ICON_ANCHOR_BOTTOM
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

@Composable
fun RestaurantMap(
    restaurants: List<Restaurant>,
    cameraTarget: MapTarget?,
    onRestaurantClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember { MapView(context) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var markerStyle by remember { mutableStateOf<Style?>(null) }
    val currentOnRestaurantClick by rememberUpdatedState(onRestaurantClick)

    DisposableEffect(mapView, lifecycle) {
        var started = false
        var resumed = false
        var destroyed = false
        mapView.onCreate(null)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            mapView.onStart()
            started = true
        }
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            mapView.onResume()
            resumed = true
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!started) {
                    mapView.onStart()
                    started = true
                }
                Lifecycle.Event.ON_RESUME -> if (!resumed) {
                    mapView.onResume()
                    resumed = true
                }
                Lifecycle.Event.ON_PAUSE -> if (resumed) {
                    mapView.onPause()
                    resumed = false
                }
                Lifecycle.Event.ON_STOP -> if (started) {
                    mapView.onStop()
                    started = false
                }
                Lifecycle.Event.ON_DESTROY -> if (!destroyed) {
                    destroyed = true
                    mapView.onDestroy()
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            mapView.visibility = View.INVISIBLE
            lifecycle.removeObserver(observer)
            if (!destroyed) {
                if (resumed) mapView.onPause()
                if (started) mapView.onStop()
                destroyed = true
                mapView.onDestroy()
            }
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                visibility = View.VISIBLE
                getMapAsync { mapLibreMap ->
                    map = mapLibreMap
                    mapLibreMap.uiSettings.isCompassEnabled = true
                    mapLibreMap.uiSettings.isAttributionEnabled = true
                    mapLibreMap.setStyle(
                        Style.Builder().fromUri("asset://resto-map-style.json"),
                    ) { style ->
                        addMarkerAssets(style, context.resources.displayMetrics.density)
                        mapLibreMap.cameraPosition = initialCamera(restaurants)
                        markerStyle = style
                    }
                }
            }
        },
        modifier = modifier,
    )

    LaunchedEffect(markerStyle, restaurants) {
        markerStyle
            ?.getSourceAs<GeoJsonSource>(MARKER_SOURCE_ID)
            ?.setGeoJson(restaurants.toFeatureCollection())
    }

    DisposableEffect(map) {
        val currentMap = map
        if (currentMap == null) return@DisposableEffect onDispose {}
        val clickListener = MapLibreMap.OnMapClickListener { point ->
            val feature = currentMap.queryRenderedFeatures(
                currentMap.projection.toScreenLocation(point),
                MARKER_LAYER_ID,
            ).firstOrNull()
            feature?.getStringProperty(RESTAURANT_ID_PROPERTY)?.let(currentOnRestaurantClick)
            feature != null
        }
        currentMap.addOnMapClickListener(clickListener)
        onDispose { currentMap.removeOnMapClickListener(clickListener) }
    }

    LaunchedEffect(map, cameraTarget) {
        val target = cameraTarget ?: return@LaunchedEffect
        map?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(target.latitude, target.longitude),
                target.zoom,
            ),
            700,
        )
    }
}

private fun addMarkerAssets(style: Style, density: Float) {
    (1..10).forEach { rating ->
        style.addImage(
            markerIconId(rating),
            createMarkerBitmap(
                color = markerColor(rating),
                label = rating.toString(),
                density = density,
            ),
        )
    }
    style.addImage(
        WISHLIST_ICON_ID,
        createMarkerBitmap(
            color = markerColor(null),
            label = "+",
            density = density,
        ),
    )
    style.addSource(
        GeoJsonSource(
            MARKER_SOURCE_ID,
            FeatureCollection.fromFeatures(emptyList<Feature>()),
        ),
    )
    style.addLayer(
        SymbolLayer(MARKER_LAYER_ID, MARKER_SOURCE_ID).withProperties(
            iconImage(get(ICON_ID_PROPERTY)),
            iconAnchor(ICON_ANCHOR_BOTTOM),
            iconAllowOverlap(true),
            iconIgnorePlacement(true),
        ),
    )
}

private fun List<Restaurant>.toFeatureCollection(): FeatureCollection = FeatureCollection.fromFeatures(
    mapNotNull { restaurant ->
        val latitude = restaurant.latitude ?: return@mapNotNull null
        val longitude = restaurant.longitude ?: return@mapNotNull null
        Feature.fromGeometry(Point.fromLngLat(longitude, latitude)).apply {
            addStringProperty(RESTAURANT_ID_PROPERTY, restaurant.id)
            addStringProperty(
                ICON_ID_PROPERTY,
                restaurant.latestVisit?.overallRating?.let(::markerIconId) ?: WISHLIST_ICON_ID,
            )
        }
    },
)

private fun markerIconId(rating: Int): String = "resto-rating-$rating"

private fun initialCamera(restaurants: List<Restaurant>): CameraPosition {
    val firstRestaurant = restaurants.firstOrNull { it.latitude != null && it.longitude != null }
    return CameraPosition.Builder()
        .target(
            LatLng(
                firstRestaurant?.latitude ?: 48.8566,
                firstRestaurant?.longitude ?: 2.3522,
            ),
        )
        .zoom(if (firstRestaurant == null) 10.5 else 12.5)
        .build()
}

private fun markerColor(rating: Int?): Int = when (ratingBand(rating)) {
    RatingBand.VERY_LOW -> Color.rgb(182, 106, 92)
    RatingBand.LOW -> Color.rgb(198, 151, 79)
    RatingBand.GOOD -> Color.rgb(126, 149, 106)
    RatingBand.VERY_GOOD -> Color.rgb(79, 127, 105)
    RatingBand.EXCEPTIONAL -> Color.rgb(49, 95, 80)
    RatingBand.WISHLIST -> Color.rgb(116, 111, 105)
}

private fun createMarkerBitmap(
    color: Int,
    label: String,
    density: Float,
): Bitmap {
    val width = (54 * density).toInt().coerceAtLeast(54)
    val height = (66 * density).toInt().coerceAtLeast(66)
    val centerX = width / 2f
    val circleRadius = width * 0.38f
    val circleCenterY = circleRadius + 3 * density
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val pointer = Path().apply {
            moveTo(centerX - circleRadius * 0.52f, circleCenterY + circleRadius * 0.62f)
            lineTo(centerX, height.toFloat())
            lineTo(centerX + circleRadius * 0.52f, circleCenterY + circleRadius * 0.62f)
            close()
        }
        canvas.drawPath(pointer, markerPaint)
        canvas.drawCircle(centerX, circleCenterY, circleRadius, markerPaint)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = circleRadius * 0.86f
            typeface = Typeface.DEFAULT_BOLD
        }
        val textY = circleCenterY - (textPaint.ascent() + textPaint.descent()) / 2f
        canvas.drawText(label, centerX, textY, textPaint)
    }
}

private const val MARKER_SOURCE_ID = "resto-restaurants-source"
private const val MARKER_LAYER_ID = "resto-restaurants-layer"
private const val RESTAURANT_ID_PROPERTY = "restaurantId"
private const val ICON_ID_PROPERTY = "iconId"
private const val WISHLIST_ICON_ID = "resto-wishlist"
