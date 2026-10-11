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
        // Applies to Google’s AppCompat UI activities too; does not initialise a Google SDK.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    // No Google initialisation, service check or network request at application startup.
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
