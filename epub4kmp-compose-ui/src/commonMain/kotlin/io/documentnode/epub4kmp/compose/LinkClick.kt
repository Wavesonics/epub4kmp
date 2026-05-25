package io.documentnode.epub4kmp.compose

import io.documentnode.epub4kmp.domain.Resource

/**
 * The result of a user clicking an `<a>` tag inside a chapter rendered by
 * [EpubContent].
 *
 * The chapter HTML usually contains hrefs that are *relative to the chapter's
 * own path* (e.g. `chapter3.xhtml` or `../Text/chapter3.xhtml#sec2`), which
 * means raw `Book.resources.getByHref(href)` won't find them. [EpubContent]
 * resolves those relative paths against the current chapter before invoking
 * `onLinkClicked` and surfaces the result here.
 *
 * @property href the original href as it appeared in the chapter HTML
 * @property resource the in-book resource the link points at, or `null` if the
 *   link is external (`http://`, `mailto:`, etc.) or cannot be resolved
 * @property fragmentId the optional `#fragment` portion of the href, if any
 */
class LinkClick internal constructor(
	val href: String,
	val resource: Resource?,
	val fragmentId: String?,
)
