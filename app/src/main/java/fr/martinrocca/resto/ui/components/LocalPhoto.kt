package fr.martinrocca.resto.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LocalPhoto(
    relativePath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var bitmap by remember(relativePath) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(relativePath) {
        bitmap = withContext(Dispatchers.IO) {
            val root = context.filesDir.canonicalFile
            val file = File(root, relativePath).canonicalFile
            if (!file.path.startsWith(root.path + File.separator) || !file.isFile) {
                null
            } else {
                decodeSampledBitmap(file, 1_024)
            }
        }
    }
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .size(144.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val loadedBitmap = bitmap
        if (loadedBitmap == null) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Image(
                bitmap = loadedBitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

private fun decodeSampledBitmap(file: File, requestedSize: Int): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / (sampleSize * 2) >= requestedSize &&
        bounds.outHeight / (sampleSize * 2) >= requestedSize
    ) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeFile(
        file.path,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    )
}
