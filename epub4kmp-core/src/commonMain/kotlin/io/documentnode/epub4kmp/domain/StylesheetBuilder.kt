package io.documentnode.epub4kmp.domain

/**
 * Builds a [Stylesheet] using a narrow typed DSL covering the common ebook
 * cases (typography, spacing, page breaks). Use [RuleBuilder.property] or
 * [StylesheetBuilder.raw] as escape hatches for anything not covered.
 *
 * ```
 * val css = stylesheet {
 *   body { fontFamily("Georgia, serif"); lineHeight(1.5) }
 *   heading(1) { fontSize("1.8em") }
 *   raw("p.first-line::first-line { font-variant: small-caps; }")
 * }
 * ```
 */
fun stylesheet(
    href: String = Stylesheet.DEFAULT_HREF,
    build: StylesheetBuilder.() -> Unit,
): Stylesheet = Stylesheet(
    href = href,
    css = StylesheetBuilder().apply(build).render(),
)

@DslMarker
annotation class CssDsl

@CssDsl
class StylesheetBuilder {
    private val rules = mutableListOf<Rule>()

    fun body(block: RuleBuilder.() -> Unit) = rule("body", block)
    fun paragraph(block: RuleBuilder.() -> Unit) = rule("p", block)
    fun blockquote(block: RuleBuilder.() -> Unit) = rule("blockquote", block)
    fun link(block: RuleBuilder.() -> Unit) = rule("a", block)
    fun image(block: RuleBuilder.() -> Unit) = rule("img", block)

    /**
     * Targets the first paragraph in its container (`p:first-of-type`).
     *
     * Standard prose convention: indent every paragraph's first line *except*
     * the opening one of each section.
     *
     * ```
     * paragraph { textIndent("1.5em") }
     * firstParagraph { textIndent("0") }
     * ```
     */
    fun firstParagraph(block: RuleBuilder.() -> Unit) = rule("p:first-of-type", block)

    /**
     * Targets any paragraph that immediately follows another paragraph
     * (`p + p`). Use this for the alternative convention where the indent
     * is added by the *following* paragraph rather than removed from the
     * first — handy if your XHTML mixes paragraphs and figures and you only
     * want to indent run-on prose.
     */
    fun paragraphAfterParagraph(block: RuleBuilder.() -> Unit) = rule("p + p", block)

    /**
     * Targets the first formatted line of paragraphs (`p::first-line`).
     * Use for small caps on the opening line of each paragraph.
     */
    fun paragraphFirstLine(block: RuleBuilder.() -> Unit) = rule("p::first-line", block)

    /**
     * Targets the first letter of paragraphs (`p::first-letter`). Use for
     * drop caps. Pair with [firstParagraph] (and combine via [selector]
     * with `p:first-of-type::first-letter`) for chapter-opener drop caps.
     */
    fun paragraphFirstLetter(block: RuleBuilder.() -> Unit) = rule("p::first-letter", block)

    fun heading(level: Int, block: RuleBuilder.() -> Unit) {
        require(level in 1..6) { "heading level must be 1..6, was $level" }
        rule("h$level", block)
    }

    /** Escape hatch for arbitrary selectors. */
    fun selector(selector: String, block: RuleBuilder.() -> Unit) = rule(selector, block)

    /** Escape hatch for arbitrary CSS — `@font-face`, media queries, anything. */
    fun raw(css: String) {
        rules.add(Rule.Raw(css.trimIndent()))
    }

    private fun rule(selector: String, block: RuleBuilder.() -> Unit) {
        rules.add(Rule.Block(selector, RuleBuilder().apply(block).properties))
    }

    fun render(): String = buildString {
        for (rule in rules) {
            when (rule) {
                is Rule.Block -> {
                    append(rule.selector).append(" {\n")
                    for ((k, v) in rule.props) {
                        append("  ").append(k).append(": ").append(v).append(";\n")
                    }
                    append("}\n\n")
                }
                is Rule.Raw -> append(rule.css).append("\n\n")
            }
        }
    }

    private sealed interface Rule {
        data class Block(val selector: String, val props: List<Pair<String, String>>) : Rule
        data class Raw(val css: String) : Rule
    }
}

@CssDsl
class RuleBuilder {
    internal val properties = mutableListOf<Pair<String, String>>()

    // Typography
    fun fontFamily(value: String) = set("font-family", value)
    fun fontSize(value: String) = set("font-size", value)
    fun fontWeight(value: String) = set("font-weight", value)
    fun fontStyle(value: String) = set("font-style", value)
    fun fontVariant(value: String) = set("font-variant", value)
    fun lineHeight(value: Double) = set("line-height", value.toString())
    fun lineHeight(value: String) = set("line-height", value)
    fun textAlign(value: String) = set("text-align", value)
    fun textIndent(value: String) = set("text-indent", value)
    fun color(value: String) = set("color", value)

    // Spacing — em-based overload is the common case for ebooks
    fun margin(em: Double) = set("margin", "${em}em")
    fun margin(value: String) = set("margin", value)
    fun marginTop(value: String) = set("margin-top", value)
    fun marginBottom(value: String) = set("margin-bottom", value)
    fun padding(value: String) = set("padding", value)

    // Ebook-specific
    fun pageBreakBefore(value: String) = set("page-break-before", value)
    fun pageBreakAfter(value: String) = set("page-break-after", value)
    fun pageBreakInside(value: String) = set("page-break-inside", value)

    /** Escape hatch for any property not covered above. */
    fun property(name: String, value: String) = set(name, value)

    private fun set(name: String, value: String) {
        properties.add(name to value)
    }
}
