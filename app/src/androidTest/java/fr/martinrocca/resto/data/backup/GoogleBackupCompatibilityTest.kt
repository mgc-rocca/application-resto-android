package fr.martinrocca.resto.data.backup

import android.net.Uri
import android.content.Context
import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.martinrocca.resto.data.local.RestoDatabase
import fr.martinrocca.resto.data.photo.PhotoManager
import fr.martinrocca.resto.data.repository.RestaurantRepository
import fr.martinrocca.resto.domain.model.RestaurantDraft
import java.io.File
import java.util.zip.ZipFile
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleBackupCompatibilityTest {
    @Test fun restoreV1ThenRoundTripV11PreservesIdsRelationsPhotosAndPersonalData() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = IsolatedBackupContext(instrumentation.targetContext)
        val database = Room.inMemoryDatabaseBuilder(context, RestoDatabase::class.java).build()
        val photos = PhotoManager(context)
        val backups = BackupRepository(context, database, photos)
        val original = File(context.cacheDir, "resto-v1-import.zip")
        val output = File(context.cacheDir, "resto-v11-export.zip")
        val fixture = instrumentation.context.assets.open("resto-stable-v1.zip").use { it.readBytes() }
        original.writeBytes(fixture)
        try {
            backups.restoreFrom(Uri.fromFile(original))
            val dao = database.restoDao()
            val initialRestaurant = dao.findRestaurant("legacy")!!
            assertEquals("geo-legacy", initialRestaurant.geoapifyPlaceId)
            assertNull(initialRestaurant.googlePlaceId)
            assertEquals("LEGACY", initialRestaurant.coordinateSource)
            dao.updateRestaurant(initialRestaurant.copy(googlePlaceId = "google-place-test"))
            val beforeRestaurants = dao.getAllRestaurants()
            val beforeVisits = dao.getAllVisits()
            val beforeTags = dao.getAllTags()
            val beforeLinks = dao.getAllRestaurantTags()
            val beforeWishes = dao.getAllWishlistEntries()
            val beforeDishes = dao.getAllDishes()
            val beforePhoto = dao.getAllPhotos().single()
            val photoBytes = photos.resolve(beforePhoto.relativePath)!!.readBytes()
            assertEquals(8, beforeVisits.single().overallRating)
            assertEquals(7, beforeVisits.single().foodRating)
            assertEquals("Commentaire V1", beforeVisits.single().comment)
            assertEquals(2450L, beforeDishes.single().priceCents)
            backups.exportTo(Uri.fromFile(output))
            ZipFile(output).use { zip ->
                val root = JSONObject(zip.getInputStream(zip.getEntry("backup.json")).bufferedReader().use { it.readText() })
                assertEquals(2, root.getInt("version"))
                assertFalse(root.has("googleCoordinates"))
                val records = root.getJSONArray("restaurants")
                val fields = records.getJSONObject(0).keys().asSequence().toSet()
                assertEquals(setOf("id", "name", "address", "latitude", "longitude", "geoapifyPlaceId", "michelinStatus", "createdAt", "updatedAt", "googlePlaceId", "coordinateSource"), fields)
            }
            val repository = RestaurantRepository(database, photos)
            repository.createWishlistRestaurant(RestaurantDraft("Temporaire", "Adresse temporaire"), null)
            backups.restoreFrom(Uri.fromFile(output))
            assertEquals(beforeRestaurants, dao.getAllRestaurants())
            assertEquals(beforeVisits, dao.getAllVisits())
            assertEquals(beforeTags, dao.getAllTags())
            assertEquals(beforeLinks, dao.getAllRestaurantTags())
            assertEquals(beforeWishes, dao.getAllWishlistEntries())
            assertEquals(beforeDishes, dao.getAllDishes())
            val restored = dao.getAllPhotos().single()
            assertEquals(beforePhoto.id, restored.id)
            assertEquals(beforePhoto.visitId, restored.visitId)
            assertArrayEquals(photoBytes, photos.resolve(restored.relativePath)!!.readBytes())
            // Restoring the same archive again replaces the snapshot rather than duplicating rows.
            backups.restoreFrom(Uri.fromFile(output))
            assertEquals(beforeRestaurants.size, dao.getAllRestaurants().size)
            assertEquals(beforeVisits.size, dao.getAllVisits().size)
            assertEquals(1, dao.getAllPhotos().size)
        } finally {
            photos.deletePhotos(database.restoDao().getAllPhotos().map { it.relativePath })
            database.close()
            original.delete(); output.delete()
            context.cleanUp()
        }
    }

    @Test fun googleAssociationDoesNotOverwriteExistingFieldsAndDuplicateAssociationIsRejected() = runBlocking {
        val context = IsolatedBackupContext(InstrumentationRegistry.getInstrumentation().targetContext)
        val db = Room.inMemoryDatabaseBuilder(context, RestoDatabase::class.java).build()
        val repository = RestaurantRepository(db, PhotoManager(context))
        try {
            val original = RestaurantDraft("Nom personnel", "Adresse personnelle", 48.8, 2.3, tags = listOf("Français"))
            val id = repository.createWishlistRestaurant(original, "Envie personnelle")
            repository.updateRestaurant(id, original.copy(googlePlaceId = "same-google-place"), "Envie personnelle")
            val stored = db.restoDao().findRestaurant(id)!!
            assertEquals(original.name, stored.name)
            assertEquals(original.address, stored.address)
            assertEquals(original.latitude, stored.latitude)
            assertEquals("same-google-place", stored.googlePlaceId)
            val duplicate = runCatching { repository.createWishlistRestaurant(original.copy(googlePlaceId = "same-google-place"), null) }
            assertTrue(duplicate.isFailure)
            assertEquals(1, db.restoDao().getAllRestaurants().size)
            repository.createWishlistRestaurant(original.copy(name = "Manuel 1"), null)
            repository.createWishlistRestaurant(original.copy(name = "Manuel 2"), null)
            assertEquals(3, db.restoDao().getAllRestaurants().size) // Multiple NULL IDs remain allowed.
        } finally { db.close(); context.cleanUp() }
    }
}

/** Tests never read, replace or clean the user's actual photo directory. */
private class IsolatedBackupContext(base: Context) : ContextWrapper(base) {
    private val root = File(base.cacheDir, "backup-test-${UUID.randomUUID()}").apply { mkdirs() }
    override fun getFilesDir(): File = File(root, "files").apply { mkdirs() }
    override fun getCacheDir(): File = File(root, "cache").apply { mkdirs() }
    fun cleanUp() { root.deleteRecursively() }
}
