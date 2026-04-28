package io.documentnode.epub4j.epub

import io.documentnode.epub4j.Constants
import io.documentnode.epub4j.domain.Book
import io.documentnode.epub4j.domain.MediaType
import io.documentnode.epub4j.domain.MediaTypes
import io.documentnode.epub4j.domain.Resource
import io.documentnode.epub4j.domain.Resources
import io.documentnode.epub4j.util.ResourceUtil
import okio.FileSystem
import okio.Path
import nl.adaptivity.xmlutil.dom2.Element
import nl.adaptivity.xmlutil.dom2.documentElement
import nl.adaptivity.xmlutil.dom2.length
import okio.Source

/**
 * Reads an EPUB file.
 */
class EpubReader {
    private val bookProcessor: BookProcessor = BookProcessor.IDENTITY_BOOKPROCESSOR

    /**
     * Reads an EPUB from an okio [Source]. Loads all resource bytes into memory.
     */
    fun readEpub(
        source: Source,
        encoding: String = Constants.CHARACTER_ENCODING
    ): Book = readEpub(ResourcesLoader.loadResources(source, encoding))

    /**
     * Reads an EPUB from a ZIP file in the given filesystem at [zipPath].
     *
     * Resources whose [MediaType] is in [lazyLoadedTypes] keep only a stub in
     * memory; their bytes are read from the ZIP on demand.
     */
    fun readEpub(
        fileSystem: FileSystem,
        zipPath: Path,
        encoding: String = Constants.CHARACTER_ENCODING,
        lazyLoadedTypes: List<MediaType> = emptyList()
    ): Book = readEpub(
        ResourcesLoader.loadResources(fileSystem, zipPath, encoding, lazyLoadedTypes)
    )

    fun readEpub(
        resources: Resources,
        result: Book = Book()
    ): Book {
        handleMimeType(result, resources)
        val packageResourceHref = getPackageResourceHref(resources)
        val packageResource = processPackageResource(
            packageResourceHref,
            result,
            resources
        ) ?: return result
        result.opfResource = packageResource
        result.ncxResource = processNcxResource(packageResource, result)
        return postProcessBook(result)
    }

    private fun postProcessBook(book: Book): Book = bookProcessor.processBook(book)

    private fun processNcxResource(
        @Suppress("UNUSED_PARAMETER") packageResource: Resource,
        book: Book
    ): Resource? = NCXDocument.read(book, this)

    private fun processPackageResource(
        packageResourceHref: String,
        book: Book,
        resources: Resources
    ): Resource? {
        val packageResource = resources.remove(packageResourceHref)
        try {
            packageResource?.let {
                PackageDocumentReader.read(it, this, book, resources)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return packageResource
    }

    private fun getPackageResourceHref(resources: Resources): String {
        val defaultResult = "OEBPS/content.opf"
        var result = defaultResult

        val containerResource = resources.remove("META-INF/container.xml") ?: return result
        try {
            val document = ResourceUtil.getAsDocument(containerResource)
            val docElement = document.documentElement ?: return result
            val rootfiles = docElement.getElementsByTagName("rootfiles")
            if (rootfiles.length == 0) return result
            val rootfilesElement = rootfiles.item(0) as? Element ?: return result
            val rootfile = rootfilesElement.getElementsByTagName("rootfile")
            if (rootfile.length == 0) return result
            val rootFileElement = rootfile.item(0) as? Element ?: return result
            result = rootFileElement.getAttribute("full-path") ?: defaultResult
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (result.isBlank()) {
            result = defaultResult
        }
        return result
    }

    private fun handleMimeType(@Suppress("UNUSED_PARAMETER") result: Book, resources: Resources) {
        resources.remove("mimetype")
    }
}
