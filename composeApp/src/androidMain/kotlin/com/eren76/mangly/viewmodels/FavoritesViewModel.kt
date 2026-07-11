package com.eren76.mangly.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eren76.mangly.Constants
import com.eren76.mangly.FavoritesManager
import com.eren76.mangly.FileManager
import com.eren76.mangly.composables.shared.image.CoverCache
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.manglyextension.plugins.ExtensionMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

private const val FAVORITES_VIEW_MODEL_TAG = "FavoritesViewModel"

enum class FavoriteCoverLoadState {
    Idle,
    Loading,
    Success,
    Failed
}

@HiltViewModel
class FavoritesViewModel
@Inject constructor(
    private val favoritesManager: FavoritesManager,
    private val fileManager: FileManager
) : ViewModel() {
    val favorites = mutableStateOf<List<FavoritesEntity>>(emptyList())
    val isLoading = mutableStateOf(true)

    private val coverDir = Constants.FAVORITE_COVERS_DIRECTORY
    private val coverLoadStates =
        mutableStateMapOf<UUID, FavoriteCoverLoadState>() // maps favoriteId to cover load state
    private val loadedCoverFiles =
        mutableStateMapOf<UUID, File>() // maps favoriteId to loaded cover file
    private val coverLoadJobs =
        mutableMapOf<UUID, Job>() // maps favoriteId to the job responsible for loading the cover
    private val coverLoadSemaphore = Semaphore(permits = 4)

    init {
        viewModelScope.launch {
            refreshFavorites()
        }
    }

    fun addFavorite(favorite: FavoritesEntity) {
        viewModelScope.launch {
            favoritesManager.addFavoriteToDb(favorite)
            refreshFavorites()
        }
    }

    fun removeFavorite(id: UUID, context: Context) {
        coverLoadJobs.remove(id)?.cancel()
        coverLoadStates.remove(id)
        loadedCoverFiles.remove(id)

        viewModelScope.launch {
            val toDelete = favorites.value.firstOrNull { it.id == id }
            toDelete?.coverImageFilename?.let { filename ->
                fileManager.deleteFileInDir(
                    context = context,
                    relativeDir = coverDir,
                    fileName = filename
                )
            }

            favoritesManager.removeFavoriteFromDb(id)
            refreshFavorites()

        }
    }

    fun getCoverFile(filename: String, context: Context): File? {
        return fileManager.getFileInDir(
            context = context,
            relativeDir = coverDir,
            fileName = filename
        )
    }

    fun saveCoverBytes(filename: String, bytes: ByteArray, context: Context): File {
        return fileManager.saveBytesToStorage(
            context = context,
            relativeDir = coverDir,
            fileName = filename,
            bytes = bytes,
            overwrite = true
        )
    }

    fun getCoverLoadState(favoriteId: UUID): FavoriteCoverLoadState {
        return coverLoadStates[favoriteId] ?: FavoriteCoverLoadState.Idle
    }

    fun getLoadedCoverFile(favoriteId: UUID): File? {
        return loadedCoverFiles[favoriteId]
    }

    fun ensureFavoriteCover(
        favorite: FavoritesEntity,
        extensionMetadata: ExtensionMetadata?,
        context: Context
    ) {
        if (coverLoadJobs[favorite.id]?.isActive == true) return
        if (coverLoadStates[favorite.id] == FavoriteCoverLoadState.Success) return

        if (extensionMetadata == null) {
            coverLoadStates[favorite.id] = FavoriteCoverLoadState.Failed
            return
        }

        val applicationContext = context.applicationContext
        coverLoadStates[favorite.id] = FavoriteCoverLoadState.Loading
        coverLoadJobs[favorite.id] = viewModelScope.launch {
            try {
                val savedFile: File? = coverLoadSemaphore.withPermit {
                    loadAndSaveFavoriteCover(
                        favorite = favorite,
                        extensionMetadata = extensionMetadata,
                        context = applicationContext
                    )
                }
                coverLoadStates[favorite.id] = if (savedFile != null) {
                    loadedCoverFiles[favorite.id] = savedFile
                    FavoriteCoverLoadState.Success
                } else {
                    FavoriteCoverLoadState.Failed
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                coverLoadStates[favorite.id] = FavoriteCoverLoadState.Failed
                Log.e(
                    FAVORITES_VIEW_MODEL_TAG,
                    "Failed to load cover for favorite ${favorite.id}",
                    error
                )
            } finally {
                coverLoadJobs.remove(favorite.id)
            }
        }
    }

    suspend fun updateFavoriteCoverFilename(favoriteId: UUID, filename: String?) {
        val currentFavorite = favorites.value.firstOrNull { favorite ->
            favorite.id == favoriteId
        }
        if (currentFavorite?.coverImageFilename == filename) return

        favoritesManager.updateCoverFilename(id = favoriteId, filename = filename)
        favorites.value = favorites.value.map { favorite ->
            if (favorite.id == favoriteId) {
                favorite.copy(coverImageFilename = filename)
            } else {
                favorite
            }
        }
    }

    private suspend fun loadAndSaveFavoriteCover(
        favorite: FavoritesEntity,
        extensionMetadata: ExtensionMetadata,
        context: Context
    ): File? {
        val coverInfo = withContext(Dispatchers.IO) {
            extensionMetadata.source.getImageForChaptersList(favorite.mangaUrl)
        }?.takeIf { image -> image.imageUrl.isNotBlank() } ?: return null
        val downloaded: CoverCache.DownloadedImage =
            CoverCache.downloadImage(coverInfo.imageUrl, coverInfo.headers)
                ?.takeIf { image -> image.bytes.isNotEmpty() }
                ?: return null

        if (favorites.value.none { currentFavorite -> currentFavorite.id == favorite.id }) {
            return null
        }

        val extension = CoverCache.inferImageExtension(
            contentType = downloaded.contentType,
            finalUrl = downloaded.finalUrl,
            originalUrl = coverInfo.imageUrl
        )
        val filename = "${favorite.id}.$extension"
        return try {
            val savedFile = withContext(Dispatchers.IO) {
                saveCoverBytes(
                    filename = filename,
                    bytes = downloaded.bytes,
                    context = context
                )
            }
            updateFavoriteCoverFilename(favoriteId = favorite.id, filename = filename)
            savedFile
        } catch (error: Exception) {
            withContext(NonCancellable + Dispatchers.IO) {
                fileManager.deleteFileInDir(
                    context = context,
                    relativeDir = coverDir,
                    fileName = filename
                )
            }
            throw error
        }
    }

    private suspend fun refreshFavorites() {
        favorites.value = favoritesManager.getAllFavoritesFromDb()
        isLoading.value = false
    }
}
