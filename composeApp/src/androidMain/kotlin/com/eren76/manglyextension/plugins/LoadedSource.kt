package com.eren76.manglyextension.plugins

/**
 * Executable source handle created by ExtensionManager after compatibility validation.
 * Source operations accept this type so disabled extensions cannot be passed to them.
 */
class LoadedSource internal constructor(
    val metadata: ExtensionMetadata,
    val source: Source
)

/**
 * Result of loading an extension, only gets used to determine whether we are dealing with a valid source or not.
 * As a result it gets placed in the ExtensionMetadataViewModel in the right "box"
 */
data class ExtensionLoadResult(
    val metadata: ExtensionMetadata,
    val loadedSource: LoadedSource?
)
