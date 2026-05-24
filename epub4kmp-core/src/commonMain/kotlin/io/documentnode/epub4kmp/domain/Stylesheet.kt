package io.documentnode.epub4kmp.domain

/**
 * A CSS [Resource]. Sugar over the raw [Resource] constructor with the right
 * media type, a sensible default href, and a [css] accessor.
 *
 * Register on a [Book] with [Book.addStylesheet] to also have it auto-linked
 * from every XHTML page at write time (via [io.documentnode.epub4kmp.epub.StylesheetLinker]).
 */
class Stylesheet(
    href: String = DEFAULT_HREF,
    css: String,
    id: String? = null,
) : Resource(id, css.encodeToByteArray(), href, MediaTypes.CSS) {

    /** The CSS source. */
    val css: String get() = asString()

    companion object {
        const val DEFAULT_HREF: String = "styles/book.css"
    }
}
