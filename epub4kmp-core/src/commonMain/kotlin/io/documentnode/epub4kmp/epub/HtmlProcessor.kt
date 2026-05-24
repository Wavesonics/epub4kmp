package io.documentnode.epub4kmp.epub

import io.documentnode.epub4kmp.domain.Resource
import okio.Sink

/**
 * Hook for transforming an HTML/XHTML [Resource]'s contents during EPUB
 * generation — e.g. tag rewriting, sanitizing, or dropping legacy markup.
 */
interface HtmlProcessor {
    fun processHtmlResource(resource: Resource, out: Sink)
}
