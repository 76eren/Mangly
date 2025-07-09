package com.example.manglyextension.plugins

import androidx.compose.ui.graphics.painter.BitmapPainter

data class ExtensionMetadata(
    val entryClass: String,
    val name: String,
    val version: String,
    val icon: BitmapPainter,
    val dexFile: ByteArray,
    val source: Source
)
