package io.documentnode.epub4kmp.epub

import io.documentnode.epub4kmp.Constants
import io.documentnode.epub4kmp.domain.*
import nl.adaptivity.xmlutil.XmlWriter

/**
 * Writes the OPF package document defined by namespace `http://www.idpf.org/2007/opf`.
 */
object PackageDocumentWriter : PackageDocumentBase() {
    fun write(
        epubWriter: EpubWriter,
        writer: XmlWriter,
        book: Book
    ) {
        try {
            writer.startDocument(version = "1.0", encoding = Constants.CHARACTER_ENCODING)
            // Use the OPF namespace as the document default; declare the Dublin Core
            // prefix on the root so all <dc:*> children share the same declaration.
            writer.startTag(NAMESPACE_OPF, OPFTags.packageTag, "")
            writer.namespaceAttr(PREFIX_DUBLIN_CORE, NAMESPACE_DUBLIN_CORE)
            // Bind the opf prefix too — needed for opf:scheme / opf:role / opf:file-as
            // attributes used inside Dublin Core elements.
            writer.namespaceAttr(PREFIX_OPF, NAMESPACE_OPF)
            writer.attribute(null, OPFAttributes.version, null, "2.0")
            writer.attribute(null, OPFAttributes.uniqueIdentifier, null, BOOK_ID_ID)

            PackageDocumentMetadataWriter.writeMetaData(book, writer)

            writeManifest(book, epubWriter, writer)
            writeSpine(book, epubWriter, writer)
            writeGuide(book, epubWriter, writer)

            writer.endTag(NAMESPACE_OPF, OPFTags.packageTag, "")
            writer.endDocument()
            writer.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeSpine(
        book: Book,
        @Suppress("UNUSED_PARAMETER") epubWriter: EpubWriter,
        writer: XmlWriter
    ) {
        writer.startTag(NAMESPACE_OPF, OPFTags.spine, "")
        book.spine.tocResource?.id?.let { tocId ->
            writer.attribute(null, OPFAttributes.toc, null, tocId)
        }

        if (book.coverPage != null && book.spine.findFirstResourceById(book.coverPage?.id) < 0) {
            writer.startTag(NAMESPACE_OPF, OPFTags.itemref, "")
            book.coverPage?.id?.let { id ->
                writer.attribute(null, OPFAttributes.idref, null, id)
            }
            writer.attribute(null, OPFAttributes.linear, null, "no")
            writer.endTag(NAMESPACE_OPF, OPFTags.itemref, "")
        }
        writeSpineItems(book.spine, writer)
        writer.endTag(NAMESPACE_OPF, OPFTags.spine, "")
    }

    private fun writeManifest(
        book: Book,
        epubWriter: EpubWriter,
        writer: XmlWriter
    ) {
        writer.startTag(NAMESPACE_OPF, OPFTags.manifest, "")

        writer.startTag(NAMESPACE_OPF, OPFTags.item, "")
        writer.attribute(null, OPFAttributes.id, null, epubWriter.ncxId)
        writer.attribute(null, OPFAttributes.href, null, epubWriter.ncxHref)
        writer.attribute(null, OPFAttributes.media_type, null, epubWriter.ncxMediaType)
        writer.endTag(NAMESPACE_OPF, OPFTags.item, "")

        for (resource in book.resources.all.toList()) {
            writeItem(book, resource, writer)
        }

        writer.endTag(NAMESPACE_OPF, OPFTags.manifest, "")
    }

    private fun writeItem(book: Book, resource: Resource, writer: XmlWriter) {
        if (resource.mediaType == MediaTypes.NCX && book.spine.tocResource != null) return
        if (resource.id.isNullOrBlank()) {
            println("resource id must not be empty (href: ${resource.href}, mediatype:${resource.mediaType})")
            return
        }
        if (resource.href.isNullOrBlank()) {
            println("resource href must not be empty (id: ${resource.id}, mediatype:${resource.mediaType})")
            return
        }
        val mediaTypeName = resource.mediaType?.name
        if (mediaTypeName == null) {
            println("resource mediatype must not be empty (id: ${resource.id}, href:${resource.href})")
            return
        }
        writer.startTag(NAMESPACE_OPF, OPFTags.item, "")
        writer.attribute(null, OPFAttributes.id, null, resource.id!!)
        writer.attribute(null, OPFAttributes.href, null, resource.href!!)
        writer.attribute(null, OPFAttributes.media_type, null, mediaTypeName)
        writer.endTag(NAMESPACE_OPF, OPFTags.item, "")
    }

    private fun writeSpineItems(spine: Spine, writer: XmlWriter) {
        for (spineReference in spine.getSpineReferences()) {
            writer.startTag(NAMESPACE_OPF, OPFTags.itemref, "")
            spineReference.resourceId?.let { id ->
                writer.attribute(null, OPFAttributes.idref, null, id)
            }
            if (!spineReference.linear) {
                writer.attribute(null, OPFAttributes.linear, null, OPFValues.no)
            }
            writer.endTag(NAMESPACE_OPF, OPFTags.itemref, "")
        }
    }

    private fun writeGuide(
        book: Book,
        epubWriter: EpubWriter,
        writer: XmlWriter
    ) {
        writer.startTag(NAMESPACE_OPF, OPFTags.guide, "")
        ensureCoverPageGuideReferenceWritten(book.guide, epubWriter, writer)
        for (reference in book.guide.getReferences()) {
            writeGuideReference(reference, writer)
        }
        writer.endTag(NAMESPACE_OPF, OPFTags.guide, "")
    }

    private fun ensureCoverPageGuideReferenceWritten(
        guide: Guide,
        @Suppress("UNUSED_PARAMETER") epubWriter: EpubWriter,
        writer: XmlWriter
    ) {
        if (guide.getGuideReferencesByType(GuideReference.COVER).isNotEmpty()) return
        val coverPage = guide.coverPage ?: return
        writeGuideReference(
            GuideReference(coverPage, GuideReference.COVER, GuideReference.COVER),
            writer
        )
    }

    private fun writeGuideReference(reference: GuideReference, writer: XmlWriter) {
        writer.startTag(NAMESPACE_OPF, OPFTags.reference, "")
        reference.type?.takeIf { it.isNotBlank() }?.let {
            writer.attribute(null, OPFAttributes.type, null, it)
        }
        reference.completeHref?.takeIf { it.isNotBlank() }?.let {
            writer.attribute(null, OPFAttributes.href, null, it)
        }
        reference.title?.takeIf { it.isNotBlank() }?.let {
            writer.attribute(null, OPFAttributes.title, null, it)
        }
        writer.endTag(NAMESPACE_OPF, OPFTags.reference, "")
    }
}
