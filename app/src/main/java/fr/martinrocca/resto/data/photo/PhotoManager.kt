package fr.martinrocca.resto.data.photo

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class ImportedPhoto(
    val relativePath: String,
    val mimeType: String?,
)

class PhotoManager(
    private val context: Context,
) {
    private val photoDirectory: File = File(context.filesDir, PHOTO_DIRECTORY).apply {
        mkdirs()
        listFiles { file -> file.isFile && file.name.endsWith(".tmp") }
            ?.forEach(File::delete)
    }

    suspend fun importPhotos(uriStrings: List<String>): List<ImportedPhoto> = withContext(Dispatchers.IO) {
        val selectedUris = uriStrings.distinct()
        require(selectedUris.size <= MAX_PHOTOS_PER_VISIT) {
            "Une visite ne peut pas contenir plus de $MAX_PHOTOS_PER_VISIT photos."
        }
        val imported = mutableListOf<ImportedPhoto>()
        try {
            selectedUris.forEach { uriString ->
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
                val temporary = File.createTempFile("photo-import-", ".tmp", photoDirectory)
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        temporary.outputStream().use { output ->
                            copyWithLimit(input, output, MAX_PHOTO_BYTES)
                        }
                    } ?: throw IOException("Impossible de lire une photo sélectionnée.")

                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(temporary.path, bounds)
                    require(bounds.outWidth > 0 && bounds.outHeight > 0) {
                        "Le fichier sélectionné n’est pas une image lisible."
                    }
                    check(temporary.renameTo(destination)) {
                        "Impossible de finaliser la copie de la photo."
                    }
                } finally {
                    temporary.delete()
                }

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
        const val MAX_PHOTOS_PER_VISIT = 5
        const val MAX_PHOTO_BYTES = 50L * 1024L * 1024L
    }
}

private suspend fun copyWithLimit(
    input: InputStream,
    output: OutputStream,
    byteLimit: Long,
) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var copied = 0L
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = input.read(buffer)
        if (count < 0) break
        copied += count
        require(copied <= byteLimit) {
            "Chaque photo doit peser moins de ${byteLimit / 1024L / 1024L} Mo."
        }
        output.write(buffer, 0, count)
    }
}
