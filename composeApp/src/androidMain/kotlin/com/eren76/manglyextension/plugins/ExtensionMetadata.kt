package com.eren76.manglyextension.plugins

import androidx.compose.ui.graphics.painter.BitmapPainter

/**
 * Archive metadata and availability for the UI, also retained for disabled extensions.
 * Executable source instances are held separately in [LoadedSource].
 */
data class ExtensionMetadata(
    val entryClass: String,
    val name: String,
    val version: String,
    val icon: BitmapPainter,
    val dexFile: ByteArray,
    val extensionId: String,

    /**
     * The source implements all required methods and its declared API version,
     * if present, is at least [Source.API_VERSION].
     * Set by the loader together with [ExtensionLoadResult.loadedSource].
     */
    val canSourceBeLoaded: Boolean
)
