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
}
