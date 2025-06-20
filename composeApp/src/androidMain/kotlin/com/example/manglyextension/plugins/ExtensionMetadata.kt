package com.example.manglyextension.plugins

data class ExtensionMetadata(
    val entryClass: String,
    val name: String,
    val version: String,
    val icon: ByteArray,
    val dexFile: ByteArray

)
