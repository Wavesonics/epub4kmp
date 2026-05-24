package io.documentnode.epub4kmp.domain

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.openZip

/**
 * Lazily reads resources from an EPUB on disk.
 *
 * Each call to [getResourceBytes] opens the ZIP via [FileSystem.openZip] and
 * reads a single entry. okio's ZIP filesystem caches the central directory.
 */
class EpubResourceProvider(
    private val fileSystem: FileSystem,
    private val zipPath: Path
) : LazyResourceProvider {
    override fun getResourceBytes(href: String): ByteArray {
        val zipFs = fileSystem.openZip(zipPath)
        val entryPath = "/$href".toPath()
        return zipFs.read(entryPath) { readByteArray() }
    }
}
