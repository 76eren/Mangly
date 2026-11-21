package com.eren76.manglyextension.plugins

import androidx.compose.ui.graphics.painter.BitmapPainter

data class ExtensionMetadata(
    val entryClass: String,
    val name: String,
    val version: String,
    val icon: BitmapPainter,
    val dexFile: ByteArray,
    val source: Source,

    /*
    This flag will be determined by the following conditions:
    1. The extension does not provide a version, in which case we will check if the extension meets the minimum requirements for loading.
        --> If the extension at least implements the app's Source interface, we can load it. I say at least because an extension for a newer source API version should be compatible for with an older source version but not visa versa
        --> If the extension does not implement the app's Source interface, we cannot load it, to prevent crashes.

    2. The extension does provide a version, in which case we will compare the extension's API level with the source's API level.
        --> Either the extension's API level is equal to or greater than the source's API level, in which case the extension can be loaded
        --> Or the extension's API level is less than the source's API level, in which case the extension cannot be loaded
     */
    val canSourceBeLoaded: Boolean
)
