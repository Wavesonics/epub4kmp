package io.documentnode.epub4kmp.domain

/**
 * A Resource that loads its bytes only on-demand from an EPUB file.
 *
 * The data is loaded on the first call that requires it, and can be released
 * via [close].
 */
class LazyResource(
    private val resourceProvider: LazyResourceProvider,
    private val cachedSize: Long,
    href: String
) : Resource(
    null,
    null,
    href,
    MediaTypes.determineMediaType(href)
) {
    constructor(resourceProvider: LazyResourceProvider, href: String) :
        this(resourceProvider, -1, href)

    override var data: ByteArray? = null
        get() = field ?: resourceProvider.getResourceBytes(href!!).also { field = it }

    /** Tells this resource to release its cached data. */
    override fun close() {
        data = null
    }

    override val size: Long
        get() = if (cachedSize >= 0) cachedSize else (data?.size?.toLong() ?: 0)
}
