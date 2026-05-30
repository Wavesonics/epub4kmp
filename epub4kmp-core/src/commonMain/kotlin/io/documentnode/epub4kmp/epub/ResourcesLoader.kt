package io.documentnode.epub4kmp.epub

import io.documentnode.epub4kmp.domain.*
import io.documentnode.epub4kmp.util.ResourceUtil
import io.documentnode.epub4kmp.util.openEpubZip
import no.synth.kmpzip.okio.asInputStream
import no.synth.kmpzip.zip.ZipInputStream
import okio.*
import okio.Path.Companion.toPath

/**
 * Loads [Resources] out of EPUB archives.
 */
object ResourcesLoader {
    private val ROOT: Path = "/".toPath()

    /**
     * Loads all entries from the given [Source] (a streaming ZIP).
     *
     * Reads everything into memory; cheap to call but uses memory proportional
     * to the EPUB's content size.
     */
    fun loadResources(
        source: Source,
        defaultHtmlEncoding: String
    ): Resources {
        val resources = Resources()
        ZipInputStream(source.buffer().asInputStream()).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                if (entry.isDirectory) continue
                val bytes = zis.readBytes()
                val resource = ResourceUtil.createResource(entry.name, bytes)
                if (resource.mediaType == MediaTypes.XHTML) {
                    resource.inputEncoding = defaultHtmlEncoding
                }
                resources.add(resource)
            }
        }
        return resources
    }

    /**
     * Loads entries from the ZIP at [zipPath] in [fileSystem].
     *
     * Resources with a [MediaType] in [lazyLoadedTypes] are returned as
     * [LazyResource] instances that read their bytes from the ZIP on demand.
     */
    fun loadResources(
        fileSystem: FileSystem,
        zipPath: Path,
        defaultHtmlEncoding: String,
        lazyLoadedTypes: List<MediaType> = emptyList()
    ): Resources {
        val zipFs = openEpubZip(fileSystem, zipPath)
        val provider = EpubResourceProvider(fileSystem, zipPath)
        val resources = Resources()

        fun walk(dir: Path) {
            for (path in zipFs.list(dir)) {
                val meta = zipFs.metadata(path)
                if (meta.isDirectory) {
                    walk(path)
                    continue
                }
                val href = path.toString().trimStart('/')
                val resource: Resource = if (shouldLoadLazy(href, lazyLoadedTypes)) {
                    LazyResource(provider, meta.size ?: -1L, href)
                } else {
                    val bytes = zipFs.read(path) { readByteArray() }
                    ResourceUtil.createResource(href, bytes)
                }
                if (resource.mediaType == MediaTypes.XHTML) {
                    resource.inputEncoding = defaultHtmlEncoding
                }
                resources.add(resource)
            }
        }
        walk(ROOT)
        return resources
    }

    private fun shouldLoadLazy(
        href: String,
        lazilyLoadedMediaTypes: List<MediaType>
    ): Boolean {
        if (lazilyLoadedMediaTypes.isEmpty()) return false
        return lazilyLoadedMediaTypes.contains(MediaTypes.determineMediaType(href))
    }
}
