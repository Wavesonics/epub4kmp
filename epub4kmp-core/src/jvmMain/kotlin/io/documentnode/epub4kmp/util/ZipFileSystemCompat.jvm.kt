package io.documentnode.epub4kmp.util

import okio.FileSystem
import okio.Path
import okio.openZip

internal actual fun openEpubZip(fileSystem: FileSystem, zipPath: Path): FileSystem =
    fileSystem.openZip(zipPath)
