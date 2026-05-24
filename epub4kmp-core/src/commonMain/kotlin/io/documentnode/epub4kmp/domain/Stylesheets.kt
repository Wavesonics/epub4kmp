package io.documentnode.epub4kmp.domain

/** Curated, opinionated stylesheets you can drop in without thinking. */
object Stylesheets {

    /**
     * A readable serif default: indent paragraphs after the first, generous
     * line-height, page-break before each H1 (chapter break).
     */
    fun defaultReader(): Stylesheet = stylesheet {
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
