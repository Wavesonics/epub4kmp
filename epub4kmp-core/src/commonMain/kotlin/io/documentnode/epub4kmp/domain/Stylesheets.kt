package io.documentnode.epub4kmp.domain

import io.documentnode.epub4kmp.domain.Stylesheets.DEFAULT_READER_HREF
import io.documentnode.epub4kmp.domain.Stylesheets.defaultReader


/** Curated, opinionated stylesheets you can drop in without thinking. */
object Stylesheets {

    /**
     * Default href for [defaultReader]. Distinct from [Stylesheet.DEFAULT_HREF]
     * so callers who pass through both the default reader sheet AND their own
     * bare `Stylesheet(css = ...)` won't collide.
     */
    const val DEFAULT_READER_HREF: String = "styles/__epub4kmp_default_reader.css"

    /**
     * A readable serif default: indent paragraphs after the first, generous
     * line-height, page-break before each H1 (chapter break).
     *
     * @param href where to register this stylesheet inside the book. The
     *   default ([DEFAULT_READER_HREF]) is namespaced so it does not collide
     *   with paths real EPUBs commonly use (e.g. `styles/book.css`); override
     *   only if you have a reason to.
     */
    fun defaultReader(href: String = DEFAULT_READER_HREF): Stylesheet = stylesheet(href = href) {
        body {
            fontFamily("Georgia, \"Times New Roman\", serif")
            lineHeight(1.5)
            margin("1em")
        }
        paragraph {
            textIndent("1.5em")
            margin("0")
        }
        firstParagraph { textIndent("0") }
        for (level in 1..6) heading(level) {
            fontWeight("bold")
            marginTop("1.5em")
            marginBottom("0.5em")
            textIndent("0")
        }
        heading(1) { fontSize("1.8em"); pageBreakBefore("always") }
        heading(2) { fontSize("1.4em") }
        blockquote {
            margin("1em 2em")
            fontStyle("italic")
        }
        image {
            property("max-width", "100%")
            property("height", "auto")
        }
    }
}
