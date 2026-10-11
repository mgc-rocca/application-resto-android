package fr.martinrocca.resto.data.local

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleMigrationTest {
    @Test fun stableV1DatabaseIsMigratedWithoutChangingAnyExistingRowOrForeignKey() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val fileName = "migration-v1-google-test.db"
        context.deleteDatabase(fileName)
        val file = context.getDatabasePath(fileName)
        file.parentFile!!.mkdirs()
        // This file is exported by the real stable V1 APK, including its Room identity.
        instrumentation.context.assets.open("resto-stable-v1.db").use { source ->
            file.outputStream().use { source.copyTo(it) }
        }
        val tables = listOf("restaurants", "visits", "tags", "restaurant_tags", "wishlist_entries", "dishes", "photos")
        val before: Map<String, List<List<String?>>>
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            assertEquals(1, db.version)
            before = tables.associateWith { table ->
                db.rawQuery("SELECT * FROM $table ORDER BY 1", null).use { cursor ->
                    buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).map { if (cursor.isNull(it)) null else cursor.getString(it) }) }
                }
            }
        }
        val upgraded = Room.databaseBuilder(context, RestoDatabase::class.java, fileName)
            .addMigrations(RestoDatabase.MIGRATION_1_2).build()
        try {
            val migrated = upgraded.openHelper.writableDatabase // Real Room validation, no destructive fallback.
            tables.forEach { table ->
                val after = migrated.query("SELECT * FROM $table ORDER BY 1").use { cursor ->
                    buildList { while (cursor.moveToNext()) add((0 until (before.getValue(table).firstOrNull()?.size ?: cursor.columnCount)).map { if (cursor.isNull(it)) null else cursor.getString(it) }) }
                }
                assertEquals(table, before.getValue(table), after)
            }
            migrated.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToNext()) }
            assertNull(upgraded.restoDao().findRestaurant("legacy")!!.googlePlaceId)
            assertEquals("LEGACY", upgraded.restoDao().findRestaurant("legacy")!!.coordinateSource)
            migrated.execSQL("UPDATE restaurants SET googlePlaceId='google-id' WHERE id='legacy'")
            assertEquals("google-id", upgraded.restoDao().findRestaurant("legacy")!!.googlePlaceId)
            assertEquals("visit", upgraded.restoDao().getAllVisits().single().id)
        } finally { upgraded.close(); context.deleteDatabase(fileName) }
    }
}
