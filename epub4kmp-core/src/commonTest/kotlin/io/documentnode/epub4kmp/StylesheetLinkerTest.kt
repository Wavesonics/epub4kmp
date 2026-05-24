package io.documentnode.epub4kmp

import io.documentnode.epub4kmp.epub.StylesheetLinker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StylesheetLinkerTest {

    private val linker = StylesheetLinker()

    // -- relativeHref --------------------------------------------------------

    @Test fun relativeHref_sameDir() {
        assertEquals("styles/book.css", linker.relativeHref("chapter-1.xhtml", "styles/book.css"))
    }

    @Test fun relativeHref_pageInSubdir() {
        assertEquals(
            "../styles/book.css",
            linker.relativeHref("text/chapter-1.xhtml", "styles/book.css"),
        )
    }

    @Test fun relativeHref_pageDeeperThanSheet() {
        assertEquals(
            "../../styles/book.css",
            linker.relativeHref("text/parts/ch1.xhtml", "styles/book.css"),
        )
    }

    @Test fun relativeHref_sharedPrefix() {
        // Page text/ch1.xhtml + sheet text/styles/book.css → styles/book.css
        assertEquals(
            "styles/book.css",
            linker.relativeHref("text/ch1.xhtml", "text/styles/book.css"),
        )
    }

    @Test fun relativeHref_bothInSameSubdir() {
        assertEquals(
            "book.css",
            linker.relativeHref("text/ch1.xhtml", "text/book.css"),
        )
    }

    // -- injectLinks ---------------------------------------------------------

    @Test fun injectLinks_insertsBeforeHeadClose() {
        val xhtml = "<html><head><title>x</title></head><body/></html>"
        val out = linker.injectLinks(xhtml, listOf("styles/book.css"))
        assertTrue(
            out.contains("""<link rel="stylesheet" type="text/css" href="styles/book.css"/>"""),
            "link tag should be present, got: $out",
        )
        assertTrue(
            out.indexOf("<link") < out.indexOf("</head>"),
            "link must appear before </head>",
        )
    }

    @Test fun injectLinks_idempotent() {
        val xhtml = "<html><head><title>x</title></head><body/></html>"
        val once = linker.injectLinks(xhtml, listOf("styles/book.css"))
        val twice = linker.injectLinks(once, listOf("styles/book.css"))
        assertEquals(once, twice)
    }

    @Test fun injectLinks_skipsAlreadyLinkedHref() {
        val xhtml = """
            <html><head>
              <title>x</title>
              <link rel="stylesheet" href="styles/book.css"/>
            </head><body/></html>
        """.trimIndent()
        val out = linker.injectLinks(xhtml, listOf("styles/book.css"))
        assertEquals(xhtml, out, "existing link should not be duplicated")
    }

    @Test fun injectLinks_singleQuotedExistingLinkAlsoCounts() {
        val xhtml = "<html><head><link href='styles/book.css' rel='stylesheet'/></head><body/></html>"
        val out = linker.injectLinks(xhtml, listOf("styles/book.css"))
        assertEquals(xhtml, out)
    }

    @Test fun injectLinks_expandsSelfClosingHead() {
        val xhtml = "<html xmlns=\"x\"><head/><body><p>x</p></body></html>"
        val out = linker.injectLinks(xhtml, listOf("styles/book.css"))
        // Must not contain a self-closing head anymore.
        assertTrue(
            !Regex("<head\\b[^>]*/>").containsMatchIn(out),
            "self-closing <head/> should have been expanded, got: $out",
        )
        // Should now have a paired head with the link inside.
        val headOpen = out.indexOf("<head>")
        val headClose = out.indexOf("</head>")
        val linkAt = out.indexOf("""href="styles/book.css"""")
        assertTrue(headOpen in 0..linkAt, "link should be inside head, got: $out")
        assertTrue(linkAt < headClose, "link must precede </head>, got: $out")
    }

    @Test fun injectLinks_addsHeadWhenMissing() {
        val xhtml = "<html><body><p>hi</p></body></html>"
        val out = linker.injectLinks(xhtml, listOf("styles/book.css"))
        assertTrue(out.contains("<head>"), "head should be added")
        assertTrue(out.contains("<link"), "link should be added")
        assertTrue(
            out.indexOf("<head>") < out.indexOf("<body>"),
            "added head must precede body",
        )
    }

    @Test fun injectLinks_multipleStylesheets() {
        val xhtml = "<html><head/></html>"
        val out = linker.injectLinks(xhtml, listOf("a.css", "b.css"))
        assertTrue(out.contains("""href="a.css""""))
        assertTrue(out.contains("""href="b.css""""))
    }

    @Test fun injectLinks_emptyListReturnsInputUnchanged() {
        val xhtml = "<html><head/></html>"
        assertEquals(xhtml, linker.injectLinks(xhtml, emptyList()))
    }
}
