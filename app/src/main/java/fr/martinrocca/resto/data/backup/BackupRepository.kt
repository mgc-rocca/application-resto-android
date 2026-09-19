package fr.martinrocca.resto.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import fr.martinrocca.resto.data.local.RestoDatabase
import fr.martinrocca.resto.data.local.entity.DishEntity
import fr.martinrocca.resto.data.local.entity.PhotoEntity
import fr.martinrocca.resto.data.local.entity.RestaurantEntity
import fr.martinrocca.resto.data.local.entity.RestaurantTagEntity
import fr.martinrocca.resto.data.local.entity.TagEntity
import fr.martinrocca.resto.data.local.entity.VisitEntity
import fr.martinrocca.resto.data.local.entity.WishlistEntryEntity
import fr.martinrocca.resto.data.photo.PhotoManager
import fr.martinrocca.resto.domain.model.MichelinStatus
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupRepository(
    private val context: Context,
    private val database: RestoDatabase,
    private val photoManager: PhotoManager,
) {
    private val dao = database.restoDao()

    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val snapshot = readCurrentSnapshot()
        val archivedPhotos = snapshot.photos.associateWith { photo ->
            val file = photoManager.resolve(photo.relativePath)
                ?: throw IOException("Une photo du journal est introuvable. L’export a été annulé.")
            ArchivedPhoto(
                entity = photo,
                archivePath = "photos/${photo.id}.${safeExtension(file)}",
                source = file,
            )
        }
        val json = snapshot.toJson(
            archivedPhotos.mapValues { (_, archived) -> archived.archivePath },
        )
        val output = context.contentResolver.openOutputStream(uri, "w")
            ?: throw IOException("Impossible d’ouvrir le fichier de sauvegarde.")

        output.buffered().use { stream ->
            ZipOutputStream(stream).use { zip ->
                zip.putNextEntry(ZipEntry(MANIFEST_NAME))
                zip.write(json.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                archivedPhotos.values.forEach { archived ->
                    zip.putNextEntry(ZipEntry(archived.archivePath))
                    archived.source.inputStream().buffered().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    suspend fun restoreFrom(uri: Uri) = withContext(Dispatchers.IO) {
        val temporaryDirectory = File(
            context.cacheDir,
            "restore-${UUID.randomUUID()}",
        ).apply { mkdirs() }
        val createdPhotoPaths = mutableListOf<String>()
        try {
            val extracted = extractArchive(uri, temporaryDirectory)
            val manifestBytes = extracted.manifest
                ?: throw IllegalArgumentException("La sauvegarde ne contient pas $MANIFEST_NAME.")
            val parsed = parseSnapshot(String(manifestBytes, Charsets.UTF_8))
            validateSnapshot(parsed)

            val expectedPhotoEntries = parsed.photos.map(BackupPhoto::archivePath).toSet()
            require(extracted.photos.keys == expectedPhotoEntries) {
                "Le contenu photo de la sauvegarde est incomplet ou incohérent."
            }

            val restoredPhotos = parsed.photos.map { archivedPhoto ->
                val source = requireNotNull(extracted.photos[archivedPhoto.archivePath])
                val extension = safeExtension(source)
                val relativePath = "${PhotoManager.PHOTO_DIRECTORY}/${UUID.randomUUID()}.$extension"
                val destination = photoManager.createDestination(relativePath)
                source.inputStream().buffered().use { input ->
                    destination.outputStream().buffered().use { output -> input.copyTo(output) }
                }
                createdPhotoPaths += relativePath
                archivedPhoto.entity.copy(relativePath = relativePath)
            }

            val oldPhotoPaths = dao.getAllPhotos().map(PhotoEntity::relativePath)
            try {
                database.withTransaction {
                    dao.clearRestaurants()
                    dao.clearTags()
                    if (parsed.restaurants.isNotEmpty()) dao.insertRestaurants(parsed.restaurants)
                    if (parsed.tags.isNotEmpty()) dao.insertTags(parsed.tags)
                    if (parsed.visits.isNotEmpty()) dao.insertVisits(parsed.visits)
                    if (parsed.restaurantTags.isNotEmpty()) {
                        dao.insertRestaurantTags(parsed.restaurantTags)
                    }
                    if (parsed.wishlistEntries.isNotEmpty()) {
                        dao.insertWishlistEntries(parsed.wishlistEntries)
                    }
                    if (parsed.dishes.isNotEmpty()) dao.insertDishes(parsed.dishes)
                    if (restoredPhotos.isNotEmpty()) dao.insertPhotos(restoredPhotos)
                }
            } catch (error: Throwable) {
                photoManager.deletePhotos(createdPhotoPaths)
                throw error
            }
            photoManager.deletePhotos(oldPhotoPaths)
        } finally {
            temporaryDirectory.deleteRecursively()
        }
    }

    private suspend fun readCurrentSnapshot(): BackupSnapshot = database.withTransaction {
        BackupSnapshot(
            restaurants = dao.getAllRestaurants(),
            visits = dao.getAllVisits(),
            tags = dao.getAllTags(),
            restaurantTags = dao.getAllRestaurantTags(),
            wishlistEntries = dao.getAllWishlistEntries(),
            dishes = dao.getAllDishes(),
            photos = dao.getAllPhotos(),
        )
    }

    private fun extractArchive(uri: Uri, directory: File): ExtractedArchive {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Impossible de lire le fichier de sauvegarde.")
        var manifest: ByteArray? = null
        val photos = mutableMapOf<String, File>()
        val seenEntries = mutableSetOf<String>()
        var entryCount = 0
        var totalBytes = 0L

        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                require(entryCount <= MAX_ARCHIVE_ENTRIES) { "La sauvegarde contient trop de fichiers." }
                val name = validateEntryName(entry.name)
                require(seenEntries.add(name)) { "La sauvegarde contient une entrée dupliquée." }
                if (entry.isDirectory) {
                    require(name == "photos/") { "Dossier inattendu dans la sauvegarde." }
                    zip.closeEntry()
                    continue
                }

                when {
                    name == MANIFEST_NAME -> {
                        val result = copyEntryToMemory(zip, MAX_MANIFEST_BYTES, totalBytes)
                        manifest = result.bytes
                        totalBytes = result.totalBytes
                    }
                    name.startsWith("photos/") && name.count { it == '/' } == 1 -> {
                        val destination = File(directory, UUID.randomUUID().toString())
                        val bytesRead = destination.outputStream().buffered().use { output ->
                            copyEntry(zip, output::write, MAX_PHOTO_BYTES, totalBytes)
                        }
                        totalBytes += bytesRead
                        photos[name] = destination
                    }
                    else -> throw IllegalArgumentException("Fichier inattendu dans la sauvegarde : $name")
                }
                require(totalBytes <= MAX_TOTAL_BYTES) { "La sauvegarde est trop volumineuse." }
                zip.closeEntry()
            }
        }
        return ExtractedArchive(manifest = manifest, photos = photos)
    }

    private fun copyEntryToMemory(
        zip: ZipInputStream,
        limit: Long,
        alreadyRead: Long,
    ): MemoryCopyResult {
        val output = ByteArrayOutputStream()
        val read = copyEntry(zip, output::write, limit, alreadyRead)
        return MemoryCopyResult(output.toByteArray(), alreadyRead + read)
    }

    private fun copyEntry(
        zip: ZipInputStream,
        write: (ByteArray, Int, Int) -> Unit,
        entryLimit: Long,
        alreadyRead: Long,
    ): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var entryBytes = 0L
        while (true) {
            val count = zip.read(buffer)
            if (count < 0) break
            entryBytes += count
            require(entryBytes <= entryLimit) { "Une entrée de la sauvegarde est trop volumineuse." }
            require(alreadyRead + entryBytes <= MAX_TOTAL_BYTES) { "La sauvegarde est trop volumineuse." }
            write(buffer, 0, count)
        }
        return entryBytes
    }

    private fun validateEntryName(rawName: String): String {
        require(rawName.isNotBlank() && !rawName.startsWith('/') && '\\' !in rawName) {
            "Chemin invalide dans la sauvegarde."
        }
        val parts = rawName.removeSuffix("/").split('/')
        require(parts.none { it.isBlank() || it == "." || it == ".." }) {
            "Chemin invalide dans la sauvegarde."
        }
        return rawName
    }

    private fun parseSnapshot(json: String): ParsedSnapshot {
        val root = JSONObject(json)
        require(root.getString("format") == FORMAT_NAME) { "Format de sauvegarde inconnu." }
        require(root.getInt("version") == FORMAT_VERSION) {
            "Cette version de sauvegarde n’est pas prise en charge."
        }
        return ParsedSnapshot(
            restaurants = root.objects("restaurants") { item ->
                RestaurantEntity(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    address = item.getString("address"),
                    latitude = item.nullableDouble("latitude"),
                    longitude = item.nullableDouble("longitude"),
                    geoapifyPlaceId = item.nullableString("geoapifyPlaceId"),
                    michelinStatus = MichelinStatus.valueOf(item.getString("michelinStatus")),
                    createdAt = item.getLong("createdAt"),
                    updatedAt = item.getLong("updatedAt"),
                )
            },
            visits = root.objects("visits") { item ->
                VisitEntity(
                    id = item.getString("id"),
                    restaurantId = item.getString("restaurantId"),
                    date = item.getString("date"),
                    overallRating = item.getInt("overallRating"),
                    foodRating = item.nullableInt("foodRating"),
                    serviceRating = item.nullableInt("serviceRating"),
                    settingRating = item.nullableInt("settingRating"),
                    priceRating = item.nullableInt("priceRating"),
                    comment = item.nullableString("comment"),
                    createdAt = item.getLong("createdAt"),
                    updatedAt = item.getLong("updatedAt"),
                )
            },
            tags = root.objects("tags") { item ->
                TagEntity(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    normalizedName = item.getString("normalizedName"),
                    createdAt = item.getLong("createdAt"),
                )
            },
            restaurantTags = root.objects("restaurantTags") { item ->
                RestaurantTagEntity(
                    restaurantId = item.getString("restaurantId"),
                    tagId = item.getString("tagId"),
                )
            },
            wishlistEntries = root.objects("wishlistEntries") { item ->
                WishlistEntryEntity(
                    restaurantId = item.getString("restaurantId"),
                    addedAt = item.getLong("addedAt"),
                    note = item.nullableString("note"),
                )
            },
            dishes = root.objects("dishes") { item ->
                DishEntity(
                    id = item.getString("id"),
                    visitId = item.getString("visitId"),
                    name = item.getString("name"),
                    priceCents = item.nullableLong("priceCents"),
                    currency = item.getString("currency"),
                    sortOrder = item.getInt("sortOrder"),
                )
            },
            photos = root.objects("photos") { item ->
                BackupPhoto(
                    entity = PhotoEntity(
                        id = item.getString("id"),
                        visitId = item.getString("visitId"),
                        relativePath = "",
                        mimeType = item.nullableString("mimeType"),
                        sortOrder = item.getInt("sortOrder"),
                        createdAt = item.getLong("createdAt"),
                    ),
                    archivePath = item.getString("archivePath"),
                )
            },
        )
    }

    private fun validateSnapshot(snapshot: ParsedSnapshot) {
        val recordCount = snapshot.restaurants.size + snapshot.visits.size + snapshot.tags.size +
            snapshot.restaurantTags.size + snapshot.wishlistEntries.size + snapshot.dishes.size +
            snapshot.photos.size
        require(recordCount <= MAX_RECORDS) { "La sauvegarde contient trop de données." }

        val restaurantIds = snapshot.restaurants.uniqueIds(RestaurantEntity::id, "restaurants")
        val visitIds = snapshot.visits.uniqueIds(VisitEntity::id, "visites")
        val tagIds = snapshot.tags.uniqueIds(TagEntity::id, "tags")
        snapshot.dishes.uniqueIds(DishEntity::id, "plats")
        snapshot.photos.map(BackupPhoto::entity).uniqueIds(PhotoEntity::id, "photos")

        snapshot.restaurants.forEach { restaurant ->
            require(restaurant.name.isNotBlank() && restaurant.address.isNotBlank()) {
                "Un restaurant contient des champs obligatoires vides."
            }
            require((restaurant.latitude == null) == (restaurant.longitude == null)) {
                "Des coordonnées sont incomplètes."
            }
            restaurant.latitude?.let { require(it.isFinite() && it in -90.0..90.0) }
            restaurant.longitude?.let { require(it.isFinite() && it in -180.0..180.0) }
        }
        require(
            snapshot.restaurants.mapNotNull(RestaurantEntity::geoapifyPlaceId).distinct().size ==
                snapshot.restaurants.count { it.geoapifyPlaceId != null },
        ) { "Plusieurs restaurants possèdent le même identifiant d’adresse." }

        snapshot.visits.forEach { visit ->
            require(visit.restaurantId in restaurantIds) { "Une visite référence un restaurant absent." }
            LocalDate.parse(visit.date)
            require(visit.overallRating in 1..10) { "Une note globale est invalide." }
            listOf(visit.foodRating, visit.serviceRating, visit.settingRating, visit.priceRating)
                .filterNotNull()
                .forEach { require(it in 1..10) { "Une sous-note est invalide." } }
        }
        require(snapshot.tags.map(TagEntity::normalizedName).distinct().size == snapshot.tags.size) {
            "Plusieurs tags sont identiques."
        }
        snapshot.restaurantTags.forEach { link ->
            require(link.restaurantId in restaurantIds && link.tagId in tagIds) {
                "Une association de tag est invalide."
            }
        }
        require(snapshot.restaurantTags.distinct().size == snapshot.restaurantTags.size) {
            "Une association de tag est dupliquée."
        }
        snapshot.wishlistEntries.forEach { entry ->
            require(entry.restaurantId in restaurantIds) { "Une envie référence un restaurant absent." }
        }
        snapshot.dishes.forEach { dish ->
            require(dish.visitId in visitIds && dish.name.isNotBlank()) { "Un plat est invalide." }
            require(dish.priceCents == null || dish.priceCents >= 0) { "Un prix est invalide." }
        }
        snapshot.photos.forEach { photo ->
            require(photo.entity.visitId in visitIds) { "Une photo référence une visite absente." }
            validateEntryName(photo.archivePath)
            require(photo.archivePath.startsWith("photos/") && photo.archivePath.count { it == '/' } == 1) {
                "Le chemin d’une photo est invalide."
            }
        }
        require(snapshot.photos.map(BackupPhoto::archivePath).distinct().size == snapshot.photos.size) {
            "Plusieurs photos utilisent le même chemin."
        }
    }

    private fun safeExtension(file: File): String = file.extension
        .lowercase()
        .takeIf { it.matches(EXTENSION_REGEX) }
        ?: "jpg"

    private data class ArchivedPhoto(
        val entity: PhotoEntity,
        val archivePath: String,
        val source: File,
    )

    private data class ExtractedArchive(
        val manifest: ByteArray?,
        val photos: Map<String, File>,
    )

    private data class MemoryCopyResult(
        val bytes: ByteArray,
        val totalBytes: Long,
    )

    companion object {
        private const val FORMAT_NAME = "fr.martinrocca.resto"
        private const val FORMAT_VERSION = 1
        private const val MANIFEST_NAME = "backup.json"
        private const val MAX_ARCHIVE_ENTRIES = 10_000
        private const val MAX_RECORDS = 100_000
        private const val MAX_MANIFEST_BYTES = 20L * 1024 * 1024
        private const val MAX_PHOTO_BYTES = 100L * 1024 * 1024
        private const val MAX_TOTAL_BYTES = 1024L * 1024 * 1024
        private val EXTENSION_REGEX = "[a-z0-9]{2,5}".toRegex()
    }
}

private data class BackupSnapshot(
    val restaurants: List<RestaurantEntity>,
    val visits: List<VisitEntity>,
    val tags: List<TagEntity>,
    val restaurantTags: List<RestaurantTagEntity>,
    val wishlistEntries: List<WishlistEntryEntity>,
    val dishes: List<DishEntity>,
    val photos: List<PhotoEntity>,
)

private data class ParsedSnapshot(
    val restaurants: List<RestaurantEntity>,
    val visits: List<VisitEntity>,
    val tags: List<TagEntity>,
    val restaurantTags: List<RestaurantTagEntity>,
    val wishlistEntries: List<WishlistEntryEntity>,
    val dishes: List<DishEntity>,
    val photos: List<BackupPhoto>,
)

private data class BackupPhoto(
    val entity: PhotoEntity,
    val archivePath: String,
)

private fun BackupSnapshot.toJson(photoPaths: Map<PhotoEntity, String>): JSONObject = JSONObject().apply {
    put("format", "fr.martinrocca.resto")
    put("version", 1)
    put("exportedAt", Instant.now().toString())
    put("restaurants", restaurants.toJsonArray { restaurant ->
        JSONObject().apply {
            put("id", restaurant.id)
            put("name", restaurant.name)
            put("address", restaurant.address)
            putNullable("latitude", restaurant.latitude)
            putNullable("longitude", restaurant.longitude)
            putNullable("geoapifyPlaceId", restaurant.geoapifyPlaceId)
            put("michelinStatus", restaurant.michelinStatus.name)
            put("createdAt", restaurant.createdAt)
            put("updatedAt", restaurant.updatedAt)
        }
    })
    put("visits", visits.toJsonArray { visit ->
        JSONObject().apply {
            put("id", visit.id)
            put("restaurantId", visit.restaurantId)
            put("date", visit.date)
            put("overallRating", visit.overallRating)
            putNullable("foodRating", visit.foodRating)
            putNullable("serviceRating", visit.serviceRating)
            putNullable("settingRating", visit.settingRating)
            putNullable("priceRating", visit.priceRating)
            putNullable("comment", visit.comment)
            put("createdAt", visit.createdAt)
            put("updatedAt", visit.updatedAt)
        }
    })
    put("tags", tags.toJsonArray { tag ->
        JSONObject().apply {
            put("id", tag.id)
            put("name", tag.name)
            put("normalizedName", tag.normalizedName)
            put("createdAt", tag.createdAt)
        }
    })
    put("restaurantTags", restaurantTags.toJsonArray { link ->
        JSONObject().apply {
            put("restaurantId", link.restaurantId)
            put("tagId", link.tagId)
        }
    })
    put("wishlistEntries", wishlistEntries.toJsonArray { entry ->
        JSONObject().apply {
            put("restaurantId", entry.restaurantId)
            put("addedAt", entry.addedAt)
            putNullable("note", entry.note)
        }
    })
    put("dishes", dishes.toJsonArray { dish ->
        JSONObject().apply {
            put("id", dish.id)
            put("visitId", dish.visitId)
            put("name", dish.name)
            putNullable("priceCents", dish.priceCents)
            put("currency", dish.currency)
            put("sortOrder", dish.sortOrder)
        }
    })
    put("photos", photos.toJsonArray { photo ->
        JSONObject().apply {
            put("id", photo.id)
            put("visitId", photo.visitId)
            put("archivePath", requireNotNull(photoPaths[photo]))
            putNullable("mimeType", photo.mimeType)
            put("sortOrder", photo.sortOrder)
            put("createdAt", photo.createdAt)
        }
    })
}

private fun JSONObject.putNullable(name: String, value: Any?) {
    put(name, value ?: JSONObject.NULL)
}

private fun JSONObject.nullableString(name: String): String? =
    if (isNull(name)) null else getString(name)

private fun JSONObject.nullableInt(name: String): Int? =
    if (isNull(name)) null else getInt(name)

private fun JSONObject.nullableLong(name: String): Long? =
    if (isNull(name)) null else getLong(name)

private fun JSONObject.nullableDouble(name: String): Double? =
    if (isNull(name)) null else getDouble(name)

private inline fun <T> JSONObject.objects(name: String, transform: (JSONObject) -> T): List<T> {
    val array = getJSONArray(name)
    return List(array.length()) { index -> transform(array.getJSONObject(index)) }
}

private inline fun <T> Iterable<T>.toJsonArray(transform: (T) -> JSONObject): JSONArray =
    JSONArray().also { array -> forEach { array.put(transform(it)) } }

private fun <T> List<T>.uniqueIds(
    selector: (T) -> String,
    label: String,
): Set<String> {
    val ids = map(selector)
    require(ids.all(String::isNotBlank) && ids.distinct().size == ids.size) {
        "Les identifiants de $label sont invalides."
    }
    return ids.toSet()
}
