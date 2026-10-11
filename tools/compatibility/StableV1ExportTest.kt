package fr.martinrocca.resto.compatibility

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import fr.martinrocca.resto.data.backup.BackupRepository
import fr.martinrocca.resto.data.local.RestoDatabase
import fr.martinrocca.resto.data.photo.PhotoManager
import java.io.File
import java.util.UUID
import java.util.zip.ZipFile
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Copied by CI into commit 3413bcc, then compiled and run ONLY with the stable V1. */
@RunWith(AndroidJUnit4::class)
class StableV1ExportTest {
    @Test fun exportZipAndCheckpointedDatabaseUsingTheRealStableV1Code() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val base = instrumentation.targetContext
        val isolated = ExportContext(base)
        val name = "resto-stable-v1-export-test.db"
        base.deleteDatabase(name)
        val database = Room.databaseBuilder(base, RestoDatabase::class.java, name).build()
        val input = File(isolated.cacheDir, "input-v1.zip")
        val zip = File(base.cacheDir, "resto-stable-v1.zip")
        val sqlite = File(base.cacheDir, "resto-stable-v1.db")
        var closed = false
        try {
            instrumentation.context.assets.open("resto-v1-fixture.zip").use { source ->
                input.outputStream().use { source.copyTo(it) }
            }
            val backups = BackupRepository(isolated, database, PhotoManager(isolated))
            backups.restoreFrom(Uri.fromFile(input))
            backups.exportTo(Uri.fromFile(zip)) // V1 serializer, not a handwritten JSON archive.
            ZipFile(zip).use { archive ->
                val manifest = archive.getInputStream(archive.getEntry("backup.json"))
                    .bufferedReader().use { JSONObject(it.readText()) }
                assertEquals(1, manifest.getInt("version"))
                assertEquals(2, manifest.getJSONArray("restaurants").length())
                assertEquals(1, manifest.getJSONArray("photos").length())
            }
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }
            database.close()
            closed = true
            base.getDatabasePath(name).copyTo(sqlite, overwrite = true)
        } finally {
            if (!closed) database.close()
            base.deleteDatabase(name) // Never the application's resto.db.
            isolated.cleanUp()
        }
    }
}

private class ExportContext(base: Context) : ContextWrapper(base) {
    private val root = File(base.cacheDir, "stable-export-${UUID.randomUUID()}").apply { mkdirs() }
    override fun getFilesDir(): File = File(root, "files").apply { mkdirs() }
    override fun getCacheDir(): File = File(root, "cache").apply { mkdirs() }
    fun cleanUp() { root.deleteRecursively() }
}
