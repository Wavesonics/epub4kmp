package io.documentnode.epub4kmp

import io.documentnode.epub4kmp.domain.Stylesheet
import io.documentnode.epub4kmp.domain.stylesheet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StylesheetBuilderTest {

    @Test fun emitsSelectorsAndProperties() {
        val sheet = stylesheet {
            body { fontFamily("serif"); lineHeight(1.5) }
            paragraph { textIndent("1.5em") }
            heading(1) { fontSize("1.8em") }
        }
        val css = sheet.css
        assertTrue(css.contains("body {"))
        assertTrue(css.contains("font-family: serif;"))
        assertTrue(css.contains("line-height: 1.5;"))
        assertTrue(css.contains("p {"))
        assertTrue(css.contains("text-indent: 1.5em;"))
        assertTrue(css.contains("h1 {"))
        assertTrue(css.contains("font-size: 1.8em;"))
    }

    @Test fun rawIsEmittedVerbatim() {
        val sheet = stylesheet {
            raw("@font-face { font-family: x; src: url('x.ttf'); }")
        }
        assertTrue(sheet.css.contains("@font-face"))
    }

    @Test fun propertyEscapeHatch() {
        val sheet = stylesheet {
            body { property("font-feature-settings", "\"liga\" 1") }
        }
        assertTrue(sheet.css.contains("font-feature-settings: \"liga\" 1;"))
    }

    @Test fun headingLevelMustBe1to6() {
        assertFailsWith<IllegalArgumentException> {
            stylesheet { heading(0) {} }
        }
        assertFailsWith<IllegalArgumentException> {
            stylesheet { heading(7) {} }
        }
    }

    @Test fun emBasedMarginOverload() {
        val sheet = stylesheet { paragraph { margin(em = 1.5) } }
        assertTrue(sheet.css.contains("margin: 1.5em;"))
    }

    @Test fun defaultHrefAndMediaType() {
        val sheet = stylesheet { body { color("black") } }
        assertEquals(Stylesheet.DEFAULT_HREF, sheet.href)
    }

    @Test fun proseParagraphSelectors() {
        val sheet = stylesheet {
            paragraph { textIndent("1.5em") }
            firstParagraph { textIndent("0") }
            paragraphAfterParagraph { textIndent("1.5em") }
            paragraphFirstLine { fontVariant("small-caps") }
            paragraphFirstLetter { fontSize("3em") }
        }
        val css = sheet.css
        assertTrue(css.contains("p {"))
        assertTrue(css.contains("p:first-of-type {"))
        assertTrue(css.contains("p + p {"))
        assertTrue(css.contains("p::first-line {"))
        assertTrue(css.contains("p::first-letter {"))
        assertTrue(css.contains("font-variant: small-caps;"))
    }
}
