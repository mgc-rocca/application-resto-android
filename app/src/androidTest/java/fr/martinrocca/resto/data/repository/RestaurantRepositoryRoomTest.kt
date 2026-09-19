package fr.martinrocca.resto.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.martinrocca.resto.data.local.RestoDatabase
import fr.martinrocca.resto.data.photo.PhotoManager
import fr.martinrocca.resto.domain.model.RestaurantDraft
import fr.martinrocca.resto.domain.model.VisitDraft
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestaurantRepositoryRoomTest {
    private lateinit var database: RestoDatabase
    private lateinit var repository: RestaurantRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, RestoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RestaurantRepository(database, PhotoManager(context))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun removingOnlyWishlistStateDoesNotLeaveAnInvisibleRestaurant() = runBlocking {
        val restaurantId = repository.createWishlistRestaurant(restaurantDraft(), note = null)

        repository.removeFromWishlist(restaurantId)

        assertNull(database.restoDao().findRestaurant(restaurantId))
    }

    @Test
    fun deletingLastVisitMovesRestaurantToWishlist() = runBlocking {
        val restaurantId = repository.createVisitedRestaurant(
            draft = restaurantDraft(),
            visit = VisitDraft(
                date = LocalDate.of(2026, 9, 19),
                overallRating = 8,
            ),
        )
        val visitId = database.restoDao().getAllVisits().single().id

        repository.deleteVisit(visitId)

        assertNotNull(database.restoDao().findRestaurant(restaurantId))
        assertNotNull(database.restoDao().findWishlistEntry(restaurantId))
    }

    private fun restaurantDraft() = RestaurantDraft(
        name = "Restaurant test",
        address = "1 rue du Test, Paris",
    )
}
