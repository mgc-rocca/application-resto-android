package fr.martinrocca.resto.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
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
        val schema = JSONObject(instrumentation.context.assets.open(
            "fr.martinrocca.resto.data.local.RestoDatabase/1.json",
        ).bufferedReader().use { it.readText() }).getJSONObject("database")
        val tables = listOf("restaurants", "visits", "tags", "restaurant_tags", "wishlist_entries", "dishes", "photos")
        val before: Map<String, List<List<String?>>>
        context.openOrCreateDatabase(fileName, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("PRAGMA foreign_keys=ON")
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.getJSONArray("indices")
                for (j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.execSQL("INSERT INTO restaurants VALUES ('legacy','V1 Restaurant','Adresse V1',48.8,2.3,'geo-legacy','TWO_STARS',10,20)")
            db.execSQL("INSERT INTO restaurants VALUES ('wish','Envie V1','Adresse envie',NULL,NULL,NULL,'ABSENT',11,21)")
            db.execSQL("INSERT INTO visits VALUES ('visit','legacy','2026-09-20',8,7,6,9,8,'Commentaire V1',10,20)")
            db.execSQL("INSERT INTO tags VALUES ('tag','Français','francais',10)")
            db.execSQL("INSERT INTO restaurant_tags VALUES ('legacy','tag')")
            db.execSQL("INSERT INTO wishlist_entries VALUES ('wish',10,'À découvrir')")
            db.execSQL("INSERT INTO dishes VALUES ('dish','visit','Plat V1',2450,'EUR',0)")
            db.execSQL("INSERT INTO photos VALUES ('photo','visit','photos/legacy.jpg','image/jpeg',0,10)")
            before = tables.associateWith { table ->
                db.rawQuery("SELECT * FROM $table ORDER BY 1", null).use { cursor ->
                    buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).map { if (cursor.isNull(it)) null else cursor.getString(it) }) }
                }
            }
            db.version = 1
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
