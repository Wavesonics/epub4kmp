package io.documentnode.epub4kmp.compose.internal

import io.documentnode.epub4kmp.domain.Book
import io.documentnode.epub4kmp.domain.MediaTypes
import io.documentnode.epub4kmp.domain.Resource
import io.documentnode.epub4kmp.domain.Stylesheets
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ChapterDocumentBuilderTest {

  private val chapterHtml = """
        <html><head><title>Ch1</title></head><body><p>hello</p></body></html>
    """.trimIndent()

  private fun bookWith(stylesheet: Boolean): Pair<Book, List<Resource>> {
    val pages = (1..3).map { i ->
      Resource(
        id = "ch$i",
        data = chapterHtml.encodeToByteArray(),
        href = "Text/ch$i.xhtml",
      ).apply { mediaType = MediaTypes.XHTML }
    }
    val book = Book().apply { pages.forEach { resources.add(it) } }
    if (stylesheet) book.addStylesheet(Stylesheets.defaultReader())
    return book to pages
  }

  @Test fun stylesheetCssReachesRenderedChapter() {
    val (book, pages) = bookWith(stylesheet = true)

    val html = buildChapterDocument(book, pages[0])

    // 'font-family: Georgia' is a stable marker from Stylesheets.defaultReader().
    assertContains(html, "font-family: Georgia")
  }

  /** Perf regression guard: rendering one chapter must not touch other chapters' bytes. */
  @Test fun doesNotMutateOtherChaptersBytes() {
    val (book, pages) = bookWith(stylesheet = true)
    val before = pages.map { it.bytes().copyOf() }

    buildChapterDocument(book, pages[0])

    for ((i, page) in pages.withIndex()) {
      assertContentEquals(
        before[i], page.bytes(),
        "page ${page.href} bytes mutated by buildChapterDocument(pages[0])",
      )
    }
  }

  @Test fun isIdempotentAcrossMultipleRenders() {
    val (book, pages) = bookWith(stylesheet = true)

    val first = buildChapterDocument(book, pages[0])
    val second = buildChapterDocument(book, pages[0])
    val third = buildChapterDocument(book, pages[0])

    assertEquals(first, second)
    assertEquals(second, third)

    val occurrences = Regex("font-family: Georgia").findAll(first).count()
    assertEquals(1, occurrences, "stylesheet was inlined more than once")
  }

  @Test fun noStylesheetMeansNoInlinedCss() {
    val (book, pages) = bookWith(stylesheet = false)
    val html = buildChapterDocument(book, pages[0])
    assertEquals(false, html.contains("font-family: Georgia"))
  }

  @Test fun handlesSelfClosingHead() {
    // Some Sigil/InDesign exports emit `<head/>` when the head has no children.
    // The theme/script inject helpers used to put their content BEFORE <html>
    // in that case, breaking dark-mode rendering and link navigation.
    val page = Resource(
      id = "ch1",
      data = "<html><head/><body><p>hi</p></body></html>".encodeToByteArray(),
      href = "ch1.xhtml",
    ).apply { mediaType = MediaTypes.XHTML }
    val book = Book().apply {
      resources.add(page)
      addStylesheet(Stylesheets.defaultReader())
    }

    val html = buildChapterDocument(book, page, backgroundCss = "#ffffff", textCss = "#000000")

    // Bridge script and theme style both landed inside the head, after the
    // expanded `<head>` open tag and before `</head>`.
    val headStart = html.indexOf("<head")
    val headEnd = html.indexOf("</head>")
    val htmlOpen = html.indexOf("<html")
    val bodyStart = html.indexOf("<body")

    assertEquals(true, headStart > htmlOpen, "head open after html open")
    assertEquals(true, headEnd in headStart..bodyStart, "</head> closes before <body>")
    assertEquals(true, html.indexOf("kmpJsBridge") in headStart..headEnd, "bridge script inside head")
    assertEquals(true, html.indexOf("color-scheme") in headStart..headEnd, "theme style inside head")
  }

  @Test fun handlesMissingHead() {
    val page = Resource(
      id = "ch1",
      data = "<html><body><p>hi</p></body></html>".encodeToByteArray(),
      href = "ch1.xhtml",
    ).apply { mediaType = MediaTypes.XHTML }
    val book = Book().apply { resources.add(page); addStylesheet(Stylesheets.defaultReader()) }

    val html = buildChapterDocument(book, page, backgroundCss = "#ffffff", textCss = "#000000")

    val headStart = html.indexOf("<head")
    val bodyStart = html.indexOf("<body")
    assertEquals(true, headStart in 0..bodyStart, "head inserted before body")
    assertEquals(true, html.indexOf("kmpJsBridge") in headStart..bodyStart, "bridge inside inserted head")
  }
}
