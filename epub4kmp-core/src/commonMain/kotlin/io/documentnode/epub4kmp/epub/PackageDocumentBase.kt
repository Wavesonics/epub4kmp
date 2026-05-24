package io.documentnode.epub4kmp.epub


/**
 * Functionality shared by the PackageDocumentReader and the PackageDocumentWriter
 *
 * @author paul
 */
open class PackageDocumentBase {
    protected interface DCTags {
        @Suppress("ConstPropertyName")
        companion object {
            const val title: String = "title"
            const val creator: String = "creator"
            const val subject: String = "subject"
            const val description: String = "description"
            const val publisher: String = "publisher"
            const val contributor: String = "contributor"
            const val date: String = "date"
            const val type: String = "type"
            const val format: String = "format"
            const val identifier: String = "identifier"
            const val source: String = "source"
            const val language: String = "language"
            const val relation: String = "relation"
            const val coverage: String = "coverage"
            const val rights: String = "rights"
        }
    }

    protected interface DCAttributes {
        @Suppress("ConstPropertyName")
        companion object {
            const val scheme: String = "scheme"
            const val id: String = "id"
        }
    }

    protected interface OPFTags {
        @Suppress("ConstPropertyName")
        companion object {
            const val metadata: String = "metadata"
            const val meta: String = "meta"
            const val manifest: String = "manifest"
            const val packageTag: String = "package"
            const val itemref: String = "itemref"
            const val spine: String = "spine"
            const val reference: String = "reference"
            const val guide: String = "guide"
            const val item: String = "item"
        }
    }

    protected interface OPFAttributes {
        @Suppress("ConstPropertyName")
        companion object {
            const val uniqueIdentifier: String = "unique-identifier"
            const val idref: String = "idref"
            const val name: String = "name"
            const val content: String = "content"
            const val type: String = "type"
            const val href: String = "href"
            const val linear: String = "linear"
            const val event: String = "event"
            const val role: String = "role"
            const val file_as: String = "file-as"
            const val id: String = "id"
            const val media_type: String = "media-type"
            const val title: String = "title"
            const val toc: String = "toc"
            const val version: String = "version"
            const val scheme: String = "scheme"
            const val property: String = "property"
        }
    }

    protected interface OPFValues {
        @Suppress("ConstPropertyName")
        companion object {
            const val meta_cover: String = "cover"
            const val reference_cover: String = "cover"
            const val no: String = "no"
            const val generator: String = "generator"
        }
    }

    @Suppress("ConstPropertyName")
    companion object {
        const val BOOK_ID_ID: String = "BookId"
        const val NAMESPACE_OPF: String = "http://www.idpf.org/2007/opf"
        const val NAMESPACE_DUBLIN_CORE: String = "http://purl.org/dc/elements/1.1/"
        const val PREFIX_DUBLIN_CORE: String = "dc"
        const val PREFIX_OPF: String = "opf"
        const val dateFormat: String = "yyyy-MM-dd"
    }
}