package io.documentnode.epub4kmp.epub

import nl.adaptivity.xmlutil.*
import nl.adaptivity.xmlutil.dom2.Document

/**
 * Low-level XML helpers used across the EPUB reader/writer code.
 *
 * Wraps xmlutil's [xmlStreaming] for parsing to [Document] and writing
 * EPUB-shaped XML.
 */
object EpubProcessorSupport {
    /**
     * Parses [xml] into a DOM2 [Document].
     */
    @OptIn(ExperimentalXmlUtilApi::class, XmlUtilInternal::class)
    fun parseDocument(xml: CharSequence): Document {
        val reader = xmlStreaming.newReader(xml)
        val writer = xmlStreaming.newWriter()
        while (reader.hasNext()) {
            reader.next()
            reader.writeCurrent(writer)
        }
        return writer.target
    }

    /**
     * Parses [bytes], decoded as [encoding], into a DOM2 [Document].
     */
    fun parseDocument(bytes: ByteArray, encoding: String = "UTF-8"): Document =
        parseDocument(bytes.decodeToString())

    /**
     * Creates an indenting [XmlWriter] that appends XML to [output].
     */
    fun createXmlWriter(output: Appendable): XmlWriter =
        xmlStreaming.newGenericWriter(
            output = output,
            isRepairNamespaces = true,
            xmlDeclMode = XmlDeclMode.Charset
        ).also { it.indentString = "  " }
}
