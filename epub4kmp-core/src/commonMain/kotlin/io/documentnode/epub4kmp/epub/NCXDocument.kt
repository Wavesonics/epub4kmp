package io.documentnode.epub4kmp.epub

import io.documentnode.epub4kmp.Constants
import io.documentnode.epub4kmp.domain.*
import io.documentnode.epub4kmp.util.ResourceUtil
import io.documentnode.epub4kmp.util.StringUtil
import io.documentnode.epub4kmp.util.percentDecode
import nl.adaptivity.xmlutil.XmlWriter
import nl.adaptivity.xmlutil.dom2.*

/**
 * Reads and writes the NCX (Navigation Control file for XML) document
 * defined by namespace `http://www.daisy.org/z3986/2005/ncx/`.
 */
@Suppress("ConstPropertyName")
object NCXDocument {
    const val NAMESPACE_NCX: String = "http://www.daisy.org/z3986/2005/ncx/"
    const val PREFIX_NCX: String = "ncx"
    const val NCX_ITEM_ID: String = "ncx"
    const val DEFAULT_NCX_HREF: String = "toc.ncx"
    const val PREFIX_DTB: String = "dtb"

    fun read(book: Book, @Suppress("UNUSED_PARAMETER") epubReader: EpubReader): Resource? {
        val ncxResource = book.spine.tocResource ?: return null
        try {
            val ncxDocument = ResourceUtil.getAsDocument(ncxResource)
            val rootElement = ncxDocument.documentElement ?: return ncxResource
            val navMapElement = DOMUtil.getFirstElementByTagNameNS(
                rootElement, NAMESPACE_NCX, NCXTags.navMap
            ) ?: return ncxResource
            book.tableOfContents = TableOfContents(
                readTOCReferences(navMapElement.childNodes, book).toMutableList()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ncxResource
    }

    private fun readTOCReferences(navpoints: NodeList, book: Book): List<TOCReference> {
        val out = mutableListOf<TOCReference>()
        for (i in 0 until navpoints.length) {
            val node = navpoints.item(i) as? Element ?: continue
            if (node.localName != NCXTags.navPoint) continue
            out.add(readTOCReference(node, book))
        }
        return out
    }

    fun readTOCReference(navpointElement: Element, book: Book): TOCReference {
        val label = readNavLabel(navpointElement)
        val resourceHref = book.spine.tocResource?.href ?: ""
        var tocResourceRoot = resourceHref.substringBeforeLast('/')
        tocResourceRoot = if (tocResourceRoot.length == resourceHref.length) "" else "$tocResourceRoot/"
        val reference = StringUtil.collapsePathDots(tocResourceRoot + readNavReference(navpointElement))
        val href = reference.substringBefore(Constants.FRAGMENT_SEPARATOR_CHAR)
        val fragmentId = reference.substringAfter(Constants.FRAGMENT_SEPARATOR_CHAR)
        val resource = book.resources.getByHref(href)
        if (resource == null) {
            println("Resource with href $href in NCX document not found")
        }
        val result = TOCReference(label, resource, fragmentId)
        result.children = readTOCReferences(navpointElement.childNodes, book).toMutableList()
        return result
    }

    private fun readNavReference(navpointElement: Element): String {
        val contentElement = DOMUtil.getFirstElementByTagNameNS(
            navpointElement, NAMESPACE_NCX, NCXTags.content
        ) ?: return ""
        val raw = DOMUtil.getAttribute(contentElement, NAMESPACE_NCX, NCXAttributes.src)
        return raw.percentDecode()
    }

    private fun readNavLabel(navpointElement: Element): String {
        val navLabel = DOMUtil.getFirstElementByTagNameNS(
            navpointElement, NAMESPACE_NCX, NCXTags.navLabel
        ) ?: return ""
        val element = DOMUtil.getFirstElementByTagNameNS(
            navLabel, NAMESPACE_NCX, NCXTags.text
        ) ?: return ""
        return DOMUtil.getTextChildrenContent(element)
    }

    /**
     * Builds an NCX [Resource] containing the table of contents for [book].
     */
    fun createNCXResource(book: Book): Resource =
        createNCXResource(
            book.metadata.getIdentifiers(),
            book.title,
            book.metadata.getAuthors(),
            book.tableOfContents
        )

    fun createNCXResource(
        identifiers: List<Identifier>,
        title: String,
        authors: List<Author>,
        tableOfContents: TableOfContents
    ): Resource {
        val sb = StringBuilder()
        val writer = EpubProcessorSupport.createXmlWriter(sb)
        try {
            write(writer, identifiers, title, authors, tableOfContents)
        } finally {
            writer.close()
        }
        return Resource(
            NCX_ITEM_ID,
            sb.toString().encodeToByteArray(),
            DEFAULT_NCX_HREF,
            MediaTypes.NCX
        )
    }

    fun write(writer: XmlWriter, book: Book) {
        write(
            writer,
            book.metadata.getIdentifiers(),
            book.title,
            book.metadata.getAuthors(),
            book.tableOfContents
        )
    }

    fun write(
        writer: XmlWriter,
        identifiers: List<Identifier>,
        title: String,
        authors: List<Author>,
        tableOfContents: TableOfContents
    ) {
        writer.startDocument(version = "1.0", encoding = Constants.CHARACTER_ENCODING)
        writer.startTag(NAMESPACE_NCX, NCXTags.ncx, "")
        writer.attribute(null, NCXAttributes.version, null, NCXAttributeValues.version)

        writer.startTag(NAMESPACE_NCX, NCXTags.head, "")
        identifiers.forEach { writeMetaElement(it.scheme, it.value, writer) }
        writeMetaElement("generator", Constants.EPUB4KMP_GENERATOR_NAME, writer)
        writeMetaElement("depth", tableOfContents.calculateDepth().toString(), writer)
        writeMetaElement("totalPageCount", "0", writer)
        writeMetaElement("maxPageNumber", "0", writer)
        writer.endTag(NAMESPACE_NCX, NCXTags.head, "")

        writer.startTag(NAMESPACE_NCX, NCXTags.docTitle, "")
        writer.startTag(NAMESPACE_NCX, NCXTags.text, "")
        writer.text(StringUtil.defaultIfNull(title))
        writer.endTag(NAMESPACE_NCX, NCXTags.text, "")
        writer.endTag(NAMESPACE_NCX, NCXTags.docTitle, "")

        for (author in authors) {
            writer.startTag(NAMESPACE_NCX, NCXTags.docAuthor, "")
            writer.startTag(NAMESPACE_NCX, NCXTags.text, "")
            writer.text(author.lastname + ", " + author.firstname)
            writer.endTag(NAMESPACE_NCX, NCXTags.text, "")
            writer.endTag(NAMESPACE_NCX, NCXTags.docAuthor, "")
        }

        writer.startTag(NAMESPACE_NCX, NCXTags.navMap, "")
        writeNavPoints(tableOfContents.getTocReferences(), 1, writer)
        writer.endTag(NAMESPACE_NCX, NCXTags.navMap, "")

        writer.endTag(NAMESPACE_NCX, NCXTags.ncx, "")
        writer.endDocument()
    }

    private fun writeMetaElement(dtbName: String, content: String, writer: XmlWriter) {
        writer.startTag(NAMESPACE_NCX, NCXTags.meta, "")
        writer.attribute(null, NCXAttributes.name, null, "$PREFIX_DTB:$dtbName")
        writer.attribute(null, NCXAttributes.content, null, content)
        writer.endTag(NAMESPACE_NCX, NCXTags.meta, "")
    }

    private fun writeNavPoints(
        tocReferences: List<TOCReference>,
        playOrder: Int,
        writer: XmlWriter
    ): Int {
        var po = playOrder
        for (tocReference in tocReferences) {
            if (tocReference.resource == null) {
                po = writeNavPoints(tocReference.children, po, writer)
                continue
            }
            if (!tocReference.title.isNullOrBlank()) {
                writeNavPointStart(tocReference, po, writer)
                po++
                if (tocReference.children.isNotEmpty()) {
                    po = writeNavPoints(tocReference.children, po, writer)
                }
                writeNavPointEnd(writer)
            }
        }
        return po
    }

    private fun writeNavPointStart(tocReference: TOCReference, playOrder: Int, writer: XmlWriter) {
        writer.startTag(NAMESPACE_NCX, NCXTags.navPoint, "")
        writer.attribute(null, NCXAttributes.id, null, "navPoint-$playOrder")
        writer.attribute(null, NCXAttributes.playOrder, null, playOrder.toString())
        writer.attribute(null, NCXAttributes.clazz, null, NCXAttributeValues.chapter)
        writer.startTag(NAMESPACE_NCX, NCXTags.navLabel, "")
        writer.startTag(NAMESPACE_NCX, NCXTags.text, "")
        writer.text(tocReference.title ?: "")
        writer.endTag(NAMESPACE_NCX, NCXTags.text, "")
        writer.endTag(NAMESPACE_NCX, NCXTags.navLabel, "")
        writer.startTag(NAMESPACE_NCX, NCXTags.content, "")
        writer.attribute(null, NCXAttributes.src, null, tocReference.completeHref ?: "")
        writer.endTag(NAMESPACE_NCX, NCXTags.content, "")
    }

    private fun writeNavPointEnd(writer: XmlWriter) {
        writer.endTag(NAMESPACE_NCX, NCXTags.navPoint, "")
    }

    private interface NCXTags {
        companion object {
            const val ncx: String = "ncx"
            const val meta: String = "meta"
            const val navPoint: String = "navPoint"
            const val navMap: String = "navMap"
            const val navLabel: String = "navLabel"
            const val content: String = "content"
            const val text: String = "text"
            const val docTitle: String = "docTitle"
            const val docAuthor: String = "docAuthor"
            const val head: String = "head"
        }
    }

    private interface NCXAttributes {
        companion object {
            const val src: String = "src"
            const val name: String = "name"
            const val content: String = "content"
            const val id: String = "id"
            const val playOrder: String = "playOrder"
            const val clazz: String = "class"
            const val version: String = "version"
        }
    }

    private interface NCXAttributeValues {
        companion object {
            const val chapter: String = "chapter"
            const val version: String = "2005-1"
        }
    }
}
