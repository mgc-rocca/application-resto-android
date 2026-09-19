package fr.martinrocca.resto

import android.app.Application
import fr.martinrocca.resto.data.backup.BackupRepository
import fr.martinrocca.resto.data.remote.geoapify.GeoapifyService
import fr.martinrocca.resto.data.local.RestoDatabase
import fr.martinrocca.resto.data.photo.PhotoManager
import fr.martinrocca.resto.data.repository.RestaurantRepository
import org.maplibre.android.MapLibre

class RestoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
    }

    val appContainer: AppContainer by lazy {
        val database = RestoDatabase.create(this)
        val photoManager = PhotoManager(this)
        AppContainer(
            database = database,
            restaurantRepository = RestaurantRepository(database, photoManager),
            backupRepository = BackupRepository(this, database, photoManager),
            geoapifyService = GeoapifyService(BuildConfig.GEOAPIFY_API_KEY),
            photoManager = photoManager,
        )
    }
}

data class AppContainer(
    val database: RestoDatabase,
    val restaurantRepository: RestaurantRepository,
    val backupRepository: BackupRepository,
    val geoapifyService: GeoapifyService,
    val photoManager: PhotoManager,
)
