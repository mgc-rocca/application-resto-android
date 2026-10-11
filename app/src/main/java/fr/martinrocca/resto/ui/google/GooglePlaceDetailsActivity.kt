package fr.martinrocca.resto.ui.google

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.PlaceDetailsCompactFragment
import com.google.android.libraries.places.widget.PlaceLoadListener
import com.google.android.libraries.places.widget.model.Orientation
import fr.martinrocca.resto.R
import fr.martinrocca.resto.RestoApplication

/** Only Google's UI Kit renders Google text. It is never copied into the personal journal. */
class GooglePlaceDetailsActivity : FragmentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val light = Configuration(newBase.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
        }
        super.attachBaseContext(newBase.createConfigurationContext(light))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val density = resources.displayMetrics.density
        val padding = (20 * density).toInt()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            setBackgroundColor(android.graphics.Color.WHITE)
        }
        val message = TextView(this).apply {
            text = "Informations Google Maps"
            textSize = 18f
            setTextColor(android.graphics.Color.rgb(32, 37, 34))
        }
        layout.addView(message)
        val host = FragmentContainerView(this).apply { id = R.id.google_place_details }
        layout.addView(host, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        layout.addView(Button(this).apply {
            text = "Revenir à Resto"
            setOnClickListener { finish() }
        })
        setContentView(layout)

        val service = (application as RestoApplication).appContainer.googlePlacesService
        val error = service.preparePlaces()
        val placeId = intent.getStringExtra(EXTRA_PLACE_ID)
        if (error != null || placeId.isNullOrBlank()) {
            message.text = error ?: "Établissement Google non associé."
            return
        }
        try {
            val existing = supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? PlaceDetailsCompactFragment
            val details = existing ?: PlaceDetailsCompactFragment.newInstance(
                Orientation.VERTICAL,
                listOf(PlaceDetailsCompactFragment.Content.ADDRESS),
                R.style.Theme_Resto_Places,
            )
            details.setPlaceLoadListener(object : PlaceLoadListener {
                override fun onSuccess(place: Place) { message.text = "Informations Google Maps" }
                override fun onFailure(e: Exception) {
                    message.text = "Informations Google indisponibles. Votre fiche Resto reste accessible."
                    host.visibility = View.GONE
                }
            })
            if (existing == null) {
                supportFragmentManager.beginTransaction().replace(host.id, details, FRAGMENT_TAG).commitNow()
                // Exactly one load per explicit user opening; never in onResume or recomposition.
                details.loadWithPlaceId(placeId)
            }
        } catch (_: Exception) {
            message.text = "Informations Google indisponibles. Votre fiche Resto reste accessible."
        }
    }

    companion object {
        private const val EXTRA_PLACE_ID = "googlePlaceId"
        private const val FRAGMENT_TAG = "googlePlaceDetails"
        fun intent(context: Context, placeId: String): Intent =
            Intent(context, GooglePlaceDetailsActivity::class.java).putExtra(EXTRA_PLACE_ID, placeId)
    }
}
