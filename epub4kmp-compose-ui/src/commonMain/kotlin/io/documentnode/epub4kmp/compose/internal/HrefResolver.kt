package io.documentnode.epub4kmp.compose.internal

import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.domain.Resource

internal const val EPUB_SCHEME = "epub"
internal const val EPUB_HOST = "book"

/**
 * Resolves an href (which may be relative to [base], absolute within the EPUB,
 * or an `epub://` URL produced by [buildUrl]) back to a [Resource].
 *
 * Strips a leading fragment from the href before lookup.
 */
internal fun resolveHref(book: Book, href: String, base: Resource? = null): Resource? =
	resolveLink(book, href, base).resource

/**
 * Like [resolveHref] but also returns the fragment portion (the part after `#`,
 * if any) so callers can position the WebView at an anchor after navigating.
 *
 * Returns `(null, null)` for external links (e.g. `http://`, `mailto:`) since
 * those don't map to a book resource.
 *
 * Percent-encoded hrefs are decoded before lookup: a chapter that links to
 * `<a href="Chapter%201.xhtml">` resolves to the resource stored under the
 * literal path `Chapter 1.xhtml`, since [io.documentnode.epub4kmp.domain.Resources]
 * keys by the decoded path.
 */
internal fun resolveLink(book: Book, href: String, base: Resource? = null): ResolvedLink {
	val rawFragment = href.substringAfter('#', missingDelimiterValue = "").ifEmpty { null }
	val fragment = rawFragment?.let(::percentDecode)
	val trimmed = href.substringBefore('#')
	if (trimmed.isEmpty()) {
		// Pure fragment link (`#sec2`) — stays on the current chapter.
		return ResolvedLink(resource = base, fragmentId = fragment)
	}
	// External URL → not resolvable as a book resource.
	if (Regex("^[a-z][a-z0-9+.\\-]*:", RegexOption.IGNORE_CASE).containsMatchIn(trimmed) &&
		!trimmed.startsWith("$EPUB_SCHEME://")
	) {
		return ResolvedLink(resource = null, fragmentId = fragment)
	}

	val decoded = percentDecode(trimmed)
	val path = when {
		decoded.startsWith("$EPUB_SCHEME://") -> decoded.removePrefix("$EPUB_SCHEME://$EPUB_HOST/")
		decoded.startsWith("/") -> decoded.removePrefix("/")
		base?.href != null -> joinHref(base.href!!, decoded)
		else -> decoded
	}

	return ResolvedLink(
		resource = book.resources.getByHref(normalizePath(path)),
		fragmentId = fragment,
	)
}

internal data class ResolvedLink(val resource: Resource?, val fragmentId: String?)

/**
 * Decodes `%XX` byte sequences in [s] as UTF-8. EPUB hrefs that contain a
 * space or non-ASCII character are typically percent-encoded in the XHTML
 * (`Chapter%201.xhtml`) but stored in [io.documentnode.epub4kmp.domain.Resources]
 * under the literal path (`Chapter 1.xhtml`) — so we must decode before lookup.
 *
 * Treats malformed `%XX` sequences as literal `%` followed by the two
 * characters, matching most browser behavior.
 *
 * Note: does not decode `+` to space; that's an HTML-form convention, not a
 * URL-path convention, and EPUB hrefs follow the URL-path rules.
 */
private fun percentDecode(s: String): String {
	if ('%' !in s) return s
	val out = ArrayList<Byte>(s.length)
	var i = 0
	while (i < s.length) {
		val c = s[i]
		if (c == '%' && i + 2 < s.length) {
			val hi = hexDigit(s[i + 1])
			val lo = hexDigit(s[i + 2])
			if (hi >= 0 && lo >= 0) {
				out.add(((hi shl 4) or lo).toByte())
				i += 3
				continue
			}
		}
		// Append this single char's UTF-8 bytes.
		for (b in c.toString().encodeToByteArray()) out.add(b)
		i++
	}
	return out.toByteArray().decodeToString()
}

private fun hexDigit(c: Char): Int = when (c) {
	in '0'..'9' -> c - '0'
	in 'a'..'f' -> c - 'a' + 10
	in 'A'..'F' -> c - 'A' + 10
	else -> -1
}

internal fun buildUrl(resource: Resource): String {
	val href = resource.href ?: return "$EPUB_SCHEME://$EPUB_HOST/"
	return "$EPUB_SCHEME://$EPUB_HOST/$href"
}

internal fun buildUrlFor(href: String): String = "$EPUB_SCHEME://$EPUB_HOST/$href"

/**
 * Joins [relative] onto the directory containing [from]. Both treated as
 * forward-slash EPUB hrefs (not OS paths).
 */
private fun joinHref(from: String, relative: String): String {
	val fromDir = from.substringBeforeLast('/', missingDelimiterValue = "")
	val combined = if (fromDir.isEmpty()) relative else "$fromDir/$relative"
	return normalizePath(combined)
}

private fun normalizePath(path: String): String {
	val out = mutableListOf<String>()
	for (part in path.split('/')) {
		when (part) {
			"", "." -> Unit
			".." -> if (out.isNotEmpty()) out.removeAt(out.lastIndex)
			else -> out.add(part)
		}
	}
	return out.joinToString("/")
}
