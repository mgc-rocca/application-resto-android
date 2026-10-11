package fr.martinrocca.resto

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import fr.martinrocca.resto.data.backup.BackupRepository
import fr.martinrocca.resto.data.remote.google.GooglePlacesService
import fr.martinrocca.resto.data.local.RestoDatabase
import fr.martinrocca.resto.data.photo.PhotoManager
import fr.martinrocca.resto.data.repository.RestaurantRepository

class RestoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Applies to Google’s AppCompat UI activities too.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        // Local SDK configuration only: needed when Android restores a Google autocomplete
        // activity after process death. Never fetches a place or waits for a map/network.
        if (BuildConfig.GOOGLE_PLACES_API_KEY.isNotBlank()) {
            runCatching { appContainer.googlePlacesService.preparePlaces() }
        }
    }

    // Room remains independent of the optional Google SDK configuration.
    val appContainer: AppContainer by lazy {
        val database = RestoDatabase.create(this)
        val photoManager = PhotoManager(this)
        AppContainer(
            database = database,
            restaurantRepository = RestaurantRepository(database, photoManager),
            backupRepository = BackupRepository(this, database, photoManager),
            googlePlacesService = GooglePlacesService(this),
            photoManager = photoManager,
        )
    }
}

data class AppContainer(
    val database: RestoDatabase,
    val restaurantRepository: RestaurantRepository,
    val backupRepository: BackupRepository,
    val googlePlacesService: GooglePlacesService,
    val photoManager: PhotoManager,
)
