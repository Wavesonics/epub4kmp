package io.documentnode.epub4kmp.epub

import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.domain.MediaTypes

/**
 * Injects `<link rel="stylesheet" .../>` tags into every XHTML resource for
 * each href in [Book.stylesheets]. Idempotent — re-running it (or running it
 * on a book that already has links) is a no-op.
 *
 * Path resolution: stylesheet hrefs are made relative to each page's own href,
 * so a stylesheet at `styles/book.css` becomes `styles/book.css` from
 * `chapter-1.xhtml` and `../styles/book.css` from `text/chapter-1.xhtml`.
 *
 * Wired into [EpubWriter]'s default pipeline. Users who don't want this can
 * pass their own [BookProcessor] to the [EpubWriter] constructor.
 *
 * When [Book.stylesheets] is empty this processor returns immediately and
 * lazy-loaded resources stay lazy. Otherwise every XHTML page is read and
 * rewritten, materializing any lazy XHTML for the rest of the write.
 */
class StylesheetLinker : BookProcessor {
    override fun processBook(book: Book): Book {
        if (book.stylesheets.isEmpty()) return book
        val pages = book.resources.getResourcesByMediaType(MediaTypes.XHTML)
        for (page in pages) {
            val pageHref = page.href ?: continue
            val rels = book.stylesheets.map { relativeHref(from = pageHref, to = it) }
            page.data = injectLinks(page.asString(), rels).encodeToByteArray()
        }
        return book
    }

    /**
     * Inserts a `<link>` for every entry in [stylesheetHrefs] that isn't
     * already present in [xhtml]. Handles three cases, in order:
     *
     * 1. Document has `</head>` → insert before it.
     * 2. Document has a self-closing `<head/>` → expand to a full `<head>…</head>`.
     * 3. Document has no head at all → insert one right after `<html ...>`.
     */
    internal fun injectLinks(xhtml: String, stylesheetHrefs: List<String>): String {
        if (stylesheetHrefs.isEmpty()) return xhtml
        val missing = stylesheetHrefs.filterNot { href -> isAlreadyLinked(xhtml, href) }
        if (missing.isEmpty()) return xhtml
        val links = missing.joinToString("\n") {
            """<link rel="stylesheet" type="text/css" href="$it"/>"""
        }

        HEAD_CLOSE.find(xhtml)?.let { headClose ->
            return xhtml.substring(0, headClose.range.first) + links + "\n" +
                xhtml.substring(headClose.range.first)
        }

        HEAD_SELF_CLOSING.find(xhtml)?.let { selfClose ->
            // Replace `<head ... />` with `<head ...>\n$links\n</head>`.
            val openMatch = selfClose.value
            val openTag = openMatch.removeSuffix("/>").trimEnd() + ">"
            return xhtml.substring(0, selfClose.range.first) +
                "$openTag\n$links\n</head>" +
                xhtml.substring(selfClose.range.last + 1)
        }

        // No <head> at all — insert one right after the opening <html ...> tag.
        val htmlOpen = HTML_OPEN.find(xhtml) ?: return xhtml
        val insertAt = htmlOpen.range.last + 1
        return xhtml.substring(0, insertAt) +
            "\n<head>\n$links\n</head>\n" +
            xhtml.substring(insertAt)
    }

    /**
     * Checks whether a `<link>` tag already references the given [href].
     * Accepts single or double quotes and any attribute order.
     */
    internal fun isAlreadyLinked(xhtml: String, href: String): Boolean {
        val escaped = Regex.escape(href)
        return Regex(
            """<link\b[^>]*\bhref\s*=\s*["']$escaped["'][^>]*/?>""",
            RegexOption.IGNORE_CASE,
        ).containsMatchIn(xhtml)
    }

    /**
     * Computes [to] relative to the directory containing [from]. Both paths
     * are treated as forward-slash-separated EPUB hrefs (not OS paths).
     */
    internal fun relativeHref(from: String, to: String): String {
        val fromDirs = from.split('/').dropLast(1)
        val toParts = to.split('/')
        var common = 0
        while (
            common < fromDirs.size &&
            common < toParts.size - 1 &&
            fromDirs[common] == toParts[common]
        ) common++
        val ups = "../".repeat(fromDirs.size - common)
        val tail = toParts.drop(common).joinToString("/")
        return ups + tail
    }

    companion object {
        private val HEAD_CLOSE = Regex("</head\\s*>", RegexOption.IGNORE_CASE)
        private val HEAD_SELF_CLOSING = Regex("<head\\b[^>]*/>", RegexOption.IGNORE_CASE)
        private val HTML_OPEN = Regex("<html\\b[^>]*>", RegexOption.IGNORE_CASE)
    }
}
