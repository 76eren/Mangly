package com.eren76.mangly.composables.shared.read

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.ImageLoader
import coil3.compose.SubcomposeAsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.crossfade
import coil3.toBitmap
import com.eren76.mangly.composables.shared.image.ImageLoadingErrorComposable
import com.eren76.mangly.rememberStoragePermissionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun LongPressImageMenu(
    imageUrl: String,
    networkHeaders: NetworkHeaders,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isSaving by remember { mutableStateOf(false) }
    var saveResult by remember { mutableStateOf<String?>(null) }
    var isImageLoading by remember(imageUrl, networkHeaders) { mutableStateOf(true) }
    var hasImageLoaded by remember(imageUrl, networkHeaders) { mutableStateOf(false) }

    val imageRequest = remember(imageUrl, networkHeaders) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .httpHeaders(networkHeaders)
            .crossfade(true)
            .build()
    }

    val storagePermission = rememberStoragePermissionHandler {
        coroutineScope.launch {
            isSaving = true
            val success = saveImageToGallery(context, imageUrl, networkHeaders)
            isSaving = false
            saveResult = if (success) "Image saved!" else "Failed to save image"
            Toast.makeText(
                context,
                if (success) "Image saved to gallery" else "Failed to save image",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.85f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LongPressImageMenuHeader(onDismiss = onDismiss)

                Spacer(modifier = Modifier.height(8.dp))

                LongPressImagePreview(
                    imageRequest = imageRequest,
                    onLoadingStateChange = { loading -> isImageLoading = loading },
                    onImageLoadedStateChange = { loaded -> hasImageLoaded = loaded }
                )

                Spacer(modifier = Modifier.height(16.dp))

                SaveImageButton(
                    isSaving = isSaving,
                    isEnabled = hasImageLoaded && !isImageLoading,
                    onClick = { storagePermission.launch() }
                )

                SaveResultText(saveResult = saveResult)
            }
        }
    }
}

@Composable
private fun LongPressImageMenuHeader(onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Image Preview",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun LongPressImagePreview(
    imageRequest: ImageRequest,
    onLoadingStateChange: (Boolean) -> Unit,
    onImageLoadedStateChange: (Boolean) -> Unit
) {
    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = "Image preview",
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp)
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Fit,
        loading = {
            onLoadingStateChange(true)
            onImageLoadedStateChange(false)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        success = {
            onLoadingStateChange(false)
            onImageLoadedStateChange(true)
            Image(
                painter = it.painter,
                contentDescription = "Image preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        },
        error = {
            onLoadingStateChange(false)
            onImageLoadedStateChange(false)
            ImageLoadingErrorComposable(errorMessage = "Failed to load preview")
        }
    )
}

@Composable
private fun SaveImageButton(
    isSaving: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = isEnabled && !isSaving
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.padding(start = 8.dp))
            Text("Saving…")
        } else {
            Text("Save Image")
        }
    }
}

@Composable
private fun SaveResultText(saveResult: String?) {
    saveResult?.let { result ->
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = result,
            style = MaterialTheme.typography.bodySmall,
            color = if (result.startsWith("Image saved"))
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.error
        )
    }
}

private suspend fun saveImageToGallery(
    context: Context,
    imageUrl: String,
    networkHeaders: NetworkHeaders
): Boolean = withContext(Dispatchers.IO) {
    try {
        val imageLoader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .httpHeaders(networkHeaders)
            .build()

        val result = imageLoader.execute(request)
        if (result !is SuccessResult) return@withContext false

        val bitmap = result.image.toBitmap()
        val filename = "mangly_${System.currentTimeMillis()}.png"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for Android 10+
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/Mangly"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext false

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, contentValues, null, null)
        } else {
            // Legacy storage for API < 29
            @Suppress("DEPRECATION")
            val picturesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            )
            val manglyDir = File(picturesDir, "Mangly")
            if (!manglyDir.exists()) manglyDir.mkdirs()

            val file = File(manglyDir, filename)
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DATA, file.absolutePath)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            }
            context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
        }

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
