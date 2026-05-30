package io.documentnode.epub4kmp.domain

import io.documentnode.epub4kmp.util.openEpubZip
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Lazily reads resources from an EPUB on disk.
 *
 * The ZIP is opened once (parsing the central directory) and the resulting
 * read-only filesystem is reused across every [getResourceBytes] call, so a
 * book with many lazily loaded resources doesn't re-parse the archive per fetch.
 */
class EpubResourceProvider(
    private val fileSystem: FileSystem,
    private val zipPath: Path
) : LazyResourceProvider {
    private val zipFs: FileSystem by lazy { openEpubZip(fileSystem, zipPath) }

    override fun getResourceBytes(href: String): ByteArray {
        val entryPath = "/$href".toPath()
        return zipFs.read(entryPath) { readByteArray() }
    }
}
