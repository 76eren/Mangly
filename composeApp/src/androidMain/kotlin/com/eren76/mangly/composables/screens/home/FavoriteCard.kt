package com.eren76.mangly.composables.screens.home

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.eren76.mangly.composables.shared.image.CoverImageRequests
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.FavoriteCoverLoadState
import com.eren76.mangly.viewmodels.FavoritesViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FavoriteCard(
    favorite: FavoritesEntity,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    favoritesViewModel: FavoritesViewModel,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    HomeMangaCard(
        title = favorite.mangaTitle,
        menuKey = favorite.id,
        menuText = "Delete favorite",
        onClick = onClick,
        onDelete = {
            favoritesViewModel.removeFavorite(favorite.id, context)
            Toast.makeText(context, "Favorite deleted", Toast.LENGTH_SHORT).show()
        },
        imageContent = { modifier ->
            FavoriteImage(
                favorite = favorite,
                extensionMetadataViewModel = extensionMetadataViewModel,
                favoritesViewModel = favoritesViewModel,
                modifier = modifier
            )
        }
    )
}

@Composable
fun FavoriteImage(
    favorite: FavoritesEntity,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    favoritesViewModel: FavoritesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coverLoadState: FavoriteCoverLoadState = favoritesViewModel.getCoverLoadState(favorite.id)
    val loadedCoverFile = favoritesViewModel.getLoadedCoverFile(favorite.id)

    var storedCoverFile: File? by remember(favorite.id, favorite.coverImageFilename) {
        mutableStateOf<File?>(null)
    }
    var hasResolvedLocalCover: Boolean by remember(favorite.id, favorite.coverImageFilename) {
        mutableStateOf(false)
    }

    LaunchedEffect(favorite.id, favorite.coverImageFilename, context) {
        try {
            storedCoverFile = favorite.coverImageFilename?.let { filename ->
                withContext(Dispatchers.IO) {
                    favoritesViewModel.getCoverFile(filename = filename, context = context)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            storedCoverFile = null
        } finally {
            hasResolvedLocalCover = true
        }
    }

    val localCoverFile = loadedCoverFile ?: storedCoverFile

    LaunchedEffect(
        favorite.id,
        favorite.mangaUrl,
        favorite.coverImageFilename,
        hasResolvedLocalCover,
        localCoverFile
    ) {
        if (!hasResolvedLocalCover) return@LaunchedEffect
        if (localCoverFile != null) return@LaunchedEffect

        favoritesViewModel.ensureFavoriteCover(
            favorite = favorite,
            extensionMetadata = findMetadataForFavorite(extensionMetadataViewModel, favorite),
            context = context
        )
    }

    val localRequest = remember(localCoverFile, context) {
        CoverImageRequests.local(context = context, file = localCoverFile)
    }

    HomeCoverImage(
        model = localRequest,
        contentDescription = favorite.mangaTitle,
        isLoading = !hasResolvedLocalCover ||
                coverLoadState == FavoriteCoverLoadState.Idle ||
                coverLoadState == FavoriteCoverLoadState.Loading,
        modifier = modifier
    )
}
