package io.documentnode.epub4kmp.util

import okio.FileSystem
import okio.Path

internal actual fun openEpubZip(fileSystem: FileSystem, zipPath: Path): FileSystem =
    throw UnsupportedOperationException(
        "Loading an EPUB from a file path is not supported on wasmJs. " +
            "Read from an okio Source via EpubReader.readEpub(source) instead."
    )
