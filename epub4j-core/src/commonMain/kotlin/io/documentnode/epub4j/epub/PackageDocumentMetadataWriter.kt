package io.documentnode.epub4j.epub

import io.documentnode.epub4j.Constants
import io.documentnode.epub4j.domain.Book
import io.documentnode.epub4j.domain.Identifier
import nl.adaptivity.xmlutil.XmlWriter

object PackageDocumentMetadataWriter : PackageDocumentBase() {
    /**
     * Writes the book's metadata into the OPF [writer].
     */
    fun writeMetaData(book: Book, writer: XmlWriter) {
        writer.startTag(NAMESPACE_OPF, OPFTags.metadata, "")

        writeIdentifiers(book.metadata.getIdentifiers(), writer)
        writeSimpleMetadataElements(DCTags.title, book.metadata.getTitles(), writer)
        writeSimpleMetadataElements(DCTags.subject, book.metadata.subjects, writer)
        writeSimpleMetadataElements(DCTags.description, book.metadata.getDescriptions(), writer)
        writeSimpleMetadataElements(DCTags.publisher, book.metadata.getPublishers(), writer)
        writeSimpleMetadataElements(DCTags.type, book.metadata.getTypes(), writer)
        writeSimpleMetadataElements(DCTags.rights, book.metadata.rights, writer)

        // authors
        for ((firstname, lastname, relator) in book.metadata.getAuthors()) {
            writer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.creator, PREFIX_DUBLIN_CORE)
            writer.attribute(NAMESPACE_OPF, OPFAttributes.role, PREFIX_OPF, relator.code)
            writer.attribute(NAMESPACE_OPF, OPFAttributes.file_as, PREFIX_OPF, "$lastname, $firstname")
            writer.text("$firstname $lastname")
            writer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.creator, PREFIX_DUBLIN_CORE)
        }

        // contributors
        for ((firstname, lastname, relator) in book.metadata.getContributors()) {
            writer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.contributor, PREFIX_DUBLIN_CORE)
            writer.attribute(NAMESPACE_OPF, OPFAttributes.role, PREFIX_OPF, relator.code)
            writer.attribute(NAMESPACE_OPF, OPFAttributes.file_as, PREFIX_OPF, "$lastname, $firstname")
            writer.text("$firstname $lastname")
            writer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.contributor, PREFIX_DUBLIN_CORE)
        }

        // dates
        for (date in book.metadata.getDates()) {
            writer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.date, PREFIX_DUBLIN_CORE)
            date.event?.let {
                writer.attribute(NAMESPACE_OPF, OPFAttributes.event, PREFIX_OPF, it.toString())
            }
            writer.text(date.value)
            writer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.date, PREFIX_DUBLIN_CORE)
        }

        // language
        if (book.metadata.language.isNotBlank()) {
            writer.startTag(NAMESPACE_DUBLIN_CORE, "language", PREFIX_DUBLIN_CORE)
            writer.text(book.metadata.language)
            writer.endTag(NAMESPACE_DUBLIN_CORE, "language", PREFIX_DUBLIN_CORE)
        }

        // other properties (EPUB 3 <meta property="ns:foo">)
        for ((key, value) in book.metadata.otherProperties) {
            // Use the getter methods directly: in JVM, QName is javax.xml.namespace.QName
            // which has the same field names (namespaceURI/prefix/localPart) but as private
            // fields, so the xmlutil extension properties are shadowed. Calling the getters
            // explicitly works on every target.
            val ns = key.getNamespaceURI()
            val prefix = key.getPrefix().ifEmpty { null }
            val localPart = key.getLocalPart()
            writer.startTag(ns, OPFTags.meta, prefix)
            writer.attribute(null, OPFAttributes.property, null, localPart)
            writer.text(value)
            writer.endTag(ns, OPFTags.meta, prefix)
        }

        // cover image
        book.coverImage?.id?.let { coverId ->
            writer.startTag(NAMESPACE_OPF, OPFTags.meta, "")
            writer.attribute(null, OPFAttributes.name, null, OPFValues.meta_cover)
            writer.attribute(null, OPFAttributes.content, null, coverId)
            writer.endTag(NAMESPACE_OPF, OPFTags.meta, "")
        }

        // generator
        writer.startTag(NAMESPACE_OPF, OPFTags.meta, "")
        writer.attribute(null, OPFAttributes.name, null, OPFValues.generator)
        writer.attribute(null, OPFAttributes.content, null, Constants.EPUB4J_GENERATOR_NAME)
        writer.endTag(NAMESPACE_OPF, OPFTags.meta, "")

        writer.endTag(NAMESPACE_OPF, OPFTags.metadata, "")
    }

    private fun writeSimpleMetadataElements(
        tagName: String,
        values: List<String>,
        writer: XmlWriter
    ) {
        for (value in values) {
            if (value.isNotBlank()) {
                writer.startTag(NAMESPACE_DUBLIN_CORE, tagName, PREFIX_DUBLIN_CORE)
                writer.text(value)
                writer.endTag(NAMESPACE_DUBLIN_CORE, tagName, PREFIX_DUBLIN_CORE)
            }
        }
    }

    /**
     * Writes the list of [Identifier]s. The first one with `isBookId == true` (or the
     * first one if none) becomes the unique book identifier.
     */
    private fun writeIdentifiers(identifiers: List<Identifier>, writer: XmlWriter) {
        val bookIdIdentifier = Identifier.getBookIdIdentifier(identifiers) ?: return

        writer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.identifier, PREFIX_DUBLIN_CORE)
        writer.attribute(null, DCAttributes.id, null, BOOK_ID_ID)
        writer.attribute(NAMESPACE_OPF, OPFAttributes.scheme, PREFIX_OPF, bookIdIdentifier.scheme)
        writer.text(bookIdIdentifier.value)
        writer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.identifier, PREFIX_DUBLIN_CORE)

        for (identifier in identifiers) {
            if (identifier == bookIdIdentifier) continue
            writer.startTag(NAMESPACE_DUBLIN_CORE, DCTags.identifier, PREFIX_DUBLIN_CORE)
            writer.attribute(NAMESPACE_OPF, "scheme", PREFIX_OPF, identifier.scheme)
            writer.text(identifier.value)
            writer.endTag(NAMESPACE_DUBLIN_CORE, DCTags.identifier, PREFIX_DUBLIN_CORE)
        }
    }
}
