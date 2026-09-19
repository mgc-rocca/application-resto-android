package fr.martinrocca.resto.data.photo

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ImportedPhoto(
    val relativePath: String,
    val mimeType: String?,
)

class PhotoManager(
    private val context: Context,
) {
    private val photoDirectory: File
        get() = File(context.filesDir, PHOTO_DIRECTORY).apply { mkdirs() }

    suspend fun importPhotos(uriStrings: List<String>): List<ImportedPhoto> = withContext(Dispatchers.IO) {
        val imported = mutableListOf<ImportedPhoto>()
        try {
            uriStrings.distinct().take(MAX_PHOTOS_PER_VISIT).forEach { uriString ->
                val uri = Uri.parse(uriString)
                val mimeType = context.contentResolver.getType(uri)
                require(mimeType == null || mimeType.startsWith("image/")) {
                    "Le fichier sélectionné n’est pas une image."
                }
                val extension = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mimeType)
                    ?.lowercase()
                    ?.takeIf { it.matches("[a-z0-9]{2,5}".toRegex()) }
                    ?: "jpg"
                val fileName = "${UUID.randomUUID()}.$extension"
                val destination = File(photoDirectory, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IOException("Impossible de lire une photo sélectionnée.")

                imported += ImportedPhoto(
                    relativePath = "$PHOTO_DIRECTORY/$fileName",
                    mimeType = mimeType,
                )
            }
            imported
        } catch (error: Throwable) {
            deletePhotos(imported.map(ImportedPhoto::relativePath))
            throw error
        }
    }

    suspend fun deletePhotos(relativePaths: List<String>) = withContext(Dispatchers.IO) {
        relativePaths.forEach { relativePath ->
            resolve(relativePath)?.delete()
        }
    }

    fun resolve(relativePath: String): File? {
        val root = context.filesDir.canonicalFile
        val file = File(root, relativePath).canonicalFile
        return file.takeIf { candidate ->
            candidate.path.startsWith(root.path + File.separator) && candidate.isFile
        }
    }

    fun createDestination(relativePath: String): File {
        val root = context.filesDir.canonicalFile
        val file = File(root, relativePath).canonicalFile
        require(file.path.startsWith(root.path + File.separator)) {
            "Chemin de photo invalide."
        }
        file.parentFile?.mkdirs()
        return file
    }

    companion object {
        const val PHOTO_DIRECTORY = "restaurant_photos"
        const val MAX_PHOTOS_PER_VISIT = 20
    }
}
