package io.documentnode.epub4kmp.domain

/**
 * Loads the bytes for a resource on demand.
 *
 * Implementations are typically backed by a ZIP file on disk; the bytes
 * for a given href are read out of the archive when [getResourceBytes] is called.
 */
interface LazyResourceProvider {
    fun getResourceBytes(href: String): ByteArray
}
