package io.documentnode.epub4kmp.util

import io.documentnode.epub4kmp.Constants
import io.documentnode.epub4kmp.domain.MediaTypes
import io.documentnode.epub4kmp.domain.Resource
import io.documentnode.epub4kmp.epub.EpubProcessorSupport
import nl.adaptivity.xmlutil.dom2.Document

/**
 * Utility helpers for [Resource] instances.
 */
object ResourceUtil {
    /**
     * Creates a [Resource] for the given EPUB-relative href, with the given bytes.
     */
    fun createResource(href: String, bytes: ByteArray): Resource =
        Resource(null, bytes, href, MediaTypes.determineMediaType(href))

    /**
     * Creates a resource with as contents an HTML page with the given title.
     */
    fun createResource(title: String, href: String): Resource {
        val content =
            "<html><head><title>$title</title></head><body><h1>$title</h1></body></html>"
        return Resource(
            null,
            content.encodeToByteArray(),
            href,
            MediaTypes.XHTML,
            Constants.CHARACTER_ENCODING
        )
    }

    /**
     * Reads & parses the given resource as an XML [Document].
     */
    fun getAsDocument(resource: Resource): Document =
        EpubProcessorSupport.parseDocument(resource.bytes(), resource.inputEncoding)
}
