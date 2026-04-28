package io.documentnode.epub4j.domain

import io.documentnode.epub4j.Constants
import io.documentnode.epub4j.domain.MediaTypes.determineMediaType
import okio.Buffer
import okio.Source

/**
 * Represents a resource that is part of the epub.
 * A resource can be a html file, image, xml, etc.
 */
open class Resource(
    var id: String?,
    open var data: ByteArray?,
    var href: String?,
    var mediaType: MediaType?,
    var inputEncoding: String = Constants.CHARACTER_ENCODING
) {
    constructor(href: String) : this(null, ByteArray(0), href, determineMediaType(href))

    constructor(data: ByteArray, mediaType: MediaType?) :
        this(null, data, null, mediaType)

    constructor(data: ByteArray, href: String) : this(
        null,
        data,
        href,
        determineMediaType(href),
        Constants.CHARACTER_ENCODING
    )

    constructor(id: String, data: ByteArray, href: String) : this(
        id,
        data,
        href,
        determineMediaType(href),
        Constants.CHARACTER_ENCODING
    )

    /**
     * Gets the contents of the Resource as an okio [Source].
     */
    open fun source(): Source = Buffer().write(data ?: ByteArray(0))

    /** Returns the resource's bytes (loading them if lazy). */
    open fun bytes(): ByteArray = data ?: ByteArray(0)

    /** Returns the resource's bytes decoded as a String using the resource's [inputEncoding]. */
    fun asString(): String = bytes().decodeToString()

    /**
     * Tells this resource to release its cached data.
     *
     * If this resource was not lazy-loaded, this is a no-op.
     */
    open fun close() {}

    open val size: Long
        get() = data?.size?.toLong() ?: 0
}
